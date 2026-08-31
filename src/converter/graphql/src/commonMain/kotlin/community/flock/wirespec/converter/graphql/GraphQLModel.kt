package community.flock.wirespec.converter.graphql

object GraphQLModel {

    data class Document(val definitions: List<TypeSystemDefinition>)

    sealed interface TypeSystemDefinition

    data class SchemaDefinition(
        val query: String?,
        val mutation: String?,
        val subscription: String?,
    ) : TypeSystemDefinition

    sealed interface TypeDefinition : TypeSystemDefinition {
        val name: String
        val description: String?
        val directives: List<Directive>
    }

    data class ObjectTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val interfaces: List<String>,
        val fields: List<FieldDefinition>,
    ) : TypeDefinition

    data class InterfaceTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val interfaces: List<String>,
        val fields: List<FieldDefinition>,
    ) : TypeDefinition

    data class InputObjectTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val fields: List<InputValueDefinition>,
    ) : TypeDefinition

    data class EnumTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val values: List<String>,
    ) : TypeDefinition

    data class UnionTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
        val members: List<String>,
    ) : TypeDefinition

    data class ScalarTypeDefinition(
        override val name: String,
        override val description: String?,
        override val directives: List<Directive>,
    ) : TypeDefinition

    data class FieldDefinition(
        val name: String,
        val description: String?,
        val arguments: List<InputValueDefinition>,
        val type: TypeRef,
        val directives: List<Directive>,
    )

    data class InputValueDefinition(
        val name: String,
        val description: String?,
        val type: TypeRef,
        val directives: List<Directive>,
    )

    sealed interface TypeRef {
        val nonNull: Boolean

        data class Named(val name: String, override val nonNull: Boolean) : TypeRef
        data class ListOf(val inner: TypeRef, override val nonNull: Boolean) : TypeRef
    }

    data class Directive(val name: String, val arguments: List<Argument>)

    data class Argument(val name: String, val value: Value)

    sealed interface Value {
        data class Single(val value: String) : Value
        data class ListOf(val values: List<Single>) : Value
        data class ObjectOf(val fields: List<Argument>) : Value
    }
}
