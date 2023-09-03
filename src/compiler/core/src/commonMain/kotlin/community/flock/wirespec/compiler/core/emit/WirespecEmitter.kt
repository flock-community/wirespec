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
        is Endpoint -> identifier.emitClassName()
        is Enum -> identifier.emitClassName()
        is Refined -> identifier.emitClassName()
        is Type -> identifier.emitClassName()
        is Union -> identifier.emitClassName()
        is Channel -> identifier.emitClassName()
    }

    override fun notYetImplemented() = "\n"

    override fun emit(type: Type, ast: AST) = """
        |type ${type.identifier.emitClassName()} {
        |${type.shape.emit()}
        |}
        |""".trimMargin()


    override fun Type.Shape.emit() = value.joinToString(",\n") { "$Spacer${it.emit()}" }

    override fun Field.emit() = "${identifier.emitVariableName()}: ${reference.emit()}${if (isNullable) "?" else ""}"

    override fun Identifier.emitVariableName() = if (value in reservedKeywords) value.addBackticks() else value
    override fun Identifier.emitClassName() = if (value in reservedKeywords) value.addBackticks() else value

    override fun emit(channel: Channel): String = "channel ${channel.identifier.emitClassName()} -> ${channel.reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            Reference.Primitive.Type.String -> "String"
            Reference.Primitive.Type.Integer -> "Integer"
            Reference.Primitive.Type.Number -> "Number"
            Reference.Primitive.Type.Boolean -> "Boolean"
        }
    }
        .let { if (isIterable) "$it[]" else it }
        .let { if (isDictionary) "{ $it }" else it }

    override fun emit(enum: Enum) =
        "enum ${enum.identifier.emitClassName()} {\n${Spacer}${enum.entries.joinToString(", ") { it.capitalize() }}\n}\n"

    override fun emit(refined: Refined) = "type ${refined.identifier.emitClassName()} ${refined.validator.emit()}\n"

    override fun Refined.Validator.emit() = value

    override fun emit(endpoint: Endpoint) = """
        |endpoint ${endpoint.identifier.emitClassName()} ${endpoint.method}${endpoint.requests.emitRequest()} ${endpoint.path.emitPath()}${endpoint.queries.emitQuery()} -> {
        |${endpoint.responses.joinToString("\n") { "$Spacer${it.status.fixStatus()} -> ${it.content?.reference?.emit() ?: "Unit"}${if (it.content?.isNullable == true) "?" else ""}" }}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) =
        "type ${union.identifier.emitClassName()} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

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
