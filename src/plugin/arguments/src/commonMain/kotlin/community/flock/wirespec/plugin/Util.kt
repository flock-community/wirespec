package community.flock.wirespec.plugin

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.OpenAPIV2
import community.flock.wirespec.plugin.Language.OpenAPIV3
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Name
import community.flock.wirespec.plugin.files.plus

data class FileContent(val name: String, val content: String) {
    constructor(pair: Pair<String, String>) : this(pair.first, pair.second)
}

fun List<FileContent>.parse(logger: Logger): List<Pair<String, AST>> = map { (name, source) ->
    name to object : ParseContext {
        override val spec = WirespecSpec
        override val logger = logger
    }.parse(nonEmptyListOf(source))
}.map { (name, result) ->
    name to when (result) {
        is Right -> result.value
        is Left -> error("compile error: ${result.value}")
    }
}

fun List<FileContent>.compile(logger: Logger, emitter: Emitter) = parse(logger)
    .map { (name, ast) -> name to emitter.emit(ast, logger) }
    .flatMap { (name, results) ->
        when {
            emitter.split -> results
            else -> listOf(Emitted(name, results.first().result))
        }
    }

data class LanguageEmitter(val emitter: Emitter, val extension: FileExtension, val shared: Shared? = null)

fun Language.mapEmitter(packageName: PackageName) = when (this) {
    Java -> LanguageEmitter(JavaEmitter(packageName), FileExtension.Java, JavaShared)
    Kotlin -> LanguageEmitter(KotlinEmitter(packageName), FileExtension.Kotlin, KotlinShared)
    Scala -> LanguageEmitter(ScalaEmitter(packageName), FileExtension.Scala, ScalaShared)
    TypeScript -> LanguageEmitter(TypeScriptEmitter(), FileExtension.TypeScript)
    Wirespec -> LanguageEmitter(WirespecEmitter(), FileExtension.Wirespec)
    OpenAPIV2 -> LanguageEmitter(OpenAPIV2Emitter, FileExtension.JSON)
    OpenAPIV3 -> LanguageEmitter(OpenAPIV3Emitter, FileExtension.JSON)
}

fun WirespecArguments.mapShared() = emitters.mapNotNull {
    it.mapShared(FilePath(output.path, Name("Wirespec"), it.extension), shared)
}

private fun Emitter.mapShared(filePath: FilePath, shared: Boolean) = takeIf { shared }
    ?.let { it.shared?.run { filePath.copy(extension = it.extension) to this } }
    ?.let { (file, shared) -> file.copy(directory = file.directory + PackageName(shared.packageString)) to shared }
    ?.let { (file, shared) -> file to shared.source }
