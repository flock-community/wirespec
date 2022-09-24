package community.flock.wirespec.plugin.maven

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.utils.Logger
import java.io.File

typealias LanguageDirectory = String
typealias FullPath = String
typealias EmittedDocument = String

abstract class Generator(
    languageDirectory: LanguageDirectory,
    pathTemplate: (LanguageDirectory) -> (FileName) -> FullPath,
    private val logger: Logger,
    private val emitter: Emitter,
    private val multipleFiles: Boolean = false,
    private val disclaimer: String
) {

    private val pathTemplate: (FileName) -> FullPath = pathTemplate(languageDirectory.alsoMakeDir())

    fun generate(documents: Documents) =
        documents.emittedWith(multipleFiles, emitter) extendedWith { "$disclaimer$this" } andWrittenTo pathTemplate

    companion object {
        private fun String.alsoMakeDir() = also { File(it).mkdirs() }

        private fun Documents.emittedWith(multipleFiles: Boolean, emitter: Emitter): List<Pair<FileName, String>> {
            return map { (fileName, document) ->  Pair(fileName, document(emitter).orNull() ?: "")}
        }

        private infix fun List<Pair<FileName, EmittedDocument>>.extendedWith(block: EmittedDocument.() -> EmittedDocument) =
            map { (fileName, emittedDocument) -> fileName to emittedDocument.block() }

        private infix fun List<Pair<FileName, EmittedDocument>>.andWrittenTo(pathTemplate: (FileName) -> FullPath) =
            forEach { (fileName, emittedDocument) -> File(pathTemplate(fileName)).writeText(emittedDocument) }

        private fun List<Pair<FileName, String>>.emitDoc(emit: (String) -> String) =
            map { (fileName, document) -> fileName to emit(document) }
    }

}
