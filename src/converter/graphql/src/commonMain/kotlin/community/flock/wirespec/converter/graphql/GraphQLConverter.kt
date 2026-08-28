package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Value

object GraphQLConverter {

    /** Marker annotations used to keep the SDL round trip lossless. */
    const val INPUT_MARKER = "input"
    const val INTERFACE_MARKER = "interface"
    const val ID_MARKER = "id"

    fun convert(document: Document, strict: Boolean): List<Definition> {
        val schema = document.definitions.filterIsInstance<SchemaDefinition>().firstOrNull()
        val rootTypes = mapOf(
            (schema?.query ?: "Query") to Graphql.Kind.Query,
            (schema?.mutation ?: "Mutation") to Graphql.Kind.Mutation,
            (schema?.subscription ?: "Subscription") to Graphql.Kind.Subscription,
        )

        return document.definitions.flatMap { definition ->
            when (definition) {
                is SchemaDefinition -> emptyList()
                is ObjectTypeDefinition ->
                    rootTypes[definition.name]
                        ?.let { kind -> definition.fields.map { it.toGraphql(kind) } }
                        ?: listOf(definition.toType())

                is InterfaceTypeDefinition -> listOf(definition.toType())
                is InputObjectTypeDefinition -> listOf(definition.toType())
                is EnumTypeDefinition -> listOf(definition.toEnum())
                is UnionTypeDefinition -> listOf(definition.toUnion())
                is ScalarTypeDefinition -> listOf(definition.toRefined())
            }
        }
    }

    private fun FieldDefinition.toGraphql(kind: Graphql.Kind) = Graphql(
        comment = description?.let { Comment(it) },
        annotations = directives.toAnnotations(),
        identifier = DefinitionIdentifier(name.replaceFirstChar(Char::uppercaseChar) + kind.name),
        kind = kind,
        operation = FieldIdentifier(name),
        inputs = arguments.map { it.toField() },
        output = type.toReference(),
    )

    private fun ObjectTypeDefinition.toType() = Type(
        comment = description?.let { Comment(it) },
        annotations = directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        shape = Type.Shape(fields.map { it.toField() }),
        extends = interfaces.map { Reference.Custom(it, isNullable = false) },
    )

    private fun InterfaceTypeDefinition.toType() = Type(
        comment = description?.let { Comment(it) },
        annotations = listOf(Annotation(INTERFACE_MARKER, emptyList())) + directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        shape = Type.Shape(fields.map { it.toField() }),
        extends = interfaces.map { Reference.Custom(it, isNullable = false) },
    )

    private fun InputObjectTypeDefinition.toType() = Type(
        comment = description?.let { Comment(it) },
        annotations = listOf(Annotation(INPUT_MARKER, emptyList())) + directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        shape = Type.Shape(fields.map { it.toField() }),
        extends = emptyList(),
    )

    private fun EnumTypeDefinition.toEnum() = Enum(
        comment = description?.let { Comment(it) },
        annotations = directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        entries = values.toSet(),
    )

    private fun UnionTypeDefinition.toUnion() = Union(
        comment = description?.let { Comment(it) },
        annotations = directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        entries = members.map { Reference.Custom(it, isNullable = false) }.toSet(),
    )

    private fun ScalarTypeDefinition.toRefined() = Refined(
        comment = description?.let { Comment(it) },
        annotations = directives.toAnnotations(),
        identifier = DefinitionIdentifier(name),
        reference = Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = false),
    )

    private fun FieldDefinition.toField() = Field(
        annotations = type.idAnnotations() + directives.toAnnotations(),
        identifier = FieldIdentifier(name),
        reference = type.toReference(),
        parameters = arguments.map { it.toField() },
    )

    private fun InputValueDefinition.toField() = Field(
        annotations = type.idAnnotations() + directives.toAnnotations(),
        identifier = FieldIdentifier(name),
        reference = type.toReference(),
    )

    private fun TypeRef.idAnnotations(): List<Annotation> = when (baseName()) {
        "ID" -> listOf(Annotation(ID_MARKER, emptyList()))
        else -> emptyList()
    }

    private fun TypeRef.baseName(): String? = when (this) {
        is TypeRef.Named -> name
        is TypeRef.ListOf -> inner.baseName()
    }

    // GraphQL is nullable-by-default; Wirespec is non-null by default. Pure inversion per wrapper.
    private fun TypeRef.toReference(): Reference = when (this) {
        is TypeRef.Named -> when (name) {
            "Int" -> Reference.Primitive(Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), !nonNull)
            "Float" -> Reference.Primitive(Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null), !nonNull)
            "String", "ID" -> Reference.Primitive(Reference.Primitive.Type.String(null), !nonNull)
            "Boolean" -> Reference.Primitive(Reference.Primitive.Type.Boolean, !nonNull)
            else -> Reference.Custom(name, !nonNull)
        }

        is TypeRef.ListOf -> Reference.Iterable(inner.toReference(), !nonNull)
    }

    private fun List<Directive>.toAnnotations(): List<Annotation> = map { directive ->
        Annotation(
            name = directive.name,
            parameters = directive.arguments.map { Annotation.Parameter(it.name, it.value.toAnnotationValue()) },
        )
    }

    private fun Value.toAnnotationValue(): Annotation.Value = when (this) {
        is Value.Single -> Annotation.Value.Single(value)
        is Value.ListOf -> Annotation.Value.Array(values.map { Annotation.Value.Single(it.value) })
        is Value.ObjectOf -> Annotation.Value.Dict(fields.map { Annotation.Parameter(it.name, it.value.toAnnotationValue()) })
    }
}
