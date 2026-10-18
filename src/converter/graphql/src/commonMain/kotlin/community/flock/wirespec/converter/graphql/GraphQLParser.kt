package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.ShapeEntry
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.coerceTo
import community.flock.wirespec.converter.common.Parser
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
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotation
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotationName
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.interfaceName
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.parameter
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.partName
import community.flock.wirespec.converter.graphql.GraphQLModel.DirectiveDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Document
import community.flock.wirespec.converter.graphql.GraphQLModel.EnumTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.FieldDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ImplementingTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InputValueDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.InterfaceTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.ObjectTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.Operation
import community.flock.wirespec.converter.graphql.GraphQLModel.ScalarTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.SchemaDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLModel.TypeRef
import community.flock.wirespec.converter.graphql.GraphQLModel.UnionTypeDefinition
import community.flock.wirespec.converter.graphql.GraphQLPrinter.print
import community.flock.wirespec.converter.graphql.GraphQLModel.Directive as GraphQLDirective

/**
 * Converts a GraphQL schema (SDL) into Wirespec.
 *
 * Objects and input objects become types, scalars become refined types, enums and unions map onto their Wirespec
 * counterparts and every field of a root operation type (`Query`, `Mutation`, `Subscription`) becomes an `rpc`.
 * An interface becomes a part, which the types implementing it spread. Whatever Wirespec cannot express natively —
 * descriptions, directives, field arguments, default values, extensions — is kept in annotations, so the schema can
 * be rebuilt from the Wirespec definitions.
 */
public object GraphQLParser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = GraphQLDocumentParser(moduleContent.content)
        .parseDocument()
        .let { GraphQLConverter(it).convert() }
        .toNonEmptyListOrNull()
        ?.let { AST(nonEmptyListOf(Module(moduleContent.fileUri, it))) }
        ?: error("Cannot yield an empty AST from GraphQL document ${moduleContent.fileUri.value}")
}

private class GraphQLConverter(private val document: Document) {

    private val rootTypes: Map<Operation, String> = document.definitions.filterIsInstance<SchemaDefinition>().let { schemas ->
        val defaults = Operation.entries.takeIf { schemas.none { !it.extension } }.orEmpty().associateWith { it.defaultTypeName }
        defaults + schemas.flatMap { it.operationTypes }.associate { it.operation to it.type }
    }

    private val roots: Set<String> = rootTypes.values.toSet()

    /** The types that a field or union refers to; a root operation type among them needs a type of its own. */
    private val referencedAsType: Set<String> = document.definitions
        .flatMap {
            when (it) {
                is ObjectTypeDefinition -> it.fields.map { field -> field.type.leaf() }
                is InterfaceTypeDefinition -> it.fields.map { field -> field.type.leaf() }
                is UnionTypeDefinition -> it.members
                else -> emptyList()
            }
        }
        .toSet()

    private val declared: List<String> = document.definitions
        .filterIsInstance<TypeDefinition>()
        .filterNot { it.extension }
        .map { it.name }
        .distinct()

    private val referenced: List<String> = document.typeReferences().distinct().filterNot { it in PRIMITIVES || it in declared }

    private val needsBuiltInId = BUILT_IN_ID in referenced

    private val unresolvable: Set<String> = referenced.filterNot { it.isValidTypeName() || it == BUILT_IN_ID }.toSet()

    private val taken = (declared + referenced).filter { it.isValidTypeName() }.toMutableSet()

    private val names: Map<String, String> = taken.associateWith { it } +
        (declared + unresolvable).filterNot { it.isValidTypeName() }.associateWith { claim(it.sanitize()) }

    private val interfaceDefinitions: Map<String, InterfaceTypeDefinition> = document.definitions
        .filterIsInstance<InterfaceTypeDefinition>()
        .filterNot { it.extension }
        .distinctBy { it.name }
        .associateBy { it.name }

    /** An interface becomes a part, so its own name is left to the union of the types implementing it, should a field return it. */
    private val interfaceParts: Map<String, String> = interfaceDefinitions.keys.associateWith { claim(partName(names.getValue(it))) }

    private val returnedInterfaces: Set<String> = document.typeReferences().toSet() intersect interfaceDefinitions.keys

    /** The interfaces each object and interface declares, its extensions included. */
    private val declaredInterfaces: Map<String, List<String>> = document.definitions
        .filterIsInstance<ImplementingTypeDefinition>()
        .groupBy({ it.name }, { it.interfaces })
        .mapValues { (_, interfaces) -> interfaces.flatten() }

    /** A root operation type becomes rpcs, which leave nothing to spread an interface into. */
    private val implementations: Map<ImplementingTypeDefinition, Lazy<Implementation>> = document.definitions
        .filterIsInstance<ImplementingTypeDefinition>()
        .filterNot { it is ObjectTypeDefinition && it.name in roots }
        .associateWith { lazy { it.implement() } }

    fun convert(): List<Definition> = document.definitions.flatMap { definition ->
        when (definition) {
            is SchemaDefinition -> definition.convert().let(::listOf)
            is DirectiveDefinition -> definition.convert().let(::listOf)
            is ScalarTypeDefinition -> definition.convert().let(::listOf)
            is ObjectTypeDefinition -> definition.convert()
            is InterfaceTypeDefinition -> definition.convert()
            is UnionTypeDefinition -> definition.convert().let(::listOf)
            is EnumTypeDefinition -> definition.convert().let(::listOf)
            is InputObjectTypeDefinition -> definition.convert().let(::listOf)
        }
    } + listOfNotNull(builtInId())

    private fun SchemaDefinition.convert(): Type = type(
        identifier = claim(if (extension) "SchemaExtension" else "Schema"),
        annotations = listOf(annotation(SCHEMA, *operationTypes.map { parameter(it.operation.keyword, it.type) }.toTypedArray())) +
            listOfNotNull(annotation(EXTEND).takeIf { extension }) +
            description.toAnnotations() +
            directives.toAnnotations(),
        entries = emptyList(),
    )

    private fun DirectiveDefinition.convert(): Type = type(
        identifier = claim("${name.sanitize()}Directive"),
        annotations = listOf(
            annotation(
                DIRECTIVE_DEFINITION,
                parameter("name", name),
                parameter("locations", locations),
                parameter("repeatable", "true").takeIf { repeatable },
            ),
        ) + description.toAnnotations(),
        entries = arguments.map { it.toField() },
    )

    private fun ScalarTypeDefinition.convert(): Refined = Refined(
        comment = null,
        annotations = commonAnnotations(),
        identifier = DefinitionIdentifier(identifier()),
        reference = Reference.Primitive(type = name.scalarType(), isNullable = false),
    )

    private fun ObjectTypeDefinition.convert(): List<Definition> = when (name) {
        in roots -> (names[name] ?: name.sanitize()).let { prefix ->
            val operation = rootTypes.entries.first { it.value == name }.key
            when {
                needsType() -> identifier().let { identifier ->
                    listOf<Definition>(type(identifier, commonAnnotations(interfaces), emptyList())) + fields.map { it.toRpc(prefix, operation, identifier, null) }
                }
                else -> fields.map { it.toRpc(prefix, operation, name, extensionBlock()) }
            }
        }
        else -> implementation().takeUnless { bringsNothing(it) }
            ?.let { type(identifier(), commonAnnotations(it.listed), it.entries) }
            .let(::listOfNotNull)
    }

    private fun InterfaceTypeDefinition.convert(): List<Definition> = implementation().takeUnless { bringsNothing(it) }
        ?.let { implementation ->
            listOfNotNull(
                Part(
                    comment = null,
                    annotations = listOf(annotation(INTERFACE)) + commonAnnotations(implementation.listed),
                    identifier = DefinitionIdentifier(identifier()),
                    shape = Shape(implementation.entries),
                ),
                standIn(),
            )
        }
        .orEmpty()

    /** A field that returns an interface returns one of the types implementing it, so the interface is also the union of those. */
    private fun InterfaceTypeDefinition.standIn(): Definition? = takeIf { interfaceDefinitions[name] === this && name in returnedInterfaces }?.let {
        val identifier = names.getValue(name)
        val annotations = listOfNotNull(annotation(NAME, name).takeIf { identifier != name })
        implementors(name)
            .takeIf { it.isNotEmpty() }
            ?.let { implementors -> Union(null, annotations, DefinitionIdentifier(identifier), implementors.map { Reference.Custom(it, isNullable = false) }.toSet()) }
            // A union needs a member, so an interface that nothing implements is an empty type.
            ?: type(identifier, annotations, emptyList())
    }

    /** The object types with a Wirespec type of their own that implement the interface, directly or through another one. */
    private fun implementors(interfaceName: String): List<String> = document.definitions
        .filterIsInstance<ObjectTypeDefinition>()
        .filter { definition ->
            implementations[definition]?.value?.let { interfaceName in it.covered || interfaceName in it.listed } ?: (interfaceName in definition.interfaces)
        }
        .map { it.name }
        .distinct()
        .filter { name -> document.definitions.any { it is ObjectTypeDefinition && !it.extension && it.name == name && (name !in roots || it.needsType()) } }
        .map { names.getValue(it) }

    /**
     * Implementing an interface is spreading its part: the fields of the interface take the place of the fields of the
     * same name, where the first of those was, so a field that GraphQL lets an implementation narrow takes the
     * interface's type, description and directives. The interfaces of the interface come along with it. An interface
     * that cannot be spread — declared elsewhere, missing fields, or with a field that another spread already brings —
     * is listed in `@GraphQLImplements`, which an extension leaves to its type.
     */
    private data class Implementation(
        val entries: List<ShapeEntry>,
        val fields: List<Field>,
        val covered: Set<String> = emptySet(),
        val listed: List<String> = emptyList(),
    )

    private fun ImplementingTypeDefinition.implementation(): Implementation = implementations.getValue(this).value

    private fun ImplementingTypeDefinition.implement(): Implementation = when {
        extension -> implement(interfaces).let { if (hasBase()) it.copy(listed = emptyList()) else it }
        else -> implement(interfaces + extensions().flatMap { it.implement(it.interfaces).listed })
    }

    // An interface that another of them implements comes after that one, which brings it along when spread.
    private fun ImplementingTypeDefinition.implement(interfaces: List<String>): Implementation = interfaces.distinct()
        .sortedBy { candidate -> interfaces.count { candidate in reachable(it) } }
        .fold(fields.map { it.toField() }.let { Implementation(it, it) }) { implementation, candidate ->
            when (candidate) {
                in implementation.covered -> implementation
                else -> spread(implementation, candidate) ?: implementation.copy(listed = implementation.listed + candidate)
            }
        }

    private fun ImplementingTypeDefinition.spread(implementation: Implementation, candidate: String): Implementation? = interfaceParts[candidate]
        // An interface that implements this one, however indirectly, is never spread into it, which would be a cycle.
        ?.takeIf { name !in reachable(candidate) }
        ?.let { part ->
            interfaceDefinitions.getValue(candidate).implementation().let { spread ->
                spread.fields.map { it.identifier.value }.toSet()
                    .takeIf { fieldNames -> implementation.entries.filterIsInstance<Field>().map { it.identifier.value }.containsAll(fieldNames) }
                    ?.let { fieldNames ->
                        Implementation(
                            entries = implementation.entries.replace(listOf(Spread(Reference.Custom(part, isNullable = false)))) { it is Field && it.identifier.value in fieldNames },
                            fields = implementation.fields.replace(spread.fields) { it.identifier.value in fieldNames },
                            covered = implementation.covered + candidate + spread.covered + spread.listed,
                            listed = implementation.listed,
                        )
                    }
            }
        }

    private fun ImplementingTypeDefinition.hasBase(): Boolean = implementations.keys.any { !it.extension && it.name == name && it::class == this::class }

    private fun ImplementingTypeDefinition.extensions(): List<ImplementingTypeDefinition> = implementations.keys.filter { it.extension && it.name == name && it::class == this::class }

    /** An extension that brings nothing but interfaces its type now implements is left out. */
    private fun ImplementingTypeDefinition.bringsNothing(implementation: Implementation): Boolean = extension && directives.isEmpty() && implementation.entries.isEmpty() && implementation.listed.isEmpty()

    private fun reachable(name: String): Set<String> = generateSequence(declaredInterfaces[name].orEmpty().toSet()) { found ->
        (found + found.flatMap { declaredInterfaces[it].orEmpty() }).takeIf { it.size > found.size }
    }.last()

    private fun UnionTypeDefinition.convert(): Definition = when {
        members.isEmpty() -> type(identifier(), listOf(annotation(UNION)) + commonAnnotations(), emptyList())
        else -> Union(
            comment = null,
            annotations = commonAnnotations() +
                listOfNotNull(annotation(UNION_MEMBERS, parameter(DEFAULT_PARAMETER, members)).takeIf { members.any { it in unresolvable } }),
            identifier = DefinitionIdentifier(identifier()),
            entries = members.map { Reference.Custom(value = it.resolve(), isNullable = false) }.toSet(),
        )
    }

    private fun EnumTypeDefinition.convert(): Definition = when {
        values.isEmpty() -> type(identifier(), listOf(annotation(ENUM)) + commonAnnotations(), emptyList())
        else -> Enum(
            comment = null,
            annotations = commonAnnotations() + values
                .filter { it.description != null || it.directives.isNotEmpty() }
                .map {
                    annotation(
                        ENUM_VALUE,
                        parameter("value", it.name),
                        it.description?.let { description -> parameter("description", description) },
                        parameter("directives", it.directives.map { directive -> directive.print() }),
                    )
                },
            identifier = DefinitionIdentifier(identifier()),
            entries = values.map { it.name }.toSet(),
        )
    }

    private fun InputObjectTypeDefinition.convert(): Type = type(
        identifier = identifier(),
        annotations = listOf(annotation(INPUT)) + commonAnnotations(),
        entries = fields.map { it.toField() },
    )

    private fun FieldDefinition.toField(): Field = Field(
        annotations = description.toAnnotations() + arguments.map { it.toArgumentAnnotation() } + directives.toAnnotations() + type.toAnnotations(),
        identifier = FieldIdentifier(name),
        reference = type.toReference(),
    )

    /**
     * A root operation type is rebuilt from the rpcs of its fields, so it only needs a type of its own for what an rpc
     * cannot carry: its description, directives and interfaces, being referenced as a type, or having no fields.
     */
    private fun ObjectTypeDefinition.needsType(): Boolean = description != null ||
        directives.isNotEmpty() ||
        interfaces.isNotEmpty() ||
        fields.isEmpty() ||
        (!extension && name in referencedAsType)

    /** Which extension of a root type this is, counting the extensions without a type of their own, to keep them apart. */
    private fun ObjectTypeDefinition.extensionBlock(): Int? = takeIf { extension }?.let {
        document.definitions
            .filterIsInstance<ObjectTypeDefinition>()
            .filter { it.extension && it.name == name && !it.needsType() }
            .indexOfFirst { it === this } + 1
    }

    /**
     * An rpc is named after its field, `addTodo` becomes `AddTodo`, and only takes the root type as prefix when that name is
     * taken. `@GraphQLName` keeps the field name when it cannot be read back from the rpc name. The root type is only
     * spelled out when it is not the default for the operation.
     */
    private fun FieldDefinition.toRpc(prefix: String, operation: Operation, rootType: String, extensionBlock: Int?): Rpc {
        val identifier = name.trimStart('_').replaceFirstChar(Char::uppercase)
            .let { claim(if (it.isValidTypeName() && it !in taken) it else prefix + it) }
        return Rpc(
            comment = null,
            annotations = listOf(
                annotation(
                    operation.annotationName,
                    rootType.takeIf { it != operation.defaultTypeName }?.let { parameter(DEFAULT_PARAMETER, it) },
                    extensionBlock?.let { parameter("extend", it.toString()) },
                ),
            ) +
                listOfNotNull(annotation(NAME, name).takeIf { identifier.replaceFirstChar(Char::lowercase) != name }) +
                description.toAnnotations() +
                directives.toAnnotations() +
                type.toAnnotations(),
            identifier = DefinitionIdentifier(identifier),
            shape = Shape(arguments.map { it.toParameter() }),
            result = type.toReference(),
            error = null,
        )
    }

    /** A default becomes a Wirespec default when it reads back as the same GraphQL literal, otherwise it stays in an annotation. */
    private fun InputValueDefinition.toField(): Field = type.toReference().let { reference ->
        toField(reference, defaultValue?.toDefaultValue(reference)?.takeIf { it !is DefaultValue.EnumValue || it.isEntryOf(type.leaf()) })
    }

    /**
     * Wirespec checks an enum default against the enum's own entries, so it only holds one of a GraphQL enum declared in
     * this document: not of an enum declared elsewhere, nor one that only an `extend enum` adds.
     */
    private fun DefaultValue.EnumValue.isEntryOf(enum: String): Boolean = document.definitions
        .filterIsInstance<EnumTypeDefinition>()
        .any { !it.extension && it.name == enum && it.values.any { entry -> entry.name == value } }

    /** Wirespec only allows defaults on the fields of a type, so an rpc parameter keeps its default in an annotation. */
    private fun InputValueDefinition.toParameter(): Field = toField(type.toReference(), null)

    private fun InputValueDefinition.toField(reference: Reference, native: DefaultValue?): Field = Field(
        annotations = description.toAnnotations() +
            listOfNotNull(defaultValue?.takeIf { native == null }?.let { annotation(DEFAULT, it.print()) }) +
            directives.toAnnotations() +
            type.toAnnotations(),
        identifier = FieldIdentifier(name),
        reference = reference,
        defaultValue = native,
    )

    private fun InputValueDefinition.toArgumentAnnotation(): Annotation = annotation(
        ARGUMENT,
        parameter("name", name),
        parameter("type", type.print()),
        defaultValue?.let { parameter("defaultValue", it.print()) },
        description?.let { parameter("description", it) },
        parameter("directives", directives.map { it.print() }),
    )

    private fun TypeDefinition.identifier(): String = when {
        extension -> claim("${wirespecName()}Extension")
        else -> wirespecName()
    }

    /** Only an extension of a type declared elsewhere has no name yet. */
    private fun TypeDefinition.wirespecName(): String = (names[name] ?: name.sanitize()).let { named ->
        when (this) {
            is InterfaceTypeDefinition -> interfaceParts[name] ?: partName(named)
            is ScalarTypeDefinition, is ObjectTypeDefinition, is UnionTypeDefinition, is EnumTypeDefinition, is InputObjectTypeDefinition -> named
        }
    }

    /** The name the Wirespec name reads back as, which an interface part spells with a suffix. */
    private fun TypeDefinition.readBack(): String? = when (this) {
        is InterfaceTypeDefinition -> interfaceParts[name]?.let(::interfaceName)
        is ScalarTypeDefinition, is ObjectTypeDefinition, is UnionTypeDefinition, is EnumTypeDefinition, is InputObjectTypeDefinition -> names[name]
    }

    private fun TypeDefinition.commonAnnotations(interfaces: List<String> = emptyList()): List<Annotation> = listOfNotNull(
        annotation(EXTEND, name).takeIf { extension },
        annotation(NAME, name).takeIf { !extension && readBack() != name },
        annotation(IMPLEMENTS, parameter(DEFAULT_PARAMETER, interfaces)).takeIf { interfaces.isNotEmpty() },
    ) + description().toAnnotations() + directives().toAnnotations()

    private fun TypeDefinition.description(): String? = when (this) {
        is ScalarTypeDefinition -> description
        is ObjectTypeDefinition -> description
        is InterfaceTypeDefinition -> description
        is UnionTypeDefinition -> description
        is EnumTypeDefinition -> description
        is InputObjectTypeDefinition -> description
    }

    private fun TypeDefinition.directives(): List<GraphQLDirective> = when (this) {
        is ScalarTypeDefinition -> directives
        is ObjectTypeDefinition -> directives
        is InterfaceTypeDefinition -> directives
        is UnionTypeDefinition -> directives
        is EnumTypeDefinition -> directives
        is InputObjectTypeDefinition -> directives
    }

    private fun String?.toAnnotations(): List<Annotation> = listOfNotNull(this?.let { annotation(DESCRIPTION, it) })

    private fun List<GraphQLDirective>.toAnnotations(): List<Annotation> = map { annotation(DIRECTIVE, it.print()) }

    /** The exact GraphQL type, for the rare reference to an undeclared type whose name Wirespec cannot spell. */
    private fun TypeRef.toAnnotations(): List<Annotation> = listOfNotNull(annotation(TYPE, print()).takeIf { name() in unresolvable })

    private fun TypeRef.name(): String = when (this) {
        is TypeRef.Named -> name
        is TypeRef.ListOf -> type.name()
        is TypeRef.NonNull -> type.name()
    }

    private fun TypeRef.toReference(isNullable: Boolean = true): Reference = when (this) {
        is TypeRef.NonNull -> type.toReference(isNullable = false)
        is TypeRef.ListOf -> Reference.Iterable(reference = type.toReference(), isNullable = isNullable)
        is TypeRef.Named -> when (name) {
            "Int" -> Reference.Primitive(type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), isNullable = isNullable)
            "Float" -> Reference.Primitive(type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null), isNullable = isNullable)
            "String" -> Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = isNullable)
            "Boolean" -> Reference.Primitive(type = Reference.Primitive.Type.Boolean, isNullable = isNullable)
            else -> Reference.Custom(value = name.resolve(), isNullable = isNullable)
        }
    }

    private fun String.resolve(): String = names[this] ?: this

    private fun String.scalarType(): Reference.Primitive.Type = when (this) {
        "Int" -> Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null)
        "Float" -> Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null)
        "Boolean" -> Reference.Primitive.Type.Boolean
        else -> Reference.Primitive.Type.String(null)
    }

    private fun builtInId(): Refined? = Refined(
        comment = null,
        annotations = listOf(annotation(BUILT_IN)),
        identifier = DefinitionIdentifier(BUILT_IN_ID),
        reference = Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = false),
    ).takeIf { needsBuiltInId }

    private fun type(identifier: String, annotations: List<Annotation>, entries: List<ShapeEntry>): Type = Type(
        comment = null,
        annotations = annotations,
        identifier = DefinitionIdentifier(identifier),
        shape = Shape(entries),
        extends = emptyList(),
    )

    private fun <T> List<T>.replace(replacement: List<T>, replaced: (T) -> Boolean): List<T> = indexOfFirst(replaced).coerceAtLeast(0)
        .let { at -> take(at) + replacement + drop(at).filterNot(replaced) }

    private fun claim(candidate: String): String = generateSequence(2) { it + 1 }
        .map { "$candidate$it" }
        .let { sequenceOf(candidate) + it }
        .first { it !in taken }
        .also { taken += it }

    private companion object {
        const val BUILT_IN_ID = "ID"
        val PRIMITIVES = setOf("Int", "Float", "String", "Boolean")
        val TYPE_NAME = Regex("[A-Z][a-zA-Z0-9_]*")
        val RESERVED = setOf(
            "Any", "Boolean", "Bytes", "Integer", "Integer32", "Number", "Number32", "String", "Unit",
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH", "TRACE",
        )

        fun String.isValidTypeName() = TYPE_NAME.matches(this) && this !in RESERVED

        fun String.sanitize(): String = trimStart('_')
            .replaceFirstChar(Char::uppercase)
            .let { if (it.firstOrNull()?.isLetter() == true && it !in RESERVED) it else "GraphQL$it" }

        fun Document.typeReferences(): List<String> = definitions.flatMap { definition ->
            when (definition) {
                is SchemaDefinition -> definition.operationTypes.map { it.type }
                is DirectiveDefinition -> definition.arguments.map { it.type.leaf() }
                is ScalarTypeDefinition -> emptyList()
                is ObjectTypeDefinition -> definition.fields.flatMap { it.references() }
                is InterfaceTypeDefinition -> definition.fields.flatMap { it.references() }
                is UnionTypeDefinition -> definition.members
                is EnumTypeDefinition -> emptyList()
                is InputObjectTypeDefinition -> definition.fields.map { it.type.leaf() }
            }
        }

        fun FieldDefinition.references(): List<String> = listOf(type.leaf()) + arguments.map { it.type.leaf() }

        val NUMBER = Regex("-?[0-9]+\\.[0-9]+")

        fun GraphQLModel.Value.toDefaultValue(reference: Reference): DefaultValue? = when (this) {
            is GraphQLModel.Value.StringValue -> DefaultValue.StringValue(value)
            is GraphQLModel.Value.IntValue -> DefaultValue.IntegerValue(raw)
            is GraphQLModel.Value.FloatValue -> DefaultValue.NumberValue(raw).takeIf { NUMBER.matches(raw) }
            is GraphQLModel.Value.BooleanValue -> DefaultValue.BooleanValue(value)
            is GraphQLModel.Value.NullValue -> DefaultValue.NullValue
            is GraphQLModel.Value.EnumValue -> DefaultValue.EnumValue(name)
            is GraphQLModel.Value.ListValue, is GraphQLModel.Value.ObjectValue -> null
        }?.coerceTo(reference)?.takeIf { it.toGraphQLValue() == this }

        fun TypeRef.leaf(): String = when (this) {
            is TypeRef.Named -> name
            is TypeRef.ListOf -> type.leaf()
            is TypeRef.NonNull -> type.leaf()
        }
    }
}

internal fun DefaultValue.toGraphQLValue(): GraphQLModel.Value = when (this) {
    is DefaultValue.StringValue -> GraphQLModel.Value.StringValue(value)
    is DefaultValue.IntegerValue -> GraphQLModel.Value.IntValue(value)
    is DefaultValue.NumberValue -> GraphQLModel.Value.FloatValue(value)
    is DefaultValue.BooleanValue -> GraphQLModel.Value.BooleanValue(value)
    is DefaultValue.EnumValue -> GraphQLModel.Value.EnumValue(value)
    DefaultValue.NullValue -> GraphQLModel.Value.NullValue
}
