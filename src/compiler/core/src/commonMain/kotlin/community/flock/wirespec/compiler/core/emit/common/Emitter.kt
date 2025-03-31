package community.flock.wirespec.compiler.core.emit.common

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.common.Emitter.Param.ParamType
import community.flock.wirespec.compiler.core.emit.shared.Shared
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(
    val split: Boolean = false,
) : Emitters {

    data class Param(
        val type: ParamType,
        val identifier: Identifier,
        val reference: Reference,
    ) {
        enum class ParamType {
            PATH, QUERY, HEADER, BODY
        }
    }

    abstract val extension: FileExtension

    abstract val shared: Shared?

    open fun Definition.emitName(): String = notYetImplemented()

    fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules.flatMap { emit(it, logger) }

    open fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map {
            when (it) {
                is Definition -> it.emitName()
            }.also { name -> logger.info("Emitting Node $name") }
            when (it) {
                is Type -> Emitted(it.emitName(), emit(it, module))
                is Endpoint -> Emitted(it.emitName(), emit(it))
                is Enum -> Emitted(it.emitName(), emit(it, module))
                is Refined -> Emitted(it.emitName(), emit(it))
                is Union -> Emitted(it.emitName(), emit(it))
                is Channel -> Emitted(it.emitName(), emit(it))
            }
        }
        .run {
            if (split) this
            else nonEmptyListOf(Emitted("NoName", joinToString("\n") { it.result }))
        }

    fun String.spacer(space: Int = 1) = split("\n")
        .joinToString("\n") {
            if (it.isNotBlank()) {
                "${(1..space).joinToString("") { "$Spacer" }}$it"
            } else {
                it
            }
        }

    open fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "{${identifier.value}}"
        }

    internal fun Endpoint.Segment.emitMap() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "${'$'}{props.${emit(identifier)}}"
        }

    internal val Endpoint.pathParams get() = path.filterIsInstance<Endpoint.Segment.Param>()

    internal val Endpoint.indexedPathParams
        get() = path.withIndex().mapNotNull { (idx, segment) ->
            when (segment) {
                is Endpoint.Segment.Literal -> null
                is Endpoint.Segment.Param -> IndexedValue(idx, segment)
            }
        }

    internal fun Endpoint.Request.paramList(endpoint: Endpoint): List<Param> = listOf(
        endpoint.pathParams.map { it.toParam() },
        endpoint.queries.map { it.toParam(ParamType.QUERY) },
        endpoint.headers.map { it.toParam(ParamType.HEADER) },
        listOfNotNull(content?.toParam()),
    ).flatten()

    internal fun Endpoint.Response.paramList(): List<Param> = listOf(
        headers.map { it.toParam(ParamType.HEADER) },
        listOfNotNull(content?.toParam())
    ).flatten()

    private fun Endpoint.Segment.Param.toParam() = Param(
        type = ParamType.PATH,
        identifier = identifier,
        reference = reference,
    )

    private fun Endpoint.Content.toParam() = Param(
        type = ParamType.BODY,
        identifier = FieldIdentifier("body"),
        reference = reference,
    )

    private fun Field.toParam(type: ParamType) = Param(
        type = type,
        identifier = identifier,
        reference = reference,
    )

    companion object {
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase)
        fun String.firstToLower() = replaceFirstChar(Char::lowercase)
        fun Module.needImports() = statements.any { it is Endpoint || it is Enum || it is Refined }
        fun Module.hasEndpoints() = statements.any { it is Endpoint }
        fun String.isStatusCode() = toIntOrNull()?.let { it in 0..599 } ?: false
        val internalClasses = setOf(
            "Request", "Response"
        )
    }
}

interface HasEmitter {
    val emitters: NonEmptySet<Emitter>
}
