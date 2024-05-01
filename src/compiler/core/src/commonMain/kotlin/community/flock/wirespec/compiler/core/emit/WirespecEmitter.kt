package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class WirespecEmitter(logger: Logger = noLogger) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> name
        is Enum -> name
        is Refined -> name
        is Type -> name
        is Union -> name
    }

    override fun Type.emit(ast: AST) = """
        |type $name {
        |${shape.emit()}
        |}
        |""".trimMargin()


    override fun Type.Shape.emit() = value.joinToString(",\n") { "$SPACER${it.emit()}" }

    override fun Field.emit() = "${identifier.emit()}: ${reference.emit()}${if (isNullable) "?" else ""}"


    override fun Field.Identifier.emit() = if (value in preservedKeywords) value.addBackticks() else value

    override fun Field.Reference.emit(): String = when (this) {
        is Field.Reference.Unit -> "Unit"
        is Field.Reference.Any -> "Any"
        is Field.Reference.Custom -> value
        is Field.Reference.Primitive -> when (type) {
            Field.Reference.Primitive.Type.String -> "String"
            Field.Reference.Primitive.Type.Integer -> "Integer"
            Field.Reference.Primitive.Type.Number -> "Number"
            Field.Reference.Primitive.Type.Boolean -> "Boolean"
        }
    }.let { if (isIterable) "$it[]" else it }

    override fun Enum.emit() = "enum $name {\n${SPACER}${entries.joinToString(", ") { it.capitalize() }}\n}\n"

    override fun Refined.emit() = "type $name ${validator.emit()}\n"

    override fun Refined.Validator.emit() = "/${value.drop(1).dropLast(1)}/g"

    override fun Endpoint.emit() =
        """
            |endpoint $name ${method}${requests.emitRequest()} ${path.emitPath()}${query.emitQuery()} -> {
            |${responses.joinToString("\n") { "$SPACER${it.status.fixStatus()} -> ${it.content?.reference?.emit() ?: "Unit"}${if (it.content?.isNullable == true) "?" else ""}" }}
            |}
            |
        """.trimMargin()

    override fun Union.emit() = "type $name = ${entries.joinToString(" | ") { it.emit() }}\n"

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

    companion object {
        private val preservedKeywords = listOf(
            "type", "enum", "endpoint"
        )
    }
}
