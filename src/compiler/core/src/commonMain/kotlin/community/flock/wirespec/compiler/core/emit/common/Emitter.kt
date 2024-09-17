package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(
    val logger: Logger,
    val split: Boolean = false
) : Emitters {

    abstract fun Definition.emitName(): String

    abstract fun notYetImplemented(): String

    open fun emit(ast: AST): List<Emitted> = ast
        .map {
            logger.info("Emitting Node $it")
            when (it) {
                is Type -> Emitted(it.emitName(), emit(it, ast))
                is Endpoint -> Emitted(it.emitName(), emit(it))
                is Enum -> Emitted(it.emitName(), emit(it))
                is Refined -> Emitted(it.emitName(), emit(it))
                is Union -> Emitted(it.emitName(), emit(it))
                is Channel -> Emitted(it.emitName(), emit(it))
            }
        }
        .run {
            if (split) this
            else listOf(Emitted("NoName", joinToString("\n") { it.result }))
        }

    fun String.spacer(space: Int = 1) = split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { "$Spacer" }}$it"
            } else {
                it
            }
        }

    internal fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "{${identifier.emit()}}"
        }

    internal fun Endpoint.Segment.emitMap() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "${'$'}{props.${identifier.emit()}}"
        }


    internal val Endpoint.pathParams get() = path.filterIsInstance<Endpoint.Segment.Param>()

    internal val Endpoint.indexedPathParams
        get() = path.withIndex().mapNotNull { (idx, segment) ->
            when (segment) {
                is Endpoint.Segment.Literal -> null
                is Endpoint.Segment.Param -> IndexedValue(idx, segment)
            }
        }

    companion object {
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase)
        fun String.firstToLower() = replaceFirstChar(Char::lowercase)
        fun AST.needImports() = any { it is Endpoint || it is Enum || it is Refined }
        fun AST.hasEndpoints() = any { it is Endpoint }
        fun String.isInt() = toIntOrNull() != null
        fun String.isStatusCode() = toIntOrNull()?.let { it in 0..599 } ?: false
        val internalClasses = setOf(
            "Request", "Response"
        )
    }
}
