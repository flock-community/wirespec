package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.ARGUMENT
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.BUILT_IN
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DEFAULT
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DEFAULT_PARAMETER
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DESCRIPTION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DIRECTIVE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DIRECTIVE_DEFINITION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.ENUM
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.ENUM_VALUE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.EXTEND
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.FIELD
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.IMPLEMENTS
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.INPUT
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.INTERFACE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.NAME
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.SCHEMA
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION_MEMBERS
import community.flock.wirespec.converter.graphql.GraphQLModel.DirectiveDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Operation
import community.flock.wirespec.converter.graphql.GraphQLModel.OperationTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Value
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive as GraphQLDirective

/**
 * The inverse of [GraphQLParser]: rebuilds the GraphQL document from the Wirespec definitions and their annotations.
 * Tests use it to show the conversion is lossless.
 */
internal class WirespecToGraphQL(private val definitions: List<Definition>) {

    private val names: Map<String, String> = definitions
        .filterNot { it is Rpc || it.annotations.has(EXTEND) || it.annotations.has(SCHEMA) || it.annotations.has(DIRECTIVE_DEFINITION) }
        .associate { it.identifier.value to (it.annotations.single(NAME) ?: it.identifier.value) }

    private val rpcs: Map<String?, List<Rpc>> = definitions.filterIsInstance<Rpc>()
        .groupBy { rpc -> rpc.annotations.firstOrNull { it.name == FIELD }?.single("parent") }

    fun convert(): Document = Document(definitions.mapNotNull { it.convert() })

    private fun Definition.convert(): GraphQLModel.Definition? = when (this) {
        is Rpc, is Endpoint, is Channel -> null
        is Refined -> when {
            annotations.has(BUILT_IN) -> null
            else -> ScalarTypeDefinition(annotations.description(), graphQLName(), annotations.directives(), annotations.has(EXTEND))
        }
        is Enum -> EnumTypeDefinition(
            description = annotations.description(),
            name = graphQLName(),
            directives = annotations.directives(),
            values = entries.map { entry ->
                annotations.named(ENUM_VALUE).firstOrNull { it.single("value") == entry }.let {
                    EnumValueDefinition(it?.single("description"), entry, it?.array("directives").orEmpty().map(::parseDirective))
                }
            },
            extension = annotations.has(EXTEND),
        )
        is Union -> UnionTypeDefinition(
            description = annotations.description(),
            name = graphQLName(),
            directives = annotations.directives(),
            members = annotations.firstOrNull { it.name == UNION_MEMBERS }?.array(DEFAULT_PARAMETER)
                ?: entries.map { (it as Reference.Custom).value.let { name -> names[name] ?: name } },
            extension = annotations.has(EXTEND),
        )
        is Type -> convert()
    }

    private fun Type.convert(): GraphQLModel.Definition {
        val description = annotations.description()
        val directives = annotations.directives()
        val extension = annotations.has(EXTEND)
        val interfaces = annotations.firstOrNull { it.name == IMPLEMENTS }?.array(DEFAULT_PARAMETER).orEmpty()
        val fields = shape.value
        return when {
            annotations.has(SCHEMA) -> SchemaDefinition(
                description = description,
                directives = directives,
                operationTypes = fields.map { field ->
                    OperationTypeDefinition(Operation.entries.first { it.keyword == field.identifier.value }, (field.typeRef() as TypeRef.NonNull).type.let { (it as TypeRef.Named).name })
                },
                extension = extension,
            )
            annotations.has(DIRECTIVE_DEFINITION) -> annotations.first { it.name == DIRECTIVE_DEFINITION }.let {
                DirectiveDefinition(
                    description = description,
                    name = it.single("name")!!,
                    arguments = fields.map { field -> field.toInputValue() },
                    repeatable = it.single("repeatable") == "true",
                    locations = it.array("locations"),
                )
            }
            annotations.has(INPUT) -> InputObjectTypeDefinition(description, graphQLName(), directives, fields.map { it.toInputValue() }, extension)
            annotations.has(INTERFACE) -> InterfaceTypeDefinition(description, graphQLName(), interfaces, directives, fields.map { it.toFieldDefinition() }, extension)
            annotations.has(UNION) -> UnionTypeDefinition(description, graphQLName(), directives, emptyList(), extension)
            annotations.has(ENUM) -> EnumTypeDefinition(description, graphQLName(), directives, emptyList(), extension)
            else -> ObjectTypeDefinition(
                description = description,
                name = graphQLName(),
                interfaces = interfaces,
                directives = directives,
                fields = fields.map { it.toFieldDefinition() } + rpcs[identifier.value].orEmpty().map { it.toFieldDefinition() },
                extension = extension,
            )
        }
    }

    private fun Definition.graphQLName(): String = annotations.single(EXTEND) ?: annotations.single(NAME) ?: identifier.value

    private fun Field.toFieldDefinition(): FieldDefinition = FieldDefinition(
        description = annotations.description(),
        name = identifier.value,
        arguments = annotations.named(ARGUMENT).map {
            InputValueDefinition(
                description = it.single("description"),
                name = it.single("name")!!,
                type = parseType(it.single("type")!!),
                defaultValue = it.single("defaultValue")?.let(::parseValue),
                directives = it.array("directives").map(::parseDirective),
            )
        },
        type = typeRef(),
        directives = annotations.directives(),
    )

    private fun Rpc.toFieldDefinition(): FieldDefinition = FieldDefinition(
        description = annotations.description(),
        name = annotations.first { it.name == FIELD }.single("name")!!,
        arguments = shape.value.map { it.toInputValue() },
        type = annotations.single(TYPE)?.let(::parseType) ?: result.toTypeRef(),
        directives = annotations.directives(),
    )

    private fun Field.toInputValue(): InputValueDefinition = InputValueDefinition(
        description = annotations.description(),
        name = identifier.value,
        type = typeRef(),
        defaultValue = annotations.single(DEFAULT)?.let(::parseValue) ?: defaultValue?.toGraphQLValue(),
        directives = annotations.directives(),
    )

    private fun Field.typeRef(): TypeRef = annotations.single(TYPE)?.let(::parseType) ?: reference.toTypeRef()

    private fun Reference.toTypeRef(): TypeRef = when (this) {
        is Reference.Iterable -> TypeRef.ListOf(reference.toTypeRef())
        is Reference.Custom -> TypeRef.Named(names[value] ?: value)
        is Reference.Primitive -> TypeRef.Named(
            when (type) {
                is Reference.Primitive.Type.Integer -> "Int"
                is Reference.Primitive.Type.Number -> "Float"
                is Reference.Primitive.Type.String -> "String"
                is Reference.Primitive.Type.Boolean -> "Boolean"
                is Reference.Primitive.Type.Bytes -> error("Bytes has no GraphQL counterpart")
            },
        )
        is Reference.Any, is Reference.Unit, is Reference.Dict -> error("$value has no GraphQL counterpart")
    }.let { if (isNullable) it else TypeRef.NonNull(it) }

    private fun List<Annotation>.description(): String? = single(DESCRIPTION)

    private fun List<Annotation>.directives(): List<GraphQLDirective> = named(DIRECTIVE).map { parseDirective(it.single(DEFAULT_PARAMETER)!!) }

    private companion object {
        fun List<Annotation>.named(name: String): List<Annotation> = filter { it.name == name }

        fun List<Annotation>.has(name: String): Boolean = any { it.name == name }

        fun List<Annotation>.single(name: String): String? = firstOrNull { it.name == name }?.single(DEFAULT_PARAMETER)

        fun Annotation.single(parameter: String): String? = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Single)
            ?.value
            ?.fromLiteral()

        fun Annotation.array(parameter: String): List<String> = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Array)
            ?.value
            ?.map { it.value.fromLiteral() }
            .orEmpty()

        fun parseType(type: String): TypeRef = (GraphQLDocumentParser("input X { x: $type }").parseDocument().definitions.single() as InputObjectTypeDefinition).fields.single().type

        fun parseValue(value: String): Value = (GraphQLDocumentParser("input X { x: Int = $value }").parseDocument().definitions.single() as InputObjectTypeDefinition)
            .fields.single().defaultValue!!

        fun parseDirective(directive: String): GraphQLDirective = (GraphQLDocumentParser("scalar X $directive").parseDocument().definitions.single() as ScalarTypeDefinition)
            .directives.single()

        fun String.fromLiteral(): String = buildString {
            var index = 0
            while (index < this@fromLiteral.length) {
                val char = this@fromLiteral[index]
                when {
                    char == '\\' -> when (val escaped = this@fromLiteral[index + 1]) {
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        'u' -> append(this@fromLiteral.substring(index + 2, index + 6).toInt(16).toChar()).also { index += 4 }
                        else -> append(escaped)
                    }.also { index += 2 }
                    else -> append(char).also { index++ }
                }
            }
        }
    }
}
