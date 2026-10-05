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
import community.flock.wirespec.compiler.utils.Logger
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
 * Turns Wirespec definitions into a GraphQL document.
 *
 * Definitions that came from [GraphQLParser] carry annotations that rebuild the original document exactly. Any other
 * Wirespec gets the closest GraphQL: types become object types, or input types when an rpc takes them, every rpc
 * becomes a field of `Query`, refined types become scalars, and the types GraphQL has no built-in for become custom
 * scalars. Endpoints and channels have no GraphQL counterpart and are left out.
 */
internal class WirespecToGraphQL(private val definitions: List<Definition>, private val logger: Logger) {

    private val names: Map<String, String> = definitions
        .filterNot { it is Rpc || it.annotations.has(EXTEND) || it.annotations.has(SCHEMA) || it.annotations.has(DIRECTIVE_DEFINITION) }
        .associate { it.identifier.value to (it.annotations.single(NAME) ?: it.identifier.value) }

    /**
     * The rpcs of each root operation type, by type and extension block. An rpc that did not come from a GraphQL root
     * field belongs to `Query`.
     */
    private val rpcs: Map<Pair<String, String?>, List<Rpc>> = definitions.filterIsInstance<Rpc>().groupBy { it.rootType() }

    private val types: Map<String, Type> = definitions.filterIsInstance<Type>().associateBy { it.identifier.value }

    /** Plain types that an rpc takes, directly or through other types; GraphQL needs input types there. */
    private val inputTypes: Set<String> = types.reachableFrom(
        definitions.filterIsInstance<Rpc>().flatMap { rpc -> rpc.shape.value.map { it.reference } },
    )

    /** Plain types that are also read, so they need an object type as well as an input type. */
    private val dualTypes: Set<String> = inputTypes intersect types.reachableFrom(
        definitions.filterIsInstance<Rpc>().map { it.result } +
            definitions.filterIsInstance<Union>().flatMap { it.entries } +
            types.values.filter { it.identifier.value !in inputTypes }.flatMap { type -> type.shape.value.map { it.reference } },
    )

    fun convert(): Document = Document(
        definitions.flatMap { it.convert() } + customScalars(),
    )

    private fun Definition.convert(): List<GraphQLModel.Definition> = when (this) {
        is Rpc -> rootType().let { rootType ->
            rpcs.getValue(rootType)
                .takeIf { rootType.first !in types && it.first() === this }
                ?.let { listOf(ObjectTypeDefinition(null, rootType.first, emptyList(), emptyList(), it.map { rpc -> rpc.toFieldDefinition() }, rootType.second != null)) }
                .orEmpty()
        }
        is Endpoint -> emptyList<GraphQLModel.Definition>().also { logger.warn("Endpoint ${identifier.value} has no GraphQL counterpart and is left out") }
        is Channel -> emptyList<GraphQLModel.Definition>().also { logger.warn("Channel ${identifier.value} has no GraphQL counterpart and is left out") }
        is Refined -> when {
            annotations.has(BUILT_IN) -> emptyList()
            else -> listOf(ScalarTypeDefinition(description(), graphQLName(), annotations.directives(), annotations.has(EXTEND)))
        }
        is Enum -> listOf(
            EnumTypeDefinition(
                description = description(),
                name = graphQLName(),
                directives = annotations.directives(),
                values = entries.map { entry ->
                    annotations.named(ENUM_VALUE).firstOrNull { it.single("value") == entry }.let {
                        EnumValueDefinition(it?.single("description"), entry.toEnumValueName(), it?.array("directives").orEmpty().map(::parseDirective))
                    }
                },
                extension = annotations.has(EXTEND),
            ),
        )
        is Union -> listOf(
            UnionTypeDefinition(
                description = description(),
                name = graphQLName(),
                directives = annotations.directives(),
                members = annotations.firstOrNull { it.name == UNION_MEMBERS }?.array(DEFAULT_PARAMETER)
                    ?: entries.map { (it as Reference.Custom).value.let { name -> names[name] ?: name } },
                extension = annotations.has(EXTEND),
            ),
        )
        is Type -> convert()
    }

    private fun Type.convert(): List<GraphQLModel.Definition> {
        val description = description()
        val directives = annotations.directives()
        val extension = annotations.has(EXTEND)
        val interfaces = annotations.firstOrNull { it.name == IMPLEMENTS }?.array(DEFAULT_PARAMETER).orEmpty()
        val fields = shape.value
        fun input(name: String) = InputObjectTypeDefinition(description, name, directives, fields.map { it.toInputValue() }, extension)
        fun objectType() = ObjectTypeDefinition(
            description = description,
            name = graphQLName(),
            interfaces = interfaces,
            directives = directives,
            fields = fields.map { it.toFieldDefinition() } + rpcs[identifier.value to null].orEmpty().map { it.toFieldDefinition() },
            extension = extension,
        )
        return when {
            annotations.has(SCHEMA) -> listOf(
                SchemaDefinition(
                    description = description,
                    directives = directives,
                    operationTypes = annotations.first { it.name == SCHEMA }.let { schema ->
                        schema.parameters.map { parameter -> OperationTypeDefinition(Operation.entries.first { it.keyword == parameter.name }, schema.single(parameter.name)!!) }
                    },
                    extension = extension,
                ),
            )
            annotations.has(DIRECTIVE_DEFINITION) -> annotations.first { it.name == DIRECTIVE_DEFINITION }.let {
                listOf(
                    DirectiveDefinition(
                        description = description,
                        name = it.single("name")!!,
                        arguments = fields.map { field -> field.toInputValue() },
                        repeatable = it.single("repeatable") == "true",
                        locations = it.array("locations"),
                    ),
                )
            }
            annotations.has(INPUT) -> listOf(input(graphQLName()))
            annotations.has(INTERFACE) -> listOf(InterfaceTypeDefinition(description, graphQLName(), interfaces, directives, fields.map { it.toFieldDefinition() }, extension))
            annotations.has(UNION) -> listOf(UnionTypeDefinition(description, graphQLName(), directives, emptyList(), extension))
            annotations.has(ENUM) -> listOf(EnumTypeDefinition(description, graphQLName(), directives, emptyList(), extension))
            identifier.value in dualTypes -> listOf(objectType(), input(identifier.value.inputName()))
            identifier.value in inputTypes -> listOf(input(graphQLName()))
            else -> listOf(objectType())
        }
    }

    /** Declares the custom scalars that stand in for the Wirespec types GraphQL has no built-in for. */
    private fun customScalars(): List<ScalarTypeDefinition> = definitions
        .flatMap { definition ->
            when (definition) {
                is Type -> definition.shape.value.map { it.reference }
                is Rpc -> definition.shape.value.map { it.reference } + definition.result
                else -> emptyList()
            }
        }
        .mapNotNull { it.leaf().customScalar() }
        .distinct()
        .filterNot { it in names }
        .map { ScalarTypeDefinition(null, it, emptyList(), false) }

    /** The root operation type an rpc is a field of, rebuilt at the first of its rpcs unless a Wirespec type holds it. */
    private fun Rpc.rootType(): Pair<String, String?> = annotations.firstOrNull { it.name == FIELD }
        .let { (it?.single("parent") ?: QUERY) to it?.single("extend") }

    private fun Definition.graphQLName(): String = annotations.single(EXTEND) ?: annotations.single(NAME) ?: identifier.value

    private fun Definition.description(): String? = annotations.description() ?: comment?.value

    private fun Field.toFieldDefinition(): FieldDefinition = FieldDefinition(
        description = annotations.description(),
        name = identifier.value.toGraphQLName(),
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
        description = description(),
        name = annotations.firstOrNull { it.name == FIELD }?.single("name") ?: identifier.value.replaceFirstChar(Char::lowercase),
        arguments = shape.value.map { it.toInputValue() },
        type = annotations.single(TYPE)?.let(::parseType) ?: result.toTypeRef(input = false),
        directives = annotations.directives(),
    ).also { if (error != null) logger.warn("The error type of rpc ${identifier.value} has no GraphQL counterpart and is left out") }

    private fun Field.toInputValue(): InputValueDefinition = InputValueDefinition(
        description = annotations.description(),
        name = identifier.value.toGraphQLName(),
        type = typeRef(input = true),
        defaultValue = annotations.single(DEFAULT)?.let(::parseValue) ?: defaultValue?.toGraphQLValue(),
        directives = annotations.directives(),
    )

    private fun Field.typeRef(input: Boolean = false): TypeRef = annotations.single(TYPE)?.let(::parseType) ?: reference.toTypeRef(input)

    private fun Reference.toTypeRef(input: Boolean): TypeRef = when (this) {
        is Reference.Iterable -> TypeRef.ListOf(reference.toTypeRef(input))
        is Reference.Custom -> TypeRef.Named(if (input && value in dualTypes) value.inputName() else names[value] ?: value)
        else -> TypeRef.Named(customScalar() ?: primitiveName())
    }.let { if (isNullable) it else TypeRef.NonNull(it) }

    private fun Reference.primitiveName(): String = when (val type = (this as Reference.Primitive).type) {
        is Reference.Primitive.Type.Integer -> "Int"
        is Reference.Primitive.Type.Number -> "Float"
        is Reference.Primitive.Type.String -> "String"
        is Reference.Primitive.Type.Boolean -> "Boolean"
        is Reference.Primitive.Type.Bytes -> error("$type is a custom scalar")
    }

    private fun List<Annotation>.description(): String? = single(DESCRIPTION)

    private fun List<Annotation>.directives(): List<GraphQLDirective> = named(DIRECTIVE).map { parseDirective(it.single(DEFAULT_PARAMETER)!!) }

    private companion object {
        const val QUERY = "Query"
        val GRAPHQL_NAME = Regex("[_A-Za-z][_0-9A-Za-z]*")

        fun String.inputName(): String = "${this}Input"

        /** The custom scalar standing in for a Wirespec type GraphQL has no built-in for: a 64-bit integer, bytes, `Any`, a dictionary or `Unit`. */
        fun Reference.customScalar(): String? = when (this) {
            is Reference.Primitive -> when (val type = type) {
                is Reference.Primitive.Type.Integer -> "Long".takeIf { type.precision == Reference.Primitive.Type.Precision.P64 }
                is Reference.Primitive.Type.Bytes -> "Bytes"
                else -> null
            }
            is Reference.Any, is Reference.Dict -> "JSON"
            is Reference.Unit -> "Void"
            is Reference.Custom, is Reference.Iterable -> null
        }

        fun Reference.leaf(): Reference = when (this) {
            is Reference.Iterable -> reference.leaf()
            else -> this
        }

        fun Map<String, Type>.reachableFrom(references: List<Reference>): Set<String> = generateSequence(
            references.mapNotNull { (it.leaf() as? Reference.Custom)?.value }.filter { it in this && this.getValue(it).isPlain() }.toSet(),
        ) { found ->
            (found + found.flatMap { name -> getValue(name).shape.value.mapNotNull { (it.reference.leaf() as? Reference.Custom)?.value } }.filter { it in this && getValue(it).isPlain() })
                .takeIf { it.size > found.size }
        }.last()

        /** A type that did not come from a GraphQL interface, input, schema or directive definition. */
        fun Type.isPlain(): Boolean = listOf(SCHEMA, DIRECTIVE_DEFINITION, INPUT, INTERFACE, UNION, ENUM).none { annotations.has(it) }

        fun String.toGraphQLName(): String = takeIf { GRAPHQL_NAME.matches(it) } ?: replace(Regex("[^_0-9A-Za-z]"), "_").let { if (it.first().isDigit()) "_$it" else it }

        fun String.toEnumValueName(): String = toGraphQLName().let { if (it in setOf("true", "false", "null")) it.uppercase() else it }

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

        /** Reads back an annotation value, which the Wirespec parser keeps with its escape sequences. */
        fun String.fromLiteral(): String = buildString {
            var index = 0
            while (index < this@fromLiteral.length) {
                val char = this@fromLiteral[index]
                when {
                    char == '\\' && index + 1 < this@fromLiteral.length -> when (val escaped = this@fromLiteral[index + 1]) {
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        'u' -> append(this@fromLiteral.substring(index + 2, minOf(index + 6, this@fromLiteral.length)).toInt(16).toChar()).also { index += 4 }
                        else -> append(escaped)
                    }.also { index += 2 }
                    else -> append(char).also { index++ }
                }
            }
        }
    }
}
