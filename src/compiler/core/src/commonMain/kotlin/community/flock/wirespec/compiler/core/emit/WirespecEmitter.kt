package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class WirespecEmitter(logger: Logger = noLogger) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> emit(identifier)
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
        is Channel -> emit(identifier)
    }

    override val singleLineComment = "\n"

    override fun notYetImplemented() = singleLineComment

    override fun emit(type: Type, ast: AST) = """
        |type ${emit(type.identifier)} {
        |${type.shape.emit()}
        |}
        |""".trimMargin()

    override fun Type.Shape.emit() = value.joinToString(",\n") { "$Spacer${it.emit()}" }

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}${if (isNullable) "?" else ""}"

    override fun emit(identifier: Identifier) =
        identifier.run { if (value in reservedKeywords) value.addBackticks() else value }

    override fun emit(channel: Channel): String =
        "channel ${emit(channel.identifier)} -> ${channel.reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when(type.precision){
                Reference.Primitive.Type.Precision.P32 -> "Integer32"
                else ->"Integer"
            }
            is Reference.Primitive.Type.Number -> when(type.precision){
                Reference.Primitive.Type.Precision.P32 -> "Number32"
                else ->"Number"
            }
            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "Bytes"
        }
    }
        .let { if (isIterable) "$it[]" else it }
        .let { if (isDictionary) "{ $it }" else it }

    override fun emit(enum: Enum) =
        "enum ${emit(enum.identifier)} {\n${Spacer}${enum.entries.joinToString(", ") { it.capitalize() }}\n}\n"

    override fun emit(refined: Refined) = "type ${emit(refined.identifier)} ${refined.validator.emit()}\n"

    override fun Refined.Validator.emit() = value

    override fun emit(endpoint: Endpoint) = """
        |endpoint ${emit(endpoint.identifier)} ${endpoint.method}${endpoint.requests.emitRequest()} ${endpoint.path.emitPath()}${endpoint.queries.emitQuery()} -> {
        |${endpoint.responses.joinToString("\n") { "$Spacer${it.status.fixStatus()} -> ${it.content?.reference?.emit() ?: "Unit"}${if (it.content?.isNullable == true) "?" else ""}" }}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) =
        "type ${emit(union.identifier)} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

    private fun String.fixStatus(): String = when (this) {
        "default" -> "200"
        else -> this
    }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}: ${it.reference.emit()}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Endpoint.Request>.emitRequest() =
        firstOrNull()?.content?.reference?.emit()?.let { " $it" }.orEmpty()

    private fun List<Field>.emitQuery() = takeIf { it.isNotEmpty() }
        ?.joinToString(",", "{", "}") { it.emit() }
        ?.let { " ? $it" }
        .orEmpty()

    private fun String.capitalize() = replaceFirstChar { it.uppercase() }

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "type", "enum", "endpoint"
        )
    }
}
