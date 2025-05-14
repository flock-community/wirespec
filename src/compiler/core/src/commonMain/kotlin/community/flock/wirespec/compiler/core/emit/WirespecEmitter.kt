package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
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

open class WirespecEmitter : Emitter() {

    override val extension = FileExtension.Wirespec

    override val shared = null

    override val singleLineComment = "\n"

    override fun notYetImplemented() = singleLineComment

    override fun emit(type: Type, module: Module) = """
        |type ${emit(type.identifier)} {
        |${type.shape.emit()}
        |}
        |""".trimMargin()

    override fun Type.Shape.emit() = value.joinToString(",\n") { "$Spacer${it.emit()}" }

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}"

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.run { if (value in reservedKeywords) value.addBackticks() else value }
        is FieldIdentifier -> identifier.run {
            if (value in reservedKeywords || value.first().isUpperCase()) value.addBackticks() else value
        }
    }

    override fun emit(channel: Channel): String =
        "channel ${emit(channel.identifier)} -> ${channel.reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "{ ${reference.emit()} }"
        is Reference.Iterable -> "${reference.emit()}[]"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "Bytes"
            is Reference.Primitive.Type.Integer -> when (type.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Integer32"
                Reference.Primitive.Type.Precision.P64 -> "Integer"
            }

            is Reference.Primitive.Type.Number -> when (type.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Number32"
                Reference.Primitive.Type.Precision.P64 -> "Number"
            }
        }
    }.let { if (isNullable) "$it?" else it }

    override fun emit(enum: Enum, module: Module) =
        "enum ${emit(enum.identifier)} {\n${Spacer}${enum.entries.joinToString(", ") { it.capitalize() }}\n}\n"

    override fun emit(refined: Refined) = "type ${emit(refined.identifier)} ${refined.validator.emit()}\n"

    override fun Refined.Validator.emit() = value

    override fun emit(endpoint: Endpoint) = """
        |endpoint ${emit(endpoint.identifier)} ${endpoint.method}${endpoint.requests.emitRequest()} ${endpoint.path.emitPath()}${endpoint.queries.emitQuery()} -> {
        |${endpoint.responses.joinToString("\n") { "$Spacer${it.status.fixStatus()} -> ${it.content?.reference?.emit() ?: "Unit"}${if (it.content?.reference?.isNullable == true) "?" else ""}" }}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) =
        "type ${emit(union.identifier)} = ${union.entries.joinToString(" | ") { it.emit() }}\n"

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
