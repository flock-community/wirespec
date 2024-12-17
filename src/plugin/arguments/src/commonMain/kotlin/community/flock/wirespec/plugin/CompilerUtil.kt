package community.flock.wirespec.plugin

import arrow.core.Either
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.shared.JavaShared
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.openapi.v2.OpenApiV2Emitter
import community.flock.wirespec.openapi.v3.OpenApiV3Emitter
import community.flock.wirespec.plugin.Language.Java
import community.flock.wirespec.plugin.Language.Kotlin
import community.flock.wirespec.plugin.Language.OpenAPIV2
import community.flock.wirespec.plugin.Language.OpenAPIV3
import community.flock.wirespec.plugin.Language.Scala
import community.flock.wirespec.plugin.Language.TypeScript
import community.flock.wirespec.plugin.Language.Wirespec

typealias FilesContent = List<Pair<String, String>>

fun FilesContent.parse(logger: Logger): List<Pair<String, List<Node>>> =
    map { (name, source) -> name to WirespecSpec.parse(source)(logger) }
        .map { (name, result) ->
            name to when (result) {
                is Either.Right -> result.value
                is Either.Left -> error("compile error: ${result.value}")
            }
        }

fun FilesContent.compile(logger: Logger, emitter: Emitter) =
    parse(logger)
        .map { (name, ast) -> name to ast.validate().let { emitter.emit(it) } }
        .flatMap { (name, results) ->
            if (emitter.split) results
            else listOf(Emitted(name, results.first().result))
        }

data class LanguageEmitter(val emitter: Emitter, val extension: FileExtension, val shared: Shared? = null)

fun Language.mapEmitter(packageName: PackageName, logger: Logger) =
    when (this) {
        Java -> LanguageEmitter(JavaEmitter(packageName.value, logger), FileExtension.Java, JavaShared)
        Kotlin -> LanguageEmitter(KotlinEmitter(packageName.value, logger), FileExtension.Kotlin, KotlinShared)
        Scala -> LanguageEmitter(ScalaEmitter(packageName.value, logger), FileExtension.Scala, ScalaShared)
        TypeScript -> LanguageEmitter(TypeScriptEmitter(logger), FileExtension.TypeScript)
        Wirespec -> LanguageEmitter(WirespecEmitter(logger), FileExtension.Wirespec)
        OpenAPIV2 -> LanguageEmitter(OpenApiV2Emitter, FileExtension.Json)
        OpenAPIV3 -> LanguageEmitter(OpenApiV3Emitter, FileExtension.Json)
    }
