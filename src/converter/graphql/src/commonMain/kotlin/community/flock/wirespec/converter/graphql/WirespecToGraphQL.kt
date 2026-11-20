package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.parse.ast.AST
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
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.PATH_PARAMETER
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.SCHEMA
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE_PARAMETER
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION_MEMBERS
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotationName
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.fieldName
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
 * its interface. A type that spreads the part of an interface implements that interface. An rpc is a field of the type
 * its annotation names, `@GraphQLQuery("User")`. That is a type that a field in the header of its file leads to,
 * `@file:GraphQLQuery("Query", path: { notifications: "NotificationsQuery!" })`, which nests that type in `Query`,
 * or else a type that holds data, or else the root operation type. Wirespec that does not come from GraphQL gets the
 * closest GraphQL counterpart.
 *
 * The schema and directive definitions are `@spec:` annotations of the [ast], so they come first, in the order of the
 * file header.
 */
internal class WirespecToGraphQL(ast: AST) {

    private val definitions: List<Definition> = ast.modules.flatMap { it.statements }

    private val spec: List<Annotation> = ast.annotations.map { it.annotation }

    /** The `@file:` annotations of the file each rpc is in, which say where the types its operations name are nested. */
    private val fileAnnotations: Map<String, List<Annotation>> = ast.modules.toList()
        .flatMap { module -> module.statements.filterIsInstance<Rpc>().map { it.identifier.value to module.annotations } }
        .toMap()

    private val names: Map<String, String> = definitions
        .filterNot { it is Rpc || it.annotations.has(EXTEND) }
        .associate { it.identifier.value to it.graphQLName() }

    private val parts: Map<String, Part> = definitions.filterIsInstance<Part>().associateBy { it.identifier.value }

    /** The interfaces that keep their fields in a part, by the name of that part. */
    private val interfaceParts: Map<String, Definition> = definitions
        .mapNotNull { definition -> definition.annotations.single(INTERFACE)?.let { it to definition } }
        .toMap()

    /** A union marked `@GraphQLInterface` without a part is an interface that its members implement, with what it implements. */
    private val memberships: Map<String, List<String>> = definitions
        .filterIsInstance<Union>()
        .filter { it.annotations.has(INTERFACE) && it.annotations.single(INTERFACE) == null }
        .flatMap { union -> union.entries.filterIsInstance<Reference.Custom>().map { it.value to listOf(union.graphQLName()) + union.annotations.implements() } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, interfaces) -> interfaces.flatten() }

    private val types: Map<String, Type> = definitions.filterIsInstance<Type>().associateBy { it.identifier.value }

    /** The definitions that the fields of rpcs are added to: the types, and the unions that are interfaces. */
    private val fieldHolders: Map<String, Definition> = definitions.filter { it is Type || it is Union }.associateBy { it.identifier.value }

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

    /** The fields that the rpcs add to each type, by type and extension block, in the order of the rpcs. */
    private val members: Map<Pair<String, String?>, List<Member>> = definitions.filterIsInstance<Rpc>()
        .flatMap { it.members() }
        .groupBy { it.owner }
        .mapValues { (_, members) -> members.merged() }

    /** The GraphQL name of a type, enum, union or scalar. */
    fun graphQLName(definition: String): String = names[definition] ?: definition

    /** The root operation an rpc is a field of: an rpc without an operation annotation is a query. */
    fun operation(rpc: Rpc): Operation = Operation.entries.firstOrNull { operation -> rpc.annotations.has(operation.annotationName) } ?: Operation.QUERY

    fun fieldDefinition(rpc: Rpc): FieldDefinition = rpc.toFieldDefinition()

    /**
     * Whether an rpc is a function of a type that holds data, which is no operation of its own: it names a type that
     * is neither nested in a root type by a path of its file, nor the root type itself.
     */
    fun isFunction(rpc: Rpc): Boolean = rpc.dataType() != null

    /** The type that holds data which an rpc names, by its GraphQL name. */
    private fun Rpc.dataType(): String? = operation(this).let { operation ->
        annotations.firstOrNull { it.name == operation.annotationName }?.single(DEFAULT_PARAMETER)
            ?.takeIf { route().isEmpty() }
            ?.let { fieldHolders[it]?.graphQLName() }
            ?.takeIf { it != rootType(operation) }
    }

    /** The GraphQL name of the root type an operation is a field of or nested in. */
    fun rootType(rpc: Rpc): String = rpc.rootType().first.let { fieldHolders[it]?.graphQLName() ?: it }

    /** The root type of an operation: the one `@spec:GraphQLSchema` names, or else the default. */
    fun rootType(operation: Operation): String = spec.named(SCHEMA).firstNotNullOfOrNull { it.single(operation.keyword) } ?: operation.defaultTypeName

    /** The fields that lead from the root type to the type an operation names: those of the `path` in its file that leads there. */
    fun path(rpc: Rpc): List<String> = rpc.pathFields().map { it.name }

    fun convert(): Document = Document(
        spec.mapNotNull { it.toDefinition() } +
            definitions.flatMap { it.convert() + listOfNotNull((it as? Type)?.toInputCopy()) } +
            listOfNotNull(ScalarTypeDefinition(null, JSON, emptyList(), false).takeIf { usesJson && JSON !in names.values }),
    )

    /** A schema or directive definition; any other `@spec:` annotation is not for GraphQL. */
    private fun Annotation.toDefinition(): GraphQLModel.Definition? = when (name) {
        SCHEMA -> SchemaDefinition(
            description = single("description"),
            directives = array("directives").map(::parseDirective),
            operationTypes = parameters.mapNotNull { parameter ->
                Operation.entries.find { it.keyword == parameter.name }?.let { OperationTypeDefinition(it, single(parameter.name)!!) }
            },
            extension = single("extend") == "true",
        )
        DIRECTIVE_DEFINITION -> DirectiveDefinition(
            description = single("description"),
            name = single("name")!!,
            arguments = dictionaries("arguments").map { it.toInputValue(it.name) },
            repeatable = single("repeatable") == "true",
            locations = array("locations"),
        )
        else -> null
    }

    private fun Definition.convert(): List<GraphQLModel.Definition> = when (this) {
        // A type without a Wirespec definition of its own is rebuilt at the first rpc that adds a field to it.
        is Rpc ->
            members
                .filter { (owner, members) -> (owner.second != null || owner.first !in fieldHolders) && members.first().rpc === this }
                .map { (owner, members) -> ObjectTypeDefinition(null, owner.first, emptyList(), emptyList(), members.inOrder(), owner.second != null) }
        else -> listOfNotNull(convertModel())
    }

    private fun Definition.convertModel(): GraphQLModel.Definition? = when (this) {
        is Rpc -> null
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

    /**
     * `@GraphQLInterface` names the part with the fields of the interface. Without one, a type holds the fields itself and
     * a union has the fields that all of its members share.
     */
    private fun Definition.toInterface(): InterfaceTypeDefinition = interfaceShape().let { shape ->
        InterfaceTypeDefinition(description(), graphQLName(), interfaces(shape), annotations.directives(), fieldDefinitions(shape), annotations.has(EXTEND))
    }

    private fun Definition.interfaceShape(): Shape = annotations.single(INTERFACE)?.let { parts[it]?.shape } ?: when (this) {
        is Type -> shape
        is Union -> Shape(sharedFields())
        is Endpoint, is Channel, is Rpc, is Enum, is Refined, is Part -> Shape(emptyList())
    }

    /** The fields with the same name and type in every member, with the annotations all of those carry. */
    private fun Union.sharedFields(): List<Field> = entries
        .map { member -> (member as? Reference.Custom)?.let { types[it.value] }?.shape?.fields.orEmpty() }
        .let { members ->
            members.firstOrNull().orEmpty().mapNotNull { field ->
                members.mapNotNull { fields -> fields.find { it.identifier == field.identifier && it.reference == field.reference } }
                    .takeIf { it.size == members.size }
                    ?.let { alike -> field.copy(annotations = field.annotations.filter { annotation -> alike.all { annotation in it.annotations } }) }
            }
        }

    private fun Type.convert(): GraphQLModel.Definition {
        val description = description()
        val directives = annotations.directives()
        val extension = annotations.has(EXTEND)
        val fields = shape.fields
        return when {
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
                fields = fieldDefinitions(shape),
                extension = extension,
            )
        }
    }

    private fun Type.toInputCopy(): InputObjectTypeDefinition? = inputNames[identifier.value]?.let { name ->
        InputObjectTypeDefinition(description(), name, emptyList(), shape.fields.map { it.toInputValue() }, false)
    }

    /** The fields of a shape, and those its rpcs add after them. */
    private fun Definition.fieldDefinitions(shape: Shape): List<FieldDefinition> = shape.fields.map { it.toFieldDefinition() } + members[identifier.value to null].orEmpty().inOrder()

    /**
     * A field that [rpc] adds to the type [owner], in the `extend` block of a root type that [owner] names next to it.
     * A field that [nests] a type is one of a path, and not the rpc itself.
     */
    private data class Member(val owner: Pair<String, String?>, val field: FieldDefinition, val nests: Boolean, val rpc: Rpc)

    /** The fields in the order of their rpcs, and those that nest a type after the others, wherever the rpcs of that type are. */
    private fun List<Member>.inOrder(): List<FieldDefinition> = sortedBy { it.nests }.map { it.field }

    /**
     * An rpc is a field of the type its annotation names. An operation of a nested type also adds the fields of the path
     * that leads there: each to the type the field before it returns, starting at the root type.
     */
    private fun Rpc.members(): List<Member> = pathFields().let { path ->
        path.runningFold(rootType()) { _, field -> field.leaf()!! to null }.let { owners ->
            path.mapIndexed { index, field -> Member(owners[index], field.toPathField(), nests = true, this) } + Member(owners.last(), toFieldDefinition(), nests = false, this)
        }
    }

    /** A field of the type [parent], in its `extend` block when it is a root type, that leads to a nested type. */
    private data class Link(val parent: Pair<String, String?>, val field: Annotation)

    /** What the header of the file of an operation says about nesting: for each type it names, the fields that lead to a nested type. */
    private fun Rpc.links(): List<Link> = operation(this).let { operation ->
        fileAnnotations[identifier.value].orEmpty()
            .filter { it.name == operation.annotationName }
            .flatMap { annotation -> annotation.dictionaries(PATH_PARAMETER).map { Link((annotation.single(DEFAULT_PARAMETER) ?: operation.defaultTypeName) to annotation.single("extend"), it) } }
    }

    /**
     * The fields that lead from the root type to the type an operation names: the field that leads to that type, the
     * field that leads to the type that one is a field of, and so on. None for an operation of the root type itself.
     */
    private fun Rpc.route(): List<Link> = annotations.firstOrNull { it.name == operation(this).annotationName }?.single(DEFAULT_PARAMETER)
        ?.let { type ->
            links().let { links ->
                generateSequence(links.find { it.field.leaf() == type }) { link -> links.find { it.field.leaf() == link.parent.first } }
                    .take(links.size)
                    .toList()
                    .reversed()
            }
        }
        .orEmpty()

    /** The fields of the path that leads to the type an operation names, each with the type it returns, or with all that describes it. */
    private fun Rpc.pathFields(): List<Annotation> = route().map { it.field }

    /** The type a field of a path returns. */
    private fun Annotation.leaf(): String? = single(TYPE_PARAMETER)?.let(::parseType)?.leaf()

    private fun Annotation.toPathField(): FieldDefinition = FieldDefinition(single("description"), name, emptyList(), parseType(single(TYPE_PARAMETER)!!), array("directives").map(::parseDirective))

    /** Every rpc of a nested type adds the fields that lead to it, and the one that describes a field decides its description and directives. */
    private fun List<Member>.merged(): List<Member> = groupBy { it.field.name }.values.map { same ->
        same.first().let { first ->
            first.copy(
                field = first.field.copy(
                    description = same.firstNotNullOfOrNull { it.field.description },
                    directives = same.map { it.field.directives }.firstOrNull { it.isNotEmpty() }.orEmpty(),
                ),
            )
        }
    }

    /** The root operation type an rpc is a field of or nested in, and the `extend` block it is in. */
    private fun Rpc.rootType(): Pair<String, String?> = operation(this).let { operation ->
        route().firstOrNull()?.parent
            ?: annotations.firstOrNull { it.name == operation.annotationName }?.let { (it.single(DEFAULT_PARAMETER) ?: operation.defaultTypeName) to it.single("extend") }
            ?: (operation.defaultTypeName to null)
    }

    private fun Definition.graphQLName(): String = annotations.single(EXTEND) ?: annotations.single(NAME) ?: identifier.value

    /** What a shape implements: the interfaces of the parts it spreads, of the unions it is in, and those listed in `@GraphQLImplements`. */
    private fun Definition.interfaces(shape: Shape): List<String> = (
        shape.value.filterIsInstance<Spread>().flatMap { it.interfaces() } +
            memberships[identifier.value].orEmpty() +
            annotations.implements()
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
        arguments = emptyList(),
        type = typeRef(input = false),
        directives = annotations.directives(),
    )

    /** An argument as `@GraphQLDirectiveDefinition` holds the one called [name]. */
    private fun Annotation.toInputValue(name: String): InputValueDefinition = InputValueDefinition(
        description = single("description"),
        name = name,
        type = parseType(single(TYPE_PARAMETER)!!),
        defaultValue = single("defaultValue")?.let(::parseValue),
        directives = array("directives").map(::parseDirective),
    )

    private fun Rpc.toFieldDefinition(): FieldDefinition = FieldDefinition(
        description = description(),
        name = annotations.single(NAME) ?: fieldName(identifier.value, dataType()),
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

        val KINDS = listOf(INPUT, INTERFACE, UNION, ENUM)

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

        fun List<Annotation>.implements(): List<String> = firstOrNull { it.name == IMPLEMENTS }?.array(DEFAULT_PARAMETER).orEmpty()

        fun Annotation.single(parameter: String): String? = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Single)
            ?.value
            ?.fromLiteral()

        fun Annotation.array(parameter: String): List<String> = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Array)
            ?.value
            ?.map { it.value.fromLiteral() }
            .orEmpty()

        /**
         * A dictionary of dictionaries, such as the arguments of a directive, each entry read as an annotation named after
         * its key. An entry that is a single value, as a field of a path may be, holds just its type.
         */
        fun Annotation.dictionaries(parameter: String): List<Annotation> = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Dict)
            ?.value
            ?.map { (key, value) ->
                when (value) {
                    is Annotation.Value.Dict -> Annotation(key, value.value)
                    is Annotation.Value.Single -> Annotation(key, listOf(Annotation.Parameter(TYPE_PARAMETER, value)))
                    is Annotation.Value.Array -> Annotation(key, emptyList())
                }
            }
            .orEmpty()

        fun TypeRef.leaf(): String = when (this) {
            is TypeRef.Named -> name
            is TypeRef.ListOf -> type.leaf()
            is TypeRef.NonNull -> type.leaf()
        }

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
