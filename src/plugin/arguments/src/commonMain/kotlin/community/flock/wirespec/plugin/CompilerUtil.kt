package community.flock.wirespec.plugin

import arrow.core.Either
import community.flock.wirespec.compiler.core.*
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
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.utils.Logger

typealias FilesContent = List<Pair<String, String>>

fun FilesContent.parse(logger: Logger): List<Pair<String, List<Node>>> =
    map { (name, source) -> name to WirespecSpec.parse(source)(logger) }
        .map { (name, result) ->
            name to when (result) {
                is Either.Right -> result.value
                is Either.Left -> error("compile error")
            }
        }

fun FilesContent.compile(logger: Logger, emitter: Emitter) =
    parse(logger)
        .map { (name, ast) -> name to emitter.emit(ast) }
        .flatMap { (name, results) ->
            if (emitter.split) results
            else listOf(Emitted(name, results.first().result))
        }

data class LanguageEmitter(val emitter: Emitter, val shared: Shared?, val extension: FileExtension)
fun Language.mapEmitter(packageName: PackageName, logger: Logger)=
    when (this) {
        Language.Java -> LanguageEmitter(JavaEmitter(packageName.value, logger), JavaShared, FileExtension.Java)
        Language.Kotlin -> LanguageEmitter(KotlinEmitter(packageName.value, logger), KotlinShared, FileExtension.Kotlin)
        Language.Scala -> LanguageEmitter(ScalaEmitter(packageName.value, logger), ScalaShared, FileExtension.Scala)
        Language.TypeScript -> LanguageEmitter(TypeScriptEmitter( logger), null, FileExtension.TypeScript)
        Language.Wirespec -> LanguageEmitter(WirespecEmitter(logger), null, FileExtension.Wirespec)
    }