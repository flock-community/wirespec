package community.flock.wirespec.converter.graphql

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
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
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.coerceTo
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.BEFORE_PARAMETER
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
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.PATH_PARAMETER
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.SCHEMA
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.TYPE_PARAMETER
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.UNION_MEMBERS
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotation
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.annotationName
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.fieldName
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.parameter
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
 * Objects and input objects become types, scalars become refined types, and enums and unions map onto their Wirespec
 * counterparts. A field is a function when it takes arguments, and a function is an `rpc`: every field of a root
 * operation type (`Query`, `Mutation`, `Subscription`), every field of a type that only nests operations in one, and
 * every other field with arguments. An annotation of the rpc says which type it is a field of. A type nested in a root
 * operation type is a module of its own, whose `@file:` annotation names the fields that lead to the type.
 * An interface becomes the union of the types implementing it, which spread the part holding its fields. Whatever
 * Wirespec cannot express natively —
 * descriptions, directives, default values, extensions — is kept in annotations, so the schema can
 * be rebuilt from the Wirespec definitions. The schema and directive definitions belong to the schema as a whole, so
 * they are `@spec:` annotations.
 */
public object GraphQLParser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = GraphQLDocumentParser(moduleContent.content)
        .parseDocument()
        .let(::GraphQLConverter)
        .let { converter ->
            (listOfNotNull(converter.definitions().toNonEmptyListOrNull()?.let { Module(moduleContent.fileUri, it, annotations = emptyList()) }) + converter.nestedModules())
                .toNonEmptyListOrNull()
                ?.let { modules -> AST(modules, annotations = converter.specAnnotations().map { SpecAnnotation(modules.head.fileUri, it) }) }
                ?: error("Cannot convert GraphQL document ${moduleContent.fileUri.value}: it defines no types, and Wirespec needs a definition besides the @spec: annotations of its schema and directives")
        }
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

    private val references: List<String> = document.typeReferences()

    private val referenced: List<String> = references.distinct().filterNot { it in PRIMITIVES || it in declared }

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

    /** The interfaces each object and interface declares, its extensions included. */
    private val declaredInterfaces: Map<String, List<String>> = document.definitions
        .filterIsInstance<ImplementingTypeDefinition>()
        .groupBy({ it.name }, { it.interfaces })
        .mapValues { (_, interfaces) -> interfaces.flatten() }

    /** An interface that anything implements keeps its fields in a part, for the implementations to spread. */
    private val interfaceParts: Map<String, String> = interfaceDefinitions.keys
        .filter { name -> declaredInterfaces.values.any { name in it } }
        .associateWith { claim("${names.getValue(it)}Interface") }

    /**
     * The object types that can be nested in the type that refers to them, by the name of that type: one field refers
     * to them and nothing else does, so one way leads there. That field takes no arguments and returns the one object.
     */
    private val nestedIn: Map<String, String> = document.definitions
        .filterIsInstance<ObjectTypeDefinition>()
        .flatMap { parent -> parent.fields.filter { it.isNesting() }.map { it.type.leaf() to parent.name } }
        .filter { (type, _) -> type.canNest() }
        .toMap()

    /**
     * The types that only nest operations in a root operation type, as `Query.notifications: NotificationsQuery` does.
     * Their fields are operations too, so they become the rpcs of a module that names the fields that lead to the type,
     * and the type itself needs no Wirespec definition. A type that only holds data, without a field that takes
     * arguments, is no such type.
     */
    private val nested: Set<String> = nestedIn.keys.filter { it.isNestedInRoot() && it.hasOperations() }.toSet()

    /** A root operation type becomes rpcs, which leave nothing to spread an interface into. */
    private val implementations: Map<ImplementingTypeDefinition, Lazy<Implementation>> = document.definitions
        .filterIsInstance<ImplementingTypeDefinition>()
        .filterNot { it is ObjectTypeDefinition && it.name in roots }
        .associateWith { lazy { it.implement() } }

    /** The schema and directive definitions, which no Wirespec definition stands for. */
    fun specAnnotations(): List<Annotation> = document.definitions.mapNotNull { definition ->
        when (definition) {
            is SchemaDefinition -> definition.convert()
            is DirectiveDefinition -> definition.convert()
            is TypeDefinition -> null
        }
    }

    fun definitions(): List<Definition> = document.definitions.flatMap { definition ->
        when (definition) {
            is SchemaDefinition, is DirectiveDefinition -> emptyList()
            is ScalarTypeDefinition -> definition.convert().let(::listOf)
            is ObjectTypeDefinition -> definition.convert()
            is InterfaceTypeDefinition -> definition.convert()
            is UnionTypeDefinition -> definition.convert().let(::listOf)
            is EnumTypeDefinition -> definition.convert().let(::listOf)
            is InputObjectTypeDefinition -> definition.convert().let(::listOf)
        }
    } + listOfNotNull(builtInId())

    private fun SchemaDefinition.convert(): Annotation = annotation(
        SCHEMA,
        *operationTypes.map { parameter(it.operation.keyword, it.type) }.toTypedArray(),
        parameter("extend", "true").takeIf { extension },
        description?.let { parameter("description", it) },
        parameter("directives", directives.map { it.print() }),
    )

    private fun DirectiveDefinition.convert(): Annotation = annotation(
        DIRECTIVE_DEFINITION,
        parameter("name", name),
        parameter("locations", locations),
        parameter("repeatable", "true").takeIf { repeatable },
        description?.let { parameter("description", it) },
        parameter("arguments", arguments.associate { it.name to it.argumentParameters() }),
    )

    private fun ScalarTypeDefinition.convert(): Refined = Refined(
        comment = null,
        annotations = commonAnnotations(),
        identifier = DefinitionIdentifier(identifier()),
        reference = Reference.Primitive(type = name.scalarType(), isNullable = false),
    )

    private fun ObjectTypeDefinition.convert(): List<Definition> = when (name) {
        in roots -> rootDefinitions.getValue(this).let { root ->
            listOfNotNull(type(root.type, commonAnnotations(interfaces), emptyList()).takeIf { needsType() }) + operations(root.annotation(emptyList()))
        }
        // A module of its own.
        in nested -> emptyList()
        else -> implementation().takeUnless { bringsNothing(it) }
            ?.let { identifier().let { identifier -> listOf(type(identifier, commonAnnotations(it.listed), it.entries)) + functions(identifier) } }
            .orEmpty()
    }

    /** A root operation type as its operations name it: the root type [type] of an [operation], and its `extend` block [extend]. */
    private data class Root(val operation: Operation, val type: String, val extend: Int?)

    private val rootDefinitions: Map<ObjectTypeDefinition, Root> by lazy {
        document.definitions
            .filterIsInstance<ObjectTypeDefinition>()
            .filter { it.name in roots }
            .associateWith { definition ->
                rootTypes.entries.first { it.value == definition.name }.key.let { operation ->
                    if (definition.needsType()) Root(operation, definition.identifier(), null) else Root(operation, definition.name, definition.extensionBlock())
                }
            }
    }

    /** The root type is only spelled out when it is not the default for the operation. */
    private fun Root.annotation(path: List<Annotation.Parameter>): Annotation = annotation(
        operation.annotationName,
        type.takeIf { it != operation.defaultTypeName }?.let { parameter(DEFAULT_PARAMETER, it) },
        extend?.let { parameter("extend", it.toString()) },
        path.takeIf { it.isNotEmpty() }?.let { Annotation.Parameter(PATH_PARAMETER, Annotation.Value.Dict(it)) },
    )

    /** The fields of a type as operations. A field that leads to a nested type is written with the module of that type. */
    private fun ObjectTypeDefinition.operations(annotation: Annotation): List<Rpc> = (names[name] ?: name.sanitize()).let { prefix ->
        fields.filterNot { it.leadsToNested() }.map { it.toOperation(prefix, annotation) }
    }

    /**
     * Every nested type as a module of its own. Its `@file:` annotation says where its operations are: the root type,
     * and the fields that lead from there to the type.
     */
    fun nestedModules(): List<Module> = document.definitions
        .filterIsInstance<ObjectTypeDefinition>()
        .filter { it.name in nested }
        .mapNotNull { definition ->
            route(definition.name).let { route ->
                rootDefinitions.getValue(route.first().first).let { root ->
                    (listOfNotNull(definition.holder()) + definition.operations(annotation(root.operation.annotationName)))
                        .toNonEmptyListOrNull()
                        ?.let { Module(FileUri("${definition.name}.ws"), it, annotations = listOf(root.annotation(route.map { (parent, field) -> field.toPathParameter(parent) }))) }
                }
            }
        }

    /** The fields that lead from a root type to a nested type, each with the type it is a field of. */
    private fun route(type: String): List<Pair<ObjectTypeDefinition, FieldDefinition>> = generateSequence(leadTo(type)) { (parent, _) -> leadTo(parent.name) }.toList().reversed()

    private fun leadTo(type: String): Pair<ObjectTypeDefinition, FieldDefinition>? = nestedIn[type]?.let { parent ->
        document.definitions
            .filterIsInstance<ObjectTypeDefinition>()
            .filter { it.name == parent }
            .firstNotNullOf { definition -> definition.fields.find { it.isNesting() && it.type.leaf() == type }?.let { definition to it } }
    }

    private fun FieldDefinition.leadsToNested(): Boolean = isNesting() && type.leaf() in nested

    /** A nested type is rebuilt from its rpcs, so it only needs an empty type of its own to hold its description and directives. */
    private fun ObjectTypeDefinition.holder(): Type? = takeIf { description != null || directives.isNotEmpty() }
        ?.let { type(name, commonAnnotations(), emptyList()) }

    /**
     * A field of the path: its type, or what describes it when it has more than a type. `before` names the field that
     * follows it in [parent], so the field comes back in its place.
     */
    private fun FieldDefinition.toPathParameter(parent: ObjectTypeDefinition): Annotation.Parameter = parent.fields.dropWhile { it != this }.drop(1).firstOrNull()?.name.let { before ->
        when {
            description == null && directives.isEmpty() && before == null -> parameter(name, type.print())
            else -> Annotation.Parameter(
                name,
                Annotation.Value.Dict(
                    listOfNotNull(
                        parameter(TYPE_PARAMETER, type.print()),
                        description?.let { parameter("description", it) },
                        parameter("directives", directives.map { it.print() }),
                        before?.let { parameter(BEFORE_PARAMETER, it) },
                    ),
                ),
            )
        }
    }

    private fun FieldDefinition.isNesting(): Boolean = arguments.isEmpty() && (type is TypeRef.Named || (type is TypeRef.NonNull && type.type is TypeRef.Named))

    private fun String.canNest(): Boolean = this !in roots &&
        names[this] == this &&
        references.count { it == this } == 1 &&
        document.definitions.filterIsInstance<TypeDefinition>().singleOrNull { it.name == this }.let { it is ObjectTypeDefinition && it.interfaces.isEmpty() && !it.extension }

    private fun String.isNestedInRoot(): Boolean = generateSequence(nestedIn[this]) { nestedIn[it] }.take(nestedIn.size).any { it in roots }

    private fun String.hasOperations(): Boolean = document.definitions
        .filterIsInstance<ObjectTypeDefinition>()
        .filter { it.name == this }
        .flatMap { it.fields }
        .any { field -> field.arguments.isNotEmpty() || (field.isNesting() && nestedIn[field.type.leaf()] == this && field.type.leaf().hasOperations()) }

    /**
     * A field with arguments is an rpc of its type, named after both. `before` names the field that follows it, so
     * the field comes back in its place.
     */
    private fun ImplementingTypeDefinition.functions(identifier: String): List<Rpc> = (names[name] ?: name.sanitize()).let { prefix ->
        fields.mapIndexedNotNull { index, field ->
            field.takeIf { it.arguments.isNotEmpty() }?.let {
                claim(prefix + field.name.capitalized()).let { rpc ->
                    field.toRpc(
                        identifier = rpc,
                        annotation = annotation(
                            FIELD,
                            parameter(DEFAULT_PARAMETER, identifier),
                            fields.drop(index + 1).firstOrNull { it.arguments.isEmpty() }?.let { parameter(BEFORE_PARAMETER, it.name) },
                        ),
                        named = fieldName(rpc, name),
                    )
                }
            }
        }
    }

    /**
     * An interface that anything implements is the union of the types implementing it, `@GraphQLInterface` naming the
     * part with its fields; any other interface, an extension too, is a type with fields of its own.
     */
    private fun InterfaceTypeDefinition.convert(): List<Definition> = implementation().takeUnless { bringsNothing(it) }
        ?.let { implementation ->
            identifier().let { identifier ->
                interfaceParts[name]?.takeIf { interfaceDefinitions[name] === this }
                    ?.let { part ->
                        listOf(
                            Part(comment = null, identifier = DefinitionIdentifier(part), shape = Shape(implementation.entries)),
                            implementedBy(identifier, listOf(annotation(INTERFACE, part)) + commonAnnotations(implementation.listed)),
                        )
                    }
                    .let { it ?: listOf(type(identifier, listOf(annotation(INTERFACE)) + commonAnnotations(implementation.listed), implementation.entries)) } +
                    functions(identifier)
            }
        }
        .orEmpty()

    /** A union needs a member, so an interface that only interfaces implement is an empty type. */
    private fun InterfaceTypeDefinition.implementedBy(identifier: String, annotations: List<Annotation>): Definition = implementors(name)
        .takeIf { it.isNotEmpty() }
        ?.let { implementors -> Union(null, annotations, DefinitionIdentifier(identifier), implementors.map { Reference.Custom(it, isNullable = false) }.toSet()) }
        ?: type(identifier, annotations, emptyList())

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
        .fold(fields.filter { it.arguments.isEmpty() }.map { it.toField() }.let { Implementation(it, it) }) { implementation, candidate ->
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
    private fun ImplementingTypeDefinition.bringsNothing(implementation: Implementation): Boolean = extension && directives.isEmpty() && fields.isEmpty() && implementation.listed.isEmpty()

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
        annotations = description.toAnnotations() + directives.toAnnotations() + type.toAnnotations(),
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
     * An operation is named after its field, `addTodo` becomes `AddTodo`, and only takes the type it is a field of as
     * prefix when that name is taken.
     */
    private fun FieldDefinition.toOperation(prefix: String, annotation: Annotation): Rpc = name.capitalized()
        .let { claim(if (it.isValidTypeName() && it !in taken) it else prefix + it) }
        .let { rpc -> toRpc(identifier = rpc, annotation = annotation, named = fieldName(rpc, type = null)) }

    /** `@GraphQLName` keeps the field name when it is not the one the rpc is [named] after. */
    private fun FieldDefinition.toRpc(identifier: String, annotation: Annotation, named: String): Rpc = Rpc(
        comment = null,
        annotations = listOf(annotation) +
            listOfNotNull(annotation(NAME, name).takeIf { named != name }) +
            description.toAnnotations() +
            directives.toAnnotations() +
            type.toAnnotations(),
        identifier = DefinitionIdentifier(identifier),
        shape = Shape(arguments.map { it.toField() }),
        result = type.toReference(),
        error = null,
    )

    private fun String.capitalized(): String = trimStart('_').replaceFirstChar(Char::uppercase)

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

    private fun InputValueDefinition.toField(reference: Reference, native: DefaultValue?): Field = Field(
        annotations = description.toAnnotations() +
            listOfNotNull(defaultValue?.takeIf { native == null }?.let { annotation(DEFAULT, it.print()) }) +
            directives.toAnnotations() +
            type.toAnnotations(),
        identifier = FieldIdentifier(name),
        reference = reference,
        defaultValue = native,
    )

    /** An argument as `@GraphQLDirectiveDefinition` holds it, but for its name. */
    private fun InputValueDefinition.argumentParameters(): List<Annotation.Parameter> = listOfNotNull(
        parameter(TYPE_PARAMETER, type.print()),
        defaultValue?.let { parameter("defaultValue", it.print()) },
        description?.let { parameter("description", it) },
        parameter("directives", directives.map { it.print() }),
    )

    private fun TypeDefinition.identifier(): String = when {
        extension -> claim("${names[name] ?: name.sanitize()}Extension")
        else -> names.getValue(name)
    }

    private fun TypeDefinition.commonAnnotations(interfaces: List<String> = emptyList()): List<Annotation> = listOfNotNull(
        annotation(EXTEND, name).takeIf { extension },
        annotation(NAME, name).takeIf { !extension && names[name] != name },
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
                // The arguments of a directive keep their GraphQL types in its annotation.
                is DirectiveDefinition -> emptyList()
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
