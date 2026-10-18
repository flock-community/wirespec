package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
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
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.IMPLEMENTS
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.INPUT
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.INTERFACE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.NAME
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.SCHEMA
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION_MEMBERS
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotationName
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
 * The inverse of [GraphQLParser]: rebuilds the GraphQL document from the Wirespec definitions and their annotations,
 * which gives back the schema it was converted from, but for the fields an implementation declares differently from
 * its interface. A type that spreads the part of an interface implements that interface. Wirespec that does not come
 * from GraphQL gets the closest GraphQL counterpart.
 */
internal class WirespecToGraphQL(private val definitions: List<Definition>) {

    private val names: Map<String, String> = definitions
        .filterNot { it is Rpc || it.annotations.has(EXTEND) || it.annotations.has(SCHEMA) || it.annotations.has(DIRECTIVE_DEFINITION) }
        .associate { it.identifier.value to it.graphQLName() }

    private val parts: Map<String, Part> = definitions.filterIsInstance<Part>().associateBy { it.identifier.value }

    /** The interfaces that keep their fields in a part, by the name of that part. */
    private val interfaceParts: Map<String, Definition> = definitions
        .mapNotNull { definition -> definition.annotations.single(INTERFACE)?.let { it to definition } }
        .toMap()

    /** The rpcs of each root operation type, by type and extension block. */
    private val rpcs: Map<Pair<String, String?>, List<Rpc>> = definitions.filterIsInstance<Rpc>().groupBy { it.rootType() }

    private val types: Set<String> = definitions.filterIsInstance<Type>().map { it.identifier.value }.toSet()

    /** The types that no annotation gives a GraphQL kind, which become an object type, an input object or both. */
    private val plainTypes: Map<String, Type> = definitions.filterIsInstance<Type>()
        .filter { type -> KINDS.none { type.annotations.has(it) } }
        .associateBy { it.identifier.value }

    /** GraphQL only takes input objects as arguments, so a type that an rpc takes, directly or through its fields, is one. */
    private val inputs: Set<String> = definitions.filterIsInstance<Rpc>()
        .flatMap { rpc -> rpc.shape.fields.map { it.reference } }
        .reachablePlainTypes()

    private val outputs: Set<String> = definitions
        .flatMap { definition ->
            when (definition) {
                is Rpc -> listOf(definition.result)
                is Union -> definition.entries.toList()
                is Type -> definition.shape.fields.map { it.reference }.takeUnless { definition.identifier.value in inputs }.orEmpty()
                is Part -> definition.shape.fields.map { it.reference }.takeIf { definition.identifier.value in interfaceParts }.orEmpty()
                is Endpoint, is Channel, is Enum, is Refined -> emptyList()
            }
        }
        .reachablePlainTypes()

    /** A type that is both taken and returned is written twice, as an object type and as an input object named after it. */
    private val inputNames: Map<String, String> = (inputs intersect outputs).associateWith { type ->
        "${plainTypes.getValue(type).graphQLName()}Input".let { name ->
            (sequenceOf(name) + generateSequence(2) { it + 1 }.map { "$name$it" }).first { it !in names.values }
        }
    }

    private val usesJson: Boolean = definitions.any { definition ->
        when (definition) {
            is Type -> definition.shape.fields.map { it.reference }
            is Part -> definition.shape.fields.map { it.reference }
            is Rpc -> definition.shape.fields.map { it.reference } + definition.result
            is Endpoint, is Channel, is Enum, is Union, is Refined -> emptyList()
        }.any { it.isJson() }
    }

    fun convert(): Document = Document(
        definitions.flatMap { listOfNotNull(it.convert(), (it as? Type)?.toInputCopy()) } +
            listOfNotNull(ScalarTypeDefinition(null, JSON, emptyList(), false).takeIf { usesJson && JSON !in names.values }),
    )

    private fun Definition.convert(): GraphQLModel.Definition? = when (this) {
        // A root operation type without a Wirespec type of its own is rebuilt at its first rpc.
        is Rpc -> rootType().let { rootType ->
            rpcs.getValue(rootType)
                .takeIf { rootType.first !in types && it.first() === this }
                ?.let { ObjectTypeDefinition(null, rootType.first, emptyList(), emptyList(), it.map { rpc -> rpc.toFieldDefinition() }, rootType.second != null) }
        }
        is Refined -> when {
            annotations.has(BUILT_IN) -> null
            else -> ScalarTypeDefinition(description(), graphQLName(), annotations.directives(), annotations.has(EXTEND))
        }
        is Enum -> EnumTypeDefinition(
            description = description(),
            name = graphQLName(),
            directives = annotations.directives(),
            values = entries.map { entry ->
                annotations.named(ENUM_VALUE).firstOrNull { it.single("value") == entry }.let {
                    EnumValueDefinition(it?.single("description"), entry, it?.array("directives").orEmpty().map(::parseDirective))
                }
            },
            extension = annotations.has(EXTEND),
        )
        is Union -> takeIf { annotations.has(INTERFACE) }?.toInterface() ?: UnionTypeDefinition(
            description = description(),
            name = graphQLName(),
            directives = annotations.directives(),
            members = annotations.firstOrNull { it.name == UNION_MEMBERS }?.array(DEFAULT_PARAMETER)
                ?: entries.map { (it as Reference.Custom).value.let { name -> names[name] ?: name } },
            extension = annotations.has(EXTEND),
        )
        is Type -> convert()
        // A part is spread into the types that use it, and holds the fields of an interface that names it.
        is Part -> null
        // GraphQL has nothing like an HTTP endpoint or a channel.
        is Endpoint, is Channel -> null
    }

    /** `@GraphQLInterface` names the part with the fields of the interface, or marks a type holding them itself. */
    private fun Definition.toInterface(): InterfaceTypeDefinition = (annotations.single(INTERFACE)?.let { parts[it]?.shape } ?: (this as? Type)?.shape ?: Shape(emptyList())).let { shape ->
        InterfaceTypeDefinition(description(), graphQLName(), interfaces(shape), annotations.directives(), shape.fields.map { it.toFieldDefinition() }, annotations.has(EXTEND))
    }

    private fun Type.convert(): GraphQLModel.Definition {
        val description = description()
        val directives = annotations.directives()
        val extension = annotations.has(EXTEND)
        val fields = shape.fields
        return when {
            annotations.has(SCHEMA) -> SchemaDefinition(
                description = description,
                directives = directives,
                operationTypes = annotations.first { it.name == SCHEMA }.let { schema ->
                    schema.parameters.map { parameter -> OperationTypeDefinition(Operation.entries.first { it.keyword == parameter.name }, schema.single(parameter.name)!!) }
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
            annotations.has(INTERFACE) -> toInterface()
            annotations.has(INPUT) || identifier.value in inputs - outputs ->
                InputObjectTypeDefinition(description, graphQLName(), directives, fields.map { it.toInputValue() }, extension)
            annotations.has(UNION) -> UnionTypeDefinition(description, graphQLName(), directives, emptyList(), extension)
            annotations.has(ENUM) -> EnumTypeDefinition(description, graphQLName(), directives, emptyList(), extension)
            else -> ObjectTypeDefinition(
                description = description,
                name = graphQLName(),
                interfaces = interfaces(shape),
                directives = directives,
                fields = fields.map { it.toFieldDefinition() } + rpcs[identifier.value to null].orEmpty().map { it.toFieldDefinition() },
                extension = extension,
            )
        }
    }

    private fun Type.toInputCopy(): InputObjectTypeDefinition? = inputNames[identifier.value]?.let { name ->
        InputObjectTypeDefinition(description(), name, emptyList(), shape.fields.map { it.toInputValue() }, false)
    }

    /** An rpc without an operation annotation does not come from GraphQL, and is a query. */
    private fun Rpc.rootType(): Pair<String, String?> = Operation.entries
        .firstNotNullOfOrNull { operation -> annotations.firstOrNull { it.name == operation.annotationName }?.let { operation to it } }
        ?.let { (operation, annotation) -> (annotation.single(DEFAULT_PARAMETER) ?: operation.defaultTypeName) to annotation.single("extend") }
        ?: (Operation.QUERY.defaultTypeName to null)

    private fun Definition.graphQLName(): String = annotations.single(EXTEND) ?: annotations.single(NAME) ?: identifier.value

    /** What a shape implements: the interfaces of the parts it spreads, and those listed in `@GraphQLImplements`. */
    private fun Definition.interfaces(shape: Shape): List<String> = (
        shape.value.filterIsInstance<Spread>().flatMap { it.interfaces() } +
            annotations.firstOrNull { it.name == IMPLEMENTS }?.array(DEFAULT_PARAMETER).orEmpty()
        ).distinct()

    /** Spreading the part of an interface implements it, and everything it implements in turn. */
    private fun Spread.interfaces(): List<String> = parts[reference.value]
        ?.let { part -> interfaceParts[part.identifier.value]?.let { listOf(it.graphQLName()) + it.interfaces(part.shape) } ?: part.interfaces(part.shape) }
        .orEmpty()

    private fun Definition.description(): String? = annotations.description() ?: comment?.value

    /** The plain types these references name, and the plain types that the fields of those name, and so on. */
    private fun List<Reference>.reachablePlainTypes(): Set<String> = generateSequence(plainTypeNames()) { found ->
        (found + found.flatMap { plainTypes.getValue(it).shape.fields.map { field -> field.reference }.plainTypeNames() })
            .takeIf { it.size > found.size }
    }.last()

    private fun List<Reference>.plainTypeNames(): Set<String> = mapNotNull { it.customName() }.filter { it in plainTypes }.toSet()

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
        type = typeRef(input = false),
        directives = annotations.directives(),
    )

    private fun Rpc.toFieldDefinition(): FieldDefinition = FieldDefinition(
        description = description(),
        name = annotations.single(NAME) ?: identifier.value.replaceFirstChar(Char::lowercase),
        arguments = shape.fields.map { it.toInputValue() },
        type = annotations.single(TYPE)?.let(::parseType) ?: result.toTypeRef(input = false),
        directives = annotations.directives(),
    )

    private fun Field.toInputValue(): InputValueDefinition = InputValueDefinition(
        description = annotations.description(),
        name = identifier.value,
        type = typeRef(input = true),
        defaultValue = annotations.single(DEFAULT)?.let(::parseValue) ?: defaultValue?.toGraphQLValue(),
        directives = annotations.directives(),
    )

    private fun Field.typeRef(input: Boolean): TypeRef = annotations.single(TYPE)?.let(::parseType) ?: reference.toTypeRef(input)

    private fun Reference.toTypeRef(input: Boolean): TypeRef = when (this) {
        is Reference.Iterable -> TypeRef.ListOf(reference.toTypeRef(input))
        is Reference.Custom -> TypeRef.Named(inputNames[value]?.takeIf { input } ?: names[value] ?: value)
        is Reference.Primitive -> TypeRef.Named(
            when (type) {
                is Reference.Primitive.Type.Integer -> "Int"
                is Reference.Primitive.Type.Number -> "Float"
                // GraphQL has no binary type, so bytes go as a (base64) string.
                is Reference.Primitive.Type.String, is Reference.Primitive.Type.Bytes -> "String"
                is Reference.Primitive.Type.Boolean -> "Boolean"
            },
        )
        // GraphQL has no void, a field without a result conventionally returns a Boolean.
        is Reference.Unit -> TypeRef.Named("Boolean")
        is Reference.Any, is Reference.Dict -> TypeRef.Named(JSON)
    }.let { if (isNullable) it else TypeRef.NonNull(it) }

    private fun List<Annotation>.description(): String? = single(DESCRIPTION)

    private fun List<Annotation>.directives(): List<GraphQLDirective> = named(DIRECTIVE).map { parseDirective(it.single(DEFAULT_PARAMETER)!!) }

    private companion object {
        /** GraphQL has no type for any value or for a map, so they become a `JSON` scalar. */
        const val JSON = "JSON"

        val KINDS = listOf(INPUT, INTERFACE, UNION, ENUM, SCHEMA, DIRECTIVE_DEFINITION)

        val Definition.annotations: List<Annotation> get() = (this as? HasAnnotations)?.annotations.orEmpty()

        fun Reference.customName(): String? = when (this) {
            is Reference.Iterable -> reference.customName()
            is Reference.Custom -> value
            is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Primitive -> null
        }

        fun Reference.isJson(): Boolean = when (this) {
            is Reference.Iterable -> reference.isJson()
            is Reference.Any, is Reference.Dict -> true
            is Reference.Unit, is Reference.Custom, is Reference.Primitive -> false
        }

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
