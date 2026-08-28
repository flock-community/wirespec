package community.flock.wirespec.converter.graphql

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.parse.ast.HasComment
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.graphql.GraphQLConverter.ID_MARKER
import community.flock.wirespec.converter.graphql.GraphQLConverter.INPUT_MARKER
import community.flock.wirespec.converter.graphql.GraphQLConverter.INTERFACE_MARKER

object GraphQLEmitter : Emitter {

    override val extension = FileExtension.GraphQL

    private const val INDENT = "  "
    private const val JSON_SCALAR = "JSON"
    private val markerAnnotations = setOf(INPUT_MARKER, INTERFACE_MARKER, ID_MARKER)

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .map {
            logger.info("Emitting GraphQL SDL from ${it.fileUri.value}")
            Emitted("schema.graphqls", emit(it))
        }

    fun emit(module: Module): String {
        val jsonScalarNeeded = ScalarTracker()
        val models = module.statements.toList().mapNotNull { definition ->
            when (definition) {
                is Type -> definition.render(jsonScalarNeeded)
                is Enum -> definition.render()
                is Union -> definition.render()
                is Refined -> definition.render()
                else -> null
            }
        }
        val operations = module.statements.toList()
            .filterIsInstance<Graphql>()
            .groupBy { it.kind }
            .toList()
            .sortedBy { (kind, _) -> kind.ordinal }
            .map { (kind, definitions) -> definitions.renderRootType(kind, jsonScalarNeeded) }

        return (listOfNotNull("scalar $JSON_SCALAR".takeIf { jsonScalarNeeded.needed }) + models + operations)
            .joinToString("\n\n")
            .plus("\n")
    }

    private class ScalarTracker(var needed: Boolean = false)

    private fun List<Graphql>.renderRootType(kind: Graphql.Kind, tracker: ScalarTracker): String = joinToString("\n") { graphql ->
        val description = graphql.renderDescription()
        val arguments = graphql.inputs.renderArguments(tracker)
        val directives = graphql.annotations.renderDirectives()
        "$description$INDENT${graphql.operation}$arguments: ${graphql.output.render(tracker, graphql.annotations)}$directives"
    }.let { "type ${kind.name} {\n$it\n}" }

    private fun Type.render(tracker: ScalarTracker): String {
        val keyword = when {
            annotations.any { it.name == INPUT_MARKER } -> "input"
            annotations.any { it.name == INTERFACE_MARKER } -> "interface"
            else -> "type"
        }
        val implements = extends
            .takeIf { it.isNotEmpty() && keyword != "input" }
            ?.joinToString(" & ", " implements ") { it.value }
            .orEmpty()
        val directives = annotations.renderDirectives()
        val fields = shape.value.joinToString("\n") { it.render(tracker) }
        return "${renderDescription()}$keyword ${identifier.value}$implements$directives {\n$fields\n}"
    }

    private fun Field.render(tracker: ScalarTracker): String {
        val arguments = parameters.renderArguments(tracker)
        val directives = annotations.renderDirectives()
        return "$INDENT${identifier.value}$arguments: ${reference.render(tracker, annotations)}$directives"
    }

    private fun List<Field>.renderArguments(tracker: ScalarTracker): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { "${it.identifier.value}: ${it.reference.render(tracker, it.annotations)}" }
        .orEmpty()

    private fun Enum.render(): String = "${renderDescription()}enum ${identifier.value}${annotations.renderDirectives()} {\n" +
        entries.joinToString("\n") { "$INDENT$it" } +
        "\n}"

    private fun Union.render(): String = "${renderDescription()}union ${identifier.value}${annotations.renderDirectives()} = ${entries.joinToString(" | ") { it.value }}"

    private fun Refined.render(): String = "${renderDescription()}scalar ${identifier.value}${annotations.renderDirectives()}"

    private fun HasComment.renderDescription(): String = comment?.value
        ?.let {
            if ("\n" in it || "\"" in it) "\"\"\"\n$it\n\"\"\"\n" else "\"$it\"\n"
        }
        .orEmpty()

    // Wirespec is non-null by default; GraphQL is nullable by default. Pure inversion per wrapper.
    private fun Reference.render(tracker: ScalarTracker, annotations: List<Annotation> = emptyList()): String = when (this) {
        is Reference.Custom -> value
        is Reference.Iterable -> "[${reference.render(tracker)}]"
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String ->
                if (annotations.any { it.name == ID_MARKER }) "ID" else "String"

            is Reference.Primitive.Type.Integer -> "Int"
            is Reference.Primitive.Type.Number -> "Float"
            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> JSON_SCALAR.also { tracker.needed = true }
        }

        is Reference.Any, is Reference.Dict -> JSON_SCALAR.also { tracker.needed = true }
        is Reference.Unit -> error("Unit cannot be represented in GraphQL")
    }.let { if (isNullable) it else "$it!" }

    private fun List<Annotation>.renderDirectives(): String = filterNot { it.name in markerAnnotations }
        .joinToString("") { annotation ->
            val arguments = annotation.parameters
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ", "(", ")") { "${it.name}: ${it.value.render()}" }
                .orEmpty()
            " @${annotation.name}$arguments"
        }

    private fun Annotation.Value.render(): String = when (this) {
        is Annotation.Value.Single -> value.renderSingle()
        is Annotation.Value.Array -> value.joinToString(", ", "[", "]") { it.value.renderSingle() }
        is Annotation.Value.Dict -> value.joinToString(", ", "{", "}") { "${it.name}: ${it.value.render()}" }
    }

    private fun String.renderSingle(): String = when {
        this in setOf("true", "false", "null") -> this
        toLongOrNull() != null || toDoubleOrNull() != null -> this
        else -> "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}
