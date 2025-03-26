package community.flock.wirespec.plugin.gradle

import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Wirespec
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.CompilerArguments
import community.flock.wirespec.plugin.FileContent
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.compile
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.DirectoryPath
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.Wirespec
import community.flock.wirespec.plugin.files.SourcePath
import community.flock.wirespec.plugin.mapEmitter
import community.flock.wirespec.plugin.parse
import community.flock.wirespec.plugin.toDirectory
import community.flock.wirespec.plugin.writeToFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class CompileWirespecTask : BaseWirespecTask() {

    @Internal
    protected fun getFilesContent(): List<FileContent> = input.asFileTree
        .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
        .map(::FileContent)

    @TaskAction
    fun action() {
        val packageNameValue = packageName.getOrElse(DEFAULT_GENERATED_PACKAGE_STRING).let(PackageName::invoke)
        val emitter = try {
            emitterClass.orNull?.getDeclaredConstructor()?.newInstance() as? Emitter
        } catch (e: Exception) {
            logger.error("Cannot create instance of emitter: ${emitterClass.orNull?.simpleName}", e)
            throw e
        }?.let { emitter ->
            val ext = emitter.extension.value
            getFilesContent()
                .compile(wirespecLogger, emitter)
                .also {
                    output.dir(PackageName(emitter.shared?.packageString).toDirectory()).get().asFile.apply {
                        mkdirs()
                        emitter.shared?.source?.let { resolve("Wirespec.$ext").writeText(it) }
                    }
                }
                .onEach { (name, result) ->
                    output.dir(packageNameValue.toDirectory()).get().asFile.apply {
                        mkdirs()
                        resolve("$name.$ext").writeText(result)
                    }
                }
            emitter
        }

        val emitters = languages.get().map {
            when (it) {
                Language.Java -> JavaEmitter(packageNameValue)
                Language.Kotlin -> KotlinEmitter(packageNameValue)
                Language.Scala -> ScalaEmitter(packageNameValue)
                Language.TypeScript -> TypeScriptEmitter()
                Language.Wirespec -> WirespecEmitter()
                Language.OpenAPIV2 -> OpenAPIV2Emitter
                Language.OpenAPIV3 -> OpenAPIV3Emitter
            }
        }.plus(emitter)
            .mapNotNull { it }
            .toNonEmptySetOrNull()
            ?: throw PickAtLeastOneLanguageOrEmitter()

        val inputPath = getFullPath(input.get().asFile.absolutePath)
        val sources = when (inputPath) {
            null -> throw IsNotAFileOrDirectory(null)
            is SourcePath -> nonEmptySetOf(inputPath.readFromClasspath())
            is DirectoryPath -> Directory(inputPath).wirespecFiles()
            is FilePath -> when (inputPath.extension) {
                Wirespec -> nonEmptySetOf(Source<Wirespec>(inputPath.name, inputPath.read()))
                else -> throw WirespecFileError()
            }
        }
        CompilerArguments(
            input = sources,
            output = Directory(getOutPutPath(inputPath)),
            emitters = emitters,
            writer = { filePath, string -> filePath.write(string) },
            error = { throw RuntimeException(it) },
            packageName = packageNameValue,
            logger = wirespecLogger,
            shared = shared.getOrElse(true),
            strict = strict.getOrElse(false),
        )

        val ast = getFilesContent().parse(wirespecLogger)

        languages.getOrElse(emptyList())
            .map { it.mapEmitter(packageNameValue) }
            .forEach { (emitter, ext, sharedData) ->
                ast.forEach { (fileName, ast) ->
                    emitter.emit(ast, wirespecLogger).writeToFiles(
                        output = output.asFile.get(),
                        packageName = packageNameValue,
                        shared = if (shared.getOrElse(true)) sharedData else null,
                        fileName = if (emitter.split) null else fileName,
                        ext = ext,
                    )
                }
            }
    }
}
