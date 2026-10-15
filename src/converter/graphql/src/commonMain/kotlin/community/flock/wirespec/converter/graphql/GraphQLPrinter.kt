package community.flock.wirespec.converter.graphql

import community.flock.wirespec.converter.graphql.GraphQLModel.Argument
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.Value

/** Prints the GraphQL constructs that Wirespec has no syntax for, so they can be carried in annotations. */
internal object GraphQLPrinter {

    fun TypeRef.print(): String = when (this) {
        is TypeRef.Named -> name
        is TypeRef.ListOf -> "[${type.print()}]"
        is TypeRef.NonNull -> "${type.print()}!"
    }

    fun Directive.print(): String = "@$name${arguments.printArguments()}"

    fun List<Argument>.printArguments(): String = takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { it.print() }
        .orEmpty()

    fun Value.print(): String = when (this) {
        is Value.IntValue -> raw
        is Value.FloatValue -> raw
        is Value.StringValue -> value.quote()
        is Value.BooleanValue -> value.toString()
        is Value.NullValue -> "null"
        is Value.EnumValue -> name
        is Value.ListValue -> values.joinToString(", ", "[", "]") { it.print() }
        is Value.ObjectValue -> fields.joinToString(", ", "{", "}") { it.print() }
    }

    fun String.quote(): String = buildString {
        append('"')
        this@quote.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> when {
                    char < ' ' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    else -> append(char)
                }
            }
        }
        append('"')
    }

    private fun Argument.print(): String = "$name: ${value.print()}"
}
