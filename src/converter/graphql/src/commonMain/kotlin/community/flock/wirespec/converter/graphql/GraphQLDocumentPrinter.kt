package community.flock.wirespec.converter.graphql

import community.flock.wirespec.converter.graphql.GraphQLModel.Definition
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive
import community.flock.wirespec.converter.graphql.GraphQLModel.DirectiveDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLPrinter.print
import community.flock.wirespec.converter.graphql.GraphQLPrinter.quote

/** Prints a GraphQL document as SDL. Descriptions are written as plain strings, which keeps every value exact. */
internal object GraphQLDocumentPrinter {

    fun Document.print(): String = definitions.joinToString("\n\n") { it.print() } + "\n"

    private fun Definition.print(): String = when (this) {
        is SchemaDefinition -> description.printDescription() +
            "${extend(extension)}schema${directives.printDirectives()}" +
            operationTypes.takeIf { it.isNotEmpty() }?.joinToString("\n", " {\n", "\n}") { "  ${it.operation.keyword}: ${it.type}" }.orEmpty()
        is ScalarTypeDefinition -> description.printDescription() + "${extend(extension)}scalar $name${directives.printDirectives()}"
        is ObjectTypeDefinition -> description.printDescription() +
            "${extend(extension)}type $name${interfaces.printInterfaces()}${directives.printDirectives()}${fields.printBlock { it.print() }}"
        is InterfaceTypeDefinition -> description.printDescription() +
            "${extend(extension)}interface $name${interfaces.printInterfaces()}${directives.printDirectives()}${fields.printBlock { it.print() }}"
        is UnionTypeDefinition -> description.printDescription() +
            "${extend(extension)}union $name${directives.printDirectives()}" +
            members.takeIf { it.isNotEmpty() }?.joinToString(" | ", " = ").orEmpty()
        is EnumTypeDefinition -> description.printDescription() +
            "${extend(extension)}enum $name${directives.printDirectives()}" +
            values.printBlock { "${it.description.printDescription("  ")}${it.name}${it.directives.printDirectives()}" }
        is InputObjectTypeDefinition -> description.printDescription() +
            "${extend(extension)}input $name${directives.printDirectives()}${fields.printBlock { it.print() }}"
        is DirectiveDefinition -> description.printDescription() +
            "directive @$name${arguments.printArguments()}${if (repeatable) " repeatable" else ""} on ${locations.joinToString(" | ")}"
    }

    private fun FieldDefinition.print(): String = "${description.printDescription("  ")}$name${arguments.printArguments()}: ${type.print()}${directives.printDirectives()}"

    private fun InputValueDefinition.print(indent: String = "  "): String = "${description.printDescription(indent)}$name: ${type.print()}" +
        defaultValue?.let { " = ${it.print()}" }.orEmpty() +
        directives.printDirectives()

    private fun List<InputValueDefinition>.printArguments(): String = takeIf { it.isNotEmpty() }
        ?.joinToString("\n", "(\n", "\n  )") { "    ${it.print("    ")}" }
        .orEmpty()

    private fun <T> List<T>.printBlock(item: (T) -> String): String = takeIf { it.isNotEmpty() }
        ?.joinToString("\n", " {\n", "\n}") { "  ${item(it)}" }
        .orEmpty()

    private fun List<String>.printInterfaces(): String = takeIf { it.isNotEmpty() }?.joinToString(" & ", " implements ").orEmpty()

    private fun List<Directive>.printDirectives(): String = joinToString("") { " ${it.print()}" }

    private fun String?.printDescription(indent: String = ""): String = this?.let { "${it.quote()}\n$indent" }.orEmpty()

    private fun extend(extension: Boolean): String = if (extension) "extend " else ""
}
