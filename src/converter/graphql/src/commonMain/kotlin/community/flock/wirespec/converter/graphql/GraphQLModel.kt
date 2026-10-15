package community.flock.wirespec.converter.graphql

/**
 * The type system part of a GraphQL document, as defined by the GraphQL specification.
 * Executable definitions (operations and fragments) are not part of a schema and are rejected by the parser.
 *
 * Insignificant tokens (whitespace, commas, `#` comments) and the optional leading `&` and `|` separators
 * are not kept; descriptions are kept by value, so `"text"` and `"""text"""` are the same description.
 */
internal object GraphQLModel {

    data class Document(val definitions: List<Definition>)

    sealed interface Definition

    sealed interface TypeDefinition : Definition {
        val name: String
        val extension: Boolean
    }

    data class SchemaDefinition(
        val description: String?,
        val directives: List<Directive>,
        val operationTypes: List<OperationTypeDefinition>,
        val extension: Boolean,
    ) : Definition

    data class OperationTypeDefinition(val operation: Operation, val type: String)

    enum class Operation(val keyword: String) {
        QUERY("query"),
        MUTATION("mutation"),
        SUBSCRIPTION("subscription"),
        ;

        val defaultTypeName: String = keyword.replaceFirstChar(Char::uppercase)
    }

    data class ScalarTypeDefinition(
        val description: String?,
        override val name: String,
        val directives: List<Directive>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class ObjectTypeDefinition(
        val description: String?,
        override val name: String,
        val interfaces: List<String>,
        val directives: List<Directive>,
        val fields: List<FieldDefinition>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class InterfaceTypeDefinition(
        val description: String?,
        override val name: String,
        val interfaces: List<String>,
        val directives: List<Directive>,
        val fields: List<FieldDefinition>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class UnionTypeDefinition(
        val description: String?,
        override val name: String,
        val directives: List<Directive>,
        val members: List<String>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class EnumTypeDefinition(
        val description: String?,
        override val name: String,
        val directives: List<Directive>,
        val values: List<EnumValueDefinition>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class InputObjectTypeDefinition(
        val description: String?,
        override val name: String,
        val directives: List<Directive>,
        val fields: List<InputValueDefinition>,
        override val extension: Boolean,
    ) : TypeDefinition

    data class DirectiveDefinition(
        val description: String?,
        val name: String,
        val arguments: List<InputValueDefinition>,
        val repeatable: Boolean,
        val locations: List<String>,
    ) : Definition

    data class FieldDefinition(
        val description: String?,
        val name: String,
        val arguments: List<InputValueDefinition>,
        val type: TypeRef,
        val directives: List<Directive>,
    )

    data class InputValueDefinition(
        val description: String?,
        val name: String,
        val type: TypeRef,
        val defaultValue: Value?,
        val directives: List<Directive>,
    )

    data class EnumValueDefinition(
        val description: String?,
        val name: String,
        val directives: List<Directive>,
    )

    data class Directive(val name: String, val arguments: List<Argument>)

    data class Argument(val name: String, val value: Value)

    sealed interface TypeRef {
        data class Named(val name: String) : TypeRef
        data class ListOf(val type: TypeRef) : TypeRef
        data class NonNull(val type: TypeRef) : TypeRef
    }

    sealed interface Value {
        /** Kept as written, so `1e3` and `1000.0` stay distinct. */
        data class IntValue(val raw: String) : Value
        data class FloatValue(val raw: String) : Value
        data class StringValue(val value: String) : Value
        data class BooleanValue(val value: Boolean) : Value
        data object NullValue : Value
        data class EnumValue(val name: String) : Value
        data class ListValue(val values: List<Value>) : Value
        data class ObjectValue(val fields: List<Argument>) : Value
    }

    val builtInScalars: Set<String> = setOf("Int", "Float", "String", "Boolean", "ID")
}
