package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.Annotation

/**
 * The annotations that carry what Wirespec has no syntax for, so a GraphQL schema survives the conversion.
 *
 * Wirespec keeps a string annotation value exactly as written between its quotes, escape sequences included.
 * Values are therefore stored escaped ([toLiteral]), so the AST the converter
 * produces is the same AST the Wirespec parser produces from the emitted source.
 */
internal object GraphQLAnnotations {
    const val DESCRIPTION = "Description"
    const val NAME = "GraphQLName"
    const val INTERFACE = "GraphQLInterface"
    const val INPUT = "GraphQLInput"
    const val UNION = "GraphQLUnion"
    const val ENUM = "GraphQLEnum"
    const val SCHEMA = "GraphQLSchema"
    const val EXTEND = "GraphQLExtend"
    const val IMPLEMENTS = "GraphQLImplements"
    const val DIRECTIVE = "GraphQLDirective"
    const val DIRECTIVE_DEFINITION = "GraphQLDirectiveDefinition"
    const val ARGUMENT = "GraphQLArgument"
    const val DEFAULT = "GraphQLDefault"
    const val ENUM_VALUE = "GraphQLEnumValue"
    const val TYPE = "GraphQLType"
    const val UNION_MEMBERS = "GraphQLUnionMembers"
    const val BUILT_IN = "GraphQLBuiltIn"

    const val DEFAULT_PARAMETER = "default"

    /** Marks an rpc as a field of a root operation type: `@GraphQLQuery`, `@GraphQLMutation` or `@GraphQLSubscription`. */
    val GraphQLModel.Operation.annotationName: String get() = "GraphQL$defaultTypeName"

    fun annotation(name: String, vararg parameters: Annotation.Parameter?): Annotation = Annotation(name, parameters.filterNotNull())

    fun annotation(name: String, value: String): Annotation = annotation(name, parameter(DEFAULT_PARAMETER, value))

    fun parameter(name: String, value: String): Annotation.Parameter = Annotation.Parameter(name, Annotation.Value.Single(value.toLiteral()))

    fun parameter(name: String, values: List<String>): Annotation.Parameter? = values.takeIf { it.isNotEmpty() }
        ?.let { Annotation.Parameter(name, Annotation.Value.Array(it.map { value -> Annotation.Value.Single(value.toLiteral()) })) }

    private fun String.toLiteral(): String = buildString {
        this@toLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> when {
                    char < ' ' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    else -> append(char)
                }
            }
        }
    }
}
