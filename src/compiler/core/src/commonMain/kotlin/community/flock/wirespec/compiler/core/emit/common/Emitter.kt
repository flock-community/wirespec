package community.flock.wirespec.compiler.core.emit.common

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
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
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter : Emitters {

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

    fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .modules.flatMap { emit(it, logger) }
        .map { e -> Emitted(e.file + "." + extension.value, e.result) }

    open fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> = module
        .statements
        .map { emit(it, module, logger) }

    open fun emit(definition: Definition, module: Module, logger: Logger): Emitted = run {
        logger.info("Emitting Node ${definition.identifier.value}")
        when (definition) {
            is Type -> Emitted(emit(definition.identifier), emit(definition, module))
            is Endpoint -> Emitted(emit(definition.identifier), emit(definition))
            is Enum -> Emitted(emit(definition.identifier), emit(definition, module))
            is Refined -> Emitted(emit(definition.identifier), emit(definition))
            is Union -> Emitted(emit(definition.identifier), emit(definition))
            is Channel -> Emitted(emit(definition.identifier), emit(definition))
        }
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


    private fun Reference.flattenListDict(): Reference = when (this) {
        is Reference.Dict -> reference.flattenListDict()
        is Reference.Iterable -> reference.flattenListDict()
        else -> this
    }

    fun Node.importReferences(): List<Reference.Custom> = when (this) {
        is Endpoint -> listOf(
            path.filterIsInstance<Endpoint.Segment.Param>().map { it.reference },
            headers.map { it.reference },
            queries.map { it.reference },
            requests.map { it.content?.reference },
            responses.flatMap { listOf(it.content?.reference) + it.headers.map { it.reference } }
        ).flatten().filterNotNull().map { it.flattenListDict() }.filterIsInstance<Reference.Custom>().distinct()

        is Type -> shape.value
            .filter { identifier.value != it.reference.root().value }
            .map { it.reference.flattenListDict() }
            .filterIsInstance<Reference.Custom>()
            .distinct()

        is Channel -> if (reference is Reference.Custom) listOf(reference) else emptyList()
        else -> emptyList()
    }

    private fun Endpoint.Segment.Param.toParam() = Param(
        type = ParamType.PATH,
        identifier = identifier,
        reference = reference,
    )

    private fun Reference.root() = when (this) {
        is Reference.Dict -> reference
        is Reference.Iterable -> reference
        else -> this
    }

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

interface HasEmitters {
    val emitters: NonEmptySet<Emitter>
}
