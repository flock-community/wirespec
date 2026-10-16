package community.flock.wirespec.converter.asyncapi

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecType
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.Action
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.CHANNEL
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.COMPONENTS
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DOCUMENT
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.INLINE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.MESSAGE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.NAME
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.annotation
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.parameter
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toAnnotationValue
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toDescription
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toParameters
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toStringValue
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.converter.common.toDefaultValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type as PrimitiveType

/**
 * Converts an AsyncAPI document, version 2.x or 3.x written in JSON, into Wirespec, without loss.
 *
 * Every channel becomes a `channel` that carries the payload of its messages, and every schema becomes a type, enum,
 * union or refined type. Whatever Wirespec has no syntax for is kept in annotations: the document, its components,
 * the channels, messages and operations, the names that are no Wirespec names and the JSON Schema keywords Wirespec
 * does not write itself. [AsyncAPIEmitter] rebuilds the document from the Wirespec definitions and their annotations.
 */
public object AsyncAPIParser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = Json.parseToJsonElement(moduleContent.content)
        .let { it as? JsonObject ?: error("Invalid AsyncAPI document; expected a JSON object") }
        .let { AsyncAPIConverter(it).convert() }
        .let { AST(nonEmptyListOf(Module(moduleContent.fileUri, it))) }
}

/** A converted schema: the reference to it and the definitions it introduced. */
private data class Converted(val reference: Reference, val definitions: List<Definition> = emptyList())

/** A message as an annotation, the type of its payload and the definitions its schemas introduced. */
private data class ConvertedMessage(val annotation: Annotation, val payload: Reference, val definitions: List<Definition>)

/** A node of the document and the JSON pointer it was found at. */
private data class Located(val pointer: String, val node: JsonObject)

/** A message of an AsyncAPI 2 operation, known by the component it refers to or else by where it is. */
private data class OperationMessage(val id: String, val pointer: String, val element: JsonElement, val name: String)

private class AsyncAPIConverter(private val document: JsonObject) {

    private val majorVersion: Int = document.string("asyncapi")
        ?.let { version -> version.substringBefore('.').toIntOrNull()?.takeIf { it in SUPPORTED_VERSIONS } ?: error("Unsupported AsyncAPI version $version; only 2.x and 3.x can be converted") }
        ?: error("Invalid AsyncAPI document; missing 'asyncapi' property")

    private val taken = mutableSetOf<String>()

    private val components: JsonObject? = document["components"] as? JsonObject

    private val schemas: Map<String, JsonElement> = components?.obj("schemas").orEmpty()

    /** The defined component schemas convert to a definition and claim their names first, so they keep them. */
    private val schemaNames: Map<String, String> = schemas
        .filterValues { (it as? JsonObject)?.ownsDefinition(component = true) == true }
        .keys
        .associateWith { claim(it.toIdentifier()) }

    private val componentKeys: Map<String, String> = schemaNames.entries.associate { (key, name) -> name to key }

    private val holderName = claim(HOLDER)

    /** The reference each schema converted to, by JSON pointer, so a schema found again is not converted again. */
    private val converted = mutableMapOf<String, Reference>()

    private val inProgress = mutableSetOf<String>()

    private val inlineDefinitions = mutableMapOf<String, Model>()

    private val writer = JsonSchemaWriter(inlineDefinitions::get) { componentKeys[it] ?: it }

    private val componentMessages = mutableMapOf<String, ConvertedMessage>()

    private val resolvingMessages = mutableSetOf<String>()

    private val channelKeys: Set<String> = document.obj("channels").orEmpty().keys

    fun convert(): NonEmptyList<Definition> {
        val operations = document.obj("operations").orEmpty()
            .takeIf { majorVersion == 3 }
            .orEmpty()
            .map { (key, operation) -> key to (operation as? JsonObject ?: error("Expected an operation object at #/operations/$key")) }
        val (channelOperations, documentOperations) = operations.partition { (_, operation) -> operation.channelKey() != null }
        val channels = document.obj("channels").orEmpty().flatMap { (key, channel) ->
            when (majorVersion) {
                2 -> channelV2(key, channel)
                else -> channelV3(key, channel, channelOperations.filter { (_, operation) -> operation.channelKey() == key })
            }
        }
        val messages = components?.obj("messages").orEmpty().keys.mapNotNull(::componentMessage)
        val definitions = channels + messages.flatMap { it.definitions } + schemaNames.keys.flatMap { component(it).definitions }
        return nonEmptyListOf<Definition>(holder(messages, documentOperations)) + definitions
    }

    /** The definition that holds what belongs to the document as a whole. */
    private fun holder(messages: List<ConvertedMessage>, operations: List<Pair<String, JsonObject>>): Type = Type(
        comment = null,
        annotations = listOf(annotation(DOCUMENT, parameters = document.documentFields().toParameters())) +
            listOfNotNull(components?.componentFields()?.let { annotation(COMPONENTS, parameters = it.toParameters()) }) +
            messages.map { it.annotation } +
            operations.map { (key, operation) -> operation.toAnnotation(key, channelPointer = null) },
        identifier = DefinitionIdentifier(holderName),
        shape = Shape(emptyList()),
        extends = emptyList(),
    )

    /** The fields of the document besides the channels, operations and components that Wirespec holds itself. */
    private fun JsonObject.documentFields(): JsonObject = JsonObject(
        filterNot { (key, value) ->
            when (key) {
                "channels" -> value.isNonEmptyObject()
                "operations" -> majorVersion == 3 && value.isNonEmptyObject()
                "components" -> value is JsonObject
                else -> false
            }
        },
    )

    /** The fields of the components besides the schemas and messages that Wirespec holds itself. */
    private fun JsonObject.componentFields(): JsonObject = JsonObject(
        mapNotNull { (key, value) ->
            when {
                key == "schemas" && value is JsonObject -> JsonObject(value.filterKeys { it !in schemaNames })
                    .takeIf { it.isNotEmpty() || value.isEmpty() }
                    ?.let { key to it }
                key == "messages" && value.isNonEmptyObject() -> null
                else -> key to value
            }
        }.toMap(),
    )

    private fun channelV3(key: String, element: JsonElement, operations: List<Pair<String, JsonObject>>): List<Definition> {
        val pointer = "#/channels/${key.escapePointer()}"
        val channel = element as? JsonObject ?: error("Expected a channel object at $pointer")
        val identifier = claimChannel(key)
        return when (val ref = channel.ref()) {
            null -> channel.obj("messages").orEmpty()
                .map { (messageKey, message) -> message(messageKey, "$pointer/messages/${messageKey.escapePointer()}", message) }
                .let { messages ->
                    channel(
                        identifier = identifier,
                        annotations = channel.description().toDescription() +
                            nameAnnotation(key, identifier.firstToLower()) +
                            annotation(CHANNEL, parameters = JsonObject(channel.without("description" to { it.isString() }, "messages" to { it.isNonEmptyObject() })).toParameters()) +
                            messages.map { it.annotation } +
                            operations.map { (operationKey, operation) -> operation.toAnnotation(operationKey, pointer) },
                        payloads = messages.map { Converted(it.payload, it.definitions) },
                    )
                }
            // The channel is declared elsewhere, so only the payloads of its messages are needed.
            else -> channel(
                identifier = identifier,
                annotations = nameAnnotation(key, identifier.firstToLower()) +
                    annotation(CHANNEL, parameters = channel.toParameters()) +
                    operations.map { (operationKey, operation) -> operation.toAnnotation(operationKey, pointer) },
                payloads = (lookup(ref) as? JsonObject)?.obj("messages").orEmpty().map { (messageKey, message) ->
                    message(messageKey, "$ref/messages/${messageKey.escapePointer()}", message).let { Converted(it.payload, it.definitions) }
                },
            )
        }
    }

    /** AsyncAPI 2 describes operations from the side of the client: a client subscribes to what the application sends. */
    private fun channelV2(address: String, element: JsonElement): List<Definition> {
        val pointer = "#/channels/${address.escapePointer()}"
        val channel = element as? JsonObject ?: error("Expected a channel object at $pointer")
        val identifier = claimChannel(address)
        val operations = Action.entries
            .mapNotNull { action -> channel.obj(action.v2Keyword)?.let { action to it } }
            .takeIf { channel.ref() == null }
            .orEmpty()
        // The messages of each operation, and whether the operation lists them in a `oneOf`.
        val operationMessages = operations.map { (action, operation) ->
            val messagePointer = "$pointer/${action.v2Keyword}/message"
            when (val message = operation["message"]) {
                null -> emptyList<OperationMessage>() to false
                else -> (message as? JsonObject)?.takeIf { it.keys == setOf("oneOf") }?.get("oneOf")?.let { it as? JsonArray }
                    ?.mapIndexed { index, alternative -> operationMessage("$messagePointer/oneOf/$index", alternative, action) }
                    ?.let { it to true }
                    ?: (listOf(operationMessage(messagePointer, message, action)) to false)
            }
        }
        // A message that both operations refer to is one message of the channel.
        val keys = operationMessages.flatMap { it.first }
            .distinctBy { it.id }
            .fold(emptyList<Pair<String, OperationMessage>>()) { keys, message -> keys + (message.name.unique(keys.map { it.first }.toSet()) to message) }
        val keyOf = keys.associate { (key, message) -> message.id to key }
        val messages = keys.map { (key, message) -> message(key, message.pointer, message.element) }
        return channel(
            identifier = identifier,
            annotations = channel.takeIf { it.ref() == null }?.description().toDescription() +
                nameAnnotation(address, identifier.firstToLower()) +
                annotation(
                    CHANNEL,
                    parameters = when (channel.ref()) {
                        null -> channel.without("description" to { it.isString() }) - operations.map { it.first.v2Keyword }.toSet()
                        else -> channel
                    }.let(::JsonObject).toParameters(),
                ) +
                messages.map { it.annotation } +
                operations.zip(operationMessages).map { (operation, messagesOfOperation) ->
                    val (action, fields) = operation
                    val (operationKeys, oneOf) = messagesOfOperation.let { (messages, oneOf) -> messages.map { keyOf.getValue(it.id) } to oneOf }
                    annotation(
                        action.annotationName,
                        (fields["operationId"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
                        fields.entries.filterNot { (field, value) -> field == "operationId" && value.isString() }.map { (field, value) ->
                            parameter(
                                field,
                                when {
                                    field != "message" -> value.toAnnotationValue()
                                    oneOf -> Annotation.Value.Array(operationKeys.map { it.toStringValue() })
                                    else -> operationKeys.single().toStringValue()
                                },
                            )
                        },
                    )
                },
            payloads = messages.map { Converted(it.payload, it.definitions) },
        )
    }

    private fun operationMessage(pointer: String, element: JsonElement, action: Action): OperationMessage {
        val message = element as? JsonObject
        val ref = message?.ref()
        return OperationMessage(
            id = ref ?: pointer,
            pointer = pointer,
            element = element,
            name = ref?.componentKey("messages") ?: message?.string("name") ?: message?.string("messageId") ?: "${action.keyword}Message",
        )
    }

    /** A channel carries the payload of its message, or a union of the payloads of its messages. */
    private fun channel(identifier: String, annotations: List<Annotation>, payloads: List<Converted>): List<Definition> {
        val references = payloads.map { it.reference }.distinct()
        val union = references.takeIf { it.size > 1 && it.all { reference -> reference is Reference.Custom } }?.let { members ->
            Union(
                comment = null,
                annotations = listOf(annotation(INLINE)),
                identifier = DefinitionIdentifier(claim("${identifier}Message")),
                entries = members.map { it.copy(isNullable = false) }.toSet(),
            )
        }
        val reference = when {
            union != null -> Reference.Custom(union.identifier.value, isNullable = false)
            references.size == 1 -> references.single()
            else -> Reference.Any(isNullable = false)
        }
        return listOfNotNull(Channel(comment = null, annotations = annotations, identifier = DefinitionIdentifier(identifier), reference = reference), union) +
            payloads.flatMap { it.definitions }
    }

    /** The key of the channel an operation is on: the channel it refers to, unless the operation is no plain operation. */
    private fun JsonObject.channelKey(): String? = takeIf { get("action").actionOrNull() != null && ref() == null }
        ?.obj("channel")
        ?.takeIf { it.size == 1 }
        ?.ref()
        ?.let { ref -> ref.removePrefix("#/channels/").takeIf { it != ref && '/' !in it } }
        ?.unescapePointer()
        ?.takeIf { it in channelKeys }

    /** An operation, on the channel at [channelPointer] or on the document; on a channel, its channel is implied. */
    private fun JsonObject.toAnnotation(key: String, channelPointer: String?): Annotation {
        val action = get("action").actionOrNull()
        return annotation(
            (action ?: Action.SEND).annotationName,
            key,
            entries
                .filterNot { (field, value) -> field == "action" && action != null || field == "channel" && channelPointer != null && value == ref(channelPointer) }
                .map { (field, value) -> parameter(field, channelPointer?.takeIf { field == "messages" }?.let { value.toMessageKeys(it) } ?: value.toAnnotationValue()) },
        )
    }

    /** The messages of an operation by their key in its channel, when they all are messages of that channel. */
    private fun JsonElement.toMessageKeys(channelPointer: String): Annotation.Value? = (this as? JsonArray)?.let { messages ->
        messages
            .mapNotNull { message ->
                (message as? JsonObject)?.takeIf { it.size == 1 }?.ref()?.let { ref ->
                    ref.removePrefix("$channelPointer/messages/").takeIf { it != ref && '/' !in it }?.unescapePointer()
                }
            }
            .takeIf { it.size == messages.size }
            ?.let { keys -> Annotation.Value.Array(keys.map { it.toStringValue() }) }
    }

    private fun componentMessage(key: String): ConvertedMessage? = componentMessages[key]
        ?: components?.obj("messages")?.get(key)
            ?.let { message(key, "#/components/messages/${key.escapePointer()}", it) }
            ?.also { componentMessages[key] = it }

    /** A message: its payload and headers are schemas that convert to Wirespec, the rest of it is kept as it is. */
    private fun message(key: String, pointer: String, element: JsonElement): ConvertedMessage {
        val message = element as? JsonObject ?: error("Expected a message object at $pointer")
        return when (val ref = message.ref()) {
            null -> {
                val name = message.string("name") ?: key
                val positions = message.entries.map { (field, value) ->
                    field to when (field) {
                        "payload" -> schemaPosition("$pointer/payload", value, name.toIdentifier().let { if (it in taken) "${it}Payload" else it }, message.string("schemaFormat"))
                        "headers" -> schemaPosition("$pointer/headers", value, "${name}_headers", format = null)
                        else -> value.toAnnotationValue() to null
                    }
                }
                ConvertedMessage(
                    annotation = annotation(MESSAGE, key, positions.map { (field, position) -> parameter(field, position.first) }),
                    payload = positions.firstOrNull { (field) -> field == "payload" }?.second?.second?.reference ?: Reference.Any(isNullable = false),
                    definitions = positions.flatMap { (_, position) -> position.second?.definitions.orEmpty() },
                )
            }
            else -> referencedPayload(ref).let { payload -> ConvertedMessage(annotation(MESSAGE, key, message.toParameters()), payload.reference, payload.definitions) }
        }
    }

    /** The payload of the message [ref] refers to; the definitions of a component message are declared with that message. */
    private fun referencedPayload(ref: String): Converted = when (ref) {
        in resolvingMessages -> null
        else -> {
            resolvingMessages += ref
            when (val key = ref.componentKey("messages")) {
                null -> (lookup(ref) as? JsonObject)?.let { target -> message(ref.substringAfterLast('/').unescapePointer(), ref, target) }?.let { Converted(it.payload, it.definitions) }
                else -> componentMessage(key)?.let { Converted(it.payload) }
            }.also { resolvingMessages -= ref }
        }
    } ?: Converted(Reference.Any(isNullable = false))

    /**
     * A schema in a message: the Wirespec reference it converts to, when that writes the schema back, or else the schema
     * as it is. A schema in another format than JSON Schema, such as Avro, is kept as it is.
     */
    private fun schemaPosition(pointer: String, schema: JsonElement, name: String, format: String?): Pair<Annotation.Value, Converted?> {
        val multiFormat = (schema as? JsonObject)?.takeIf { "schemaFormat" in it && "schema" in it }
        val schemaFormat = multiFormat?.string("schemaFormat") ?: format
        return when {
            schemaFormat?.isJsonSchema() == false -> schema.toAnnotationValue() to null
            multiFormat != null -> schema.toAnnotationValue() to convertAt("$pointer/schema", multiFormat.getValue("schema"), name)
            else -> convertAt(pointer, schema, name).let { converted ->
                when (with(writer) { converted.reference.schema() }) {
                    schema -> converted.reference.toWirespec().toStringValue()
                    else -> schema.toAnnotationValue()
                } to converted
            }
        }
    }

    private fun component(key: String): Converted = convertAt(
        pointer = "#/components/schemas/${key.escapePointer()}",
        schema = schemas.getValue(key),
        name = schemaNames[key] ?: key,
        component = key.takeIf { it in schemaNames },
    )

    /**
     * Converts the schema at [pointer], once: when the same schema comes back, so does its reference, without the
     * definitions. A schema that needs a definition gets one, named after [name], or [name] itself for a [component].
     */
    private fun convertAt(pointer: String, schema: JsonElement, name: String, component: String? = null): Converted = when (val reference = converted[pointer]) {
        null -> when (pointer) {
            // A schema that contains itself through schemas without a definition of their own.
            in inProgress -> Converted(Reference.Any(isNullable = false))
            else -> inProgress.add(pointer)
                .let { convertSchema(pointer, schema, name, component) }
                .also {
                    inProgress.remove(pointer)
                    converted[pointer] = it.reference
                }
        }
        else -> Converted(reference)
    }

    private fun convertSchema(pointer: String, schema: JsonElement, name: String, component: String?): Converted = when (schema) {
        is JsonObject -> schema.ref()?.let(::reference) ?: schema.convertObject(pointer, name, schema.allowsNull(), component)
        else -> Converted(Reference.Any(isNullable = false))
    }

    /** A reference to a component schema. Any other reference stays as it is, in the `@AsyncAPISchema` of where it is used. */
    private fun reference(ref: String): Converted = ref.componentKey("schemas")
        ?.let { key ->
            when (key) {
                in schemaNames -> Converted(Reference.Custom(schemaNames.getValue(key), isNullable = false))
                in schemas -> component(key)
                else -> null
            }
        }
        ?: Converted(Reference.Any(isNullable = false))

    private fun JsonObject.convertObject(pointer: String, name: String, isNullable: Boolean, component: String?): Converted {
        val wrapped = wrapped()
        val branches = nonNullBranches()
        return when {
            wrapped != null -> wrapped.let { (path, schema) -> convertAt("$pointer/$path", schema, name) }
                .let { it.copy(reference = it.reference.nullableIf(isNullable)) }
            branches != null -> when {
                ownsDefinition(component != null) -> union(pointer, identifier(name, component), branches, isNullable, component)
                else -> Converted(Reference.Any(isNullable))
            }
            "allOf" in this -> type(identifier(name, component), allOfParts(pointer), isNullable, component)
            isEnum() -> enum(identifier(name, component), isNullable, component)
            else -> when (val type = primaryType()) {
                "object", null -> when {
                    "properties" in this -> type(identifier(name, component), listOf(Located(pointer, this)), isNullable, component)
                    else -> additionalProperties()
                        ?.let { values -> convertAt("$pointer/additionalProperties", values, "${name}_value") }
                        ?.let { values -> Converted(Reference.Dict(values.reference, isNullable), values.definitions) }
                        ?: when {
                            type == "object" && component != null -> type(name, listOf(Located(pointer, this)), isNullable, component)
                            type == "object" -> Converted(Reference.Dict(Reference.Any(isNullable = false), isNullable))
                            else -> Converted(Reference.Any(isNullable))
                        }
                }
                "array" -> (get("items") as? JsonObject)
                    ?.let { convertAt("$pointer/items", it, "${name}_item") }
                    .let { items -> Converted(Reference.Iterable(items?.reference ?: Reference.Any(isNullable = false), isNullable), items?.definitions.orEmpty()) }
                in PRIMITIVE_TYPES -> when (component) {
                    null -> Converted(Reference.Primitive(primitive(type, constrained = false), isNullable))
                    else -> Converted(
                        Reference.Custom(name, isNullable),
                        listOf(
                            define(
                                Refined(comment = null, annotations = description().toDescription(), identifier = DefinitionIdentifier(name), reference = Reference.Primitive(primitive(type, constrained = true), isNullable = false)),
                                original = this,
                                component = component,
                            ),
                        ),
                    )
                }
                else -> Converted(Reference.Any(isNullable))
            }
        }
    }

    /** Whether the schema, which is not a `$ref`, converts to a definition of its own. Agrees with [convertObject]. */
    private fun JsonObject.ownsDefinition(component: Boolean): Boolean = when (val branches = nonNullBranches()) {
        null -> when {
            wrapped() != null -> false
            "allOf" in this -> true
            isEnum() -> true
            else -> when (primaryType()) {
                "object" -> "properties" in this || component && additionalProperties() == null
                null -> "properties" in this
                in PRIMITIVE_TYPES -> component
                else -> false
            }
        }
        else -> wrapped() == null && branches.size > 1 && branches.all { it.value.isNamed() }
    }

    /** Whether a schema converts to a reference to a definition, as every member of a union has to. */
    private fun JsonElement.isNamed(component: Boolean = false): Boolean = (this as? JsonObject)?.let { schema ->
        when (val ref = schema.ref()) {
            null -> schema.ownsDefinition(component) || schema.wrapped()?.second?.isNamed(component) == true
            else -> ref.componentKey("schemas")?.let { key -> schemas[key]?.isNamed(component = true) } == true
        }
    } ?: false

    private fun JsonObject.union(pointer: String, identifier: String, branches: List<IndexedValue<JsonElement>>, isNullable: Boolean, component: String?): Converted = branches
        .map { (index, branch) -> convertAt("$pointer/${branchKeyword()}/$index", branch, (branch as? JsonObject)?.string("title") ?: "$identifier$index") }
        .let { members ->
            Converted(
                Reference.Custom(identifier, isNullable),
                listOf(
                    define(
                        Union(comment = null, annotations = description().toDescription(), identifier = DefinitionIdentifier(identifier), entries = members.map { it.reference.copy(isNullable = false) }.toSet()),
                        original = this,
                        component = component,
                    ),
                ) + members.flatMap { it.definitions },
            )
        }

    private fun JsonObject.enum(identifier: String, isNullable: Boolean, component: String?): Converted = Converted(
        Reference.Custom(identifier, isNullable),
        listOf(
            define(
                Enum(comment = null, annotations = description().toDescription(), identifier = DefinitionIdentifier(identifier), entries = enumEntries().toSet()),
                original = this,
                component = component,
            ),
        ),
    )

    /** A type with the properties of all [parts]: the schema itself and the schemas it combines with `allOf`. */
    private fun type(identifier: String, parts: List<Located>, isNullable: Boolean, component: String?): Converted {
        val required = parts.flatMap { it.node.required() }.toSet()
        val properties = parts
            .flatMap { part -> part.node.obj("properties").orEmpty().map { (key, property) -> Triple(part.pointer, key, property) } }
            .distinctBy { (_, key) -> key }
        val names = properties.fold(emptyList<String>()) { names, (_, key) -> names + key.toFieldIdentifier().unique(names.toSet()) }
        val fields = properties.zip(names).map { (property, fieldName) ->
            val (partPointer, key, schema) = property
            field("$partPointer/properties/${key.escapePointer()}", key, fieldName, schema, required = key in required, typeName = identifier)
        }
        return Converted(
            Reference.Custom(identifier, isNullable),
            listOf(
                define(
                    Type(comment = null, annotations = parts.first().node.description().toDescription(), identifier = DefinitionIdentifier(identifier), shape = Shape(fields.map { it.first }), extends = emptyList()),
                    original = parts.first().node,
                    component = component,
                ),
            ) + fields.flatMap { it.second },
        )
    }

    /** A property: its description and default belong to the field, the rest of its schema to the type it converts to. */
    private fun field(pointer: String, key: String, identifier: String, property: JsonElement, required: Boolean, typeName: String): Pair<Field, List<Definition>> {
        val schema = (property as? JsonObject)?.let { JsonObject(it - "description" - "default") } ?: property
        val converted = convertAt(pointer, schema, "${typeName}_$key")
        val reference = converted.reference.nullableIf(!required)
        val field = Field(
            annotations = (property as? JsonObject).description().toDescription() + nameAnnotation(key, identifier),
            identifier = FieldIdentifier(identifier),
            reference = reference,
            defaultValue = (property as? JsonObject)?.get("default")?.toDefaultValue(reference)?.takeIf { it.isValidFor(reference, converted.definitions) },
        )
        return (schemaPatch(with(writer) { field.canonical() }, property)?.let { field.copy(annotations = field.annotations + it) } ?: field) to converted.definitions
    }

    /** An enum default has to be an entry of the enum the field refers to. */
    private fun DefaultValue.isValidFor(reference: Reference, definitions: List<Definition>): Boolean = when (this) {
        is DefaultValue.EnumValue -> (reference as? Reference.Custom)
            ?.let { custom ->
                definitions.filterIsInstance<Enum>().find { it.identifier.value == custom.value }?.entries
                    ?: componentKeys[custom.value]?.let { key -> (schemas[key] as? JsonObject)?.takeIf { it.isEnumOnly() }?.enumEntries()?.toSet() }
            }
            ?.contains(value) == true
        else -> true
    }

    /** Adds what a definition needs to come back as the schema it was converted from. */
    private fun define(model: Model, original: JsonElement, component: String?): Model {
        val named = model.withAnnotations(
            model.annotations + listOfNotNull(
                component?.takeIf { it != model.identifier.value }?.let { annotation(NAME, it) },
                annotation(INLINE).takeIf { component == null },
            ),
        )
        return (schemaPatch(with(writer) { named.canonical() }, original)?.let { named.withAnnotations(named.annotations + it) } ?: named)
            .also { if (component == null) inlineDefinitions[it.identifier.value] = it }
    }

    private fun Model.withAnnotations(annotations: List<Annotation>): Model = when (this) {
        is Type -> copy(annotations = annotations)
        is Enum -> copy(annotations = annotations)
        is Union -> copy(annotations = annotations)
        is Refined -> copy(annotations = annotations)
    }

    private fun JsonObject.allOfParts(pointer: String): List<Located> = listOf(Located(pointer, this)) +
        (get("allOf") as? JsonArray).orEmpty().flatMapIndexed { index, part ->
            resolveSchema("$pointer/allOf/$index", part)?.let { it.node.allOfParts(it.pointer) }.orEmpty()
        }

    private fun resolveSchema(pointer: String, schema: JsonElement, seen: Set<String> = emptySet()): Located? = (schema as? JsonObject)?.let { obj ->
        when (val ref = obj.ref()) {
            null -> Located(pointer, obj)
            in seen -> null
            else -> lookup(ref)?.let { resolveSchema(ref, it, seen + ref) }
        }
    }

    /** `@AsyncAPIName` for a [key] that is not the key the emitter writes by default. */
    private fun nameAnnotation(key: String, default: String): List<Annotation> = listOfNotNull(annotation(NAME, key).takeIf { key != default })

    private fun claimChannel(key: String): String = key.toIdentifier().let { if (it in taken) "${it}Channel" else it }.let(::claim)

    private fun identifier(name: String, component: String?): String = component?.let { name } ?: claim(name.toIdentifier())

    private fun claim(name: String): String = name.unique(taken).also { taken += it }

    private fun lookup(ref: String): JsonElement? = ref.takeIf { it.startsWith("#") }
        ?.removePrefix("#")
        ?.split("/")
        ?.drop(1)
        ?.map { it.unescapePointer() }
        ?.fold<String, JsonElement?>(document) { node, segment ->
            when (node) {
                is JsonObject -> node[segment]
                is JsonArray -> segment.toIntOrNull()?.let(node::getOrNull)
                else -> null
            }
        }

    private companion object {
        val SUPPORTED_VERSIONS = setOf(2, 3)
        val PRIMITIVE_TYPES = setOf("string", "integer", "number", "boolean")
        val INTEGER = Regex("-?[0-9]+")
        val NUMBER = Regex("-?[0-9]+(\\.[0-9]+)?")

        /** The characters Wirespec can write in a field name or enum entry, between backticks when it has to. */
        val NAME_CHARACTERS = Regex("[a-zA-Z0-9_\\-]+")
        const val HOLDER = "AsyncAPI"

        fun JsonElement?.actionOrNull(): Action? = (this as? JsonPrimitive)?.takeIf { it.isString }?.let { action -> Action.entries.find { it.keyword == action.content } }

        fun String.isJsonSchema(): Boolean = listOf("application/vnd.aai.asyncapi", "application/schema+json", "application/schema+yaml").any { startsWith(it) }

        fun Reference.nullableIf(isNullable: Boolean): Reference = copy(isNullable = isNullable || this.isNullable)

        fun JsonElement.isString(): Boolean = this is JsonPrimitive && isString

        fun JsonElement.isNonEmptyObject(): Boolean = this is JsonObject && isNotEmpty()

        /** The object without the given fields, each left out only when its value is one the definition holds itself. */
        fun JsonObject.without(vararg fields: Pair<String, (JsonElement) -> Boolean>): Map<String, JsonElement> = filterNot { (key, value) ->
            fields.any { (field, isHeld) -> field == key && isHeld(value) }
        }

        fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject

        fun JsonObject.ref(): String? = string("\$ref")

        fun JsonObject?.description(): String? = this?.string("description")

        fun JsonObject.required(): List<String> = (get("required") as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }

        fun JsonObject.additionalProperties(): JsonObject? = get("additionalProperties") as? JsonObject

        fun JsonObject.types(): List<String> = when (val type = get("type")) {
            is JsonArray -> type.mapNotNull { (it as? JsonPrimitive)?.content }
            is JsonPrimitive -> listOf(type.content)
            else -> emptyList()
        }

        fun JsonObject.primaryType(): String? = types().firstOrNull { it != "null" }

        fun JsonElement.isNullSchema(): Boolean = (this as? JsonObject)?.types() == listOf("null")

        fun JsonObject.branchKeyword(): String = if ("oneOf" in this) "oneOf" else "anyOf"

        fun JsonObject.branches(): JsonArray? = get(branchKeyword()) as? JsonArray

        /** The alternatives of a `oneOf` or `anyOf` other than `null`, by their index among all alternatives. */
        fun JsonObject.nonNullBranches(): List<IndexedValue<JsonElement>>? = branches()?.withIndex()?.filterNot { it.value.isNullSchema() }

        /**
         * The one schema that this schema only wraps, with its path: a single alternative besides `null`, or a single
         * `allOf` part next to no properties.
         */
        fun JsonObject.wrapped(): Pair<String, JsonElement>? = nonNullBranches()
            ?.singleOrNull()
            ?.let { (index, branch) -> "${branchKeyword()}/$index" to branch }
            ?: (get("allOf") as? JsonArray)?.singleOrNull()?.takeIf { "properties" !in this }?.let { "allOf/0" to it }

        fun JsonObject.allowsNull(): Boolean = "null" in types() ||
            (get("nullable") as? JsonPrimitive)?.content == "true" ||
            (get("enum") as? JsonArray)?.any { it is JsonNull } == true ||
            branches()?.any { it.isNullSchema() } == true

        fun JsonObject.isEnum(): Boolean = (get("enum") as? JsonArray)?.any { it !is JsonNull } == true

        /** An enum that nothing else in the schema makes convert to something else first. */
        fun JsonObject.isEnumOnly(): Boolean = isEnum() && wrapped() == null && branches() == null && "allOf" !in this

        /** The values of an enum as entries Wirespec can write, each one different from the others. */
        fun JsonObject.enumEntries(): List<String> = (get("enum") as? JsonArray).orEmpty()
            .filterNot { it is JsonNull }
            .map { (it as? JsonPrimitive)?.content ?: it.toString() }
            .fold(emptyList()) { entries, value -> entries + value.toName().unique(entries.toSet()) }

        fun JsonObject.primitive(type: String?, constrained: Boolean): PrimitiveType = when (type) {
            "string" -> when (string("format")) {
                "binary" -> PrimitiveType.Bytes
                else -> PrimitiveType.String(
                    string("pattern")?.takeIf { constrained && '\n' !in it && '\r' !in it }?.let { PrimitiveType.Constraint.RegExp("/${it.escapeSlashes()}/g") },
                )
            }
            "integer" -> PrimitiveType.Integer(
                precision = if (string("format") == "int32") PrimitiveType.Precision.P32 else PrimitiveType.Precision.P64,
                constraint = bound { it.takeIf(INTEGER::matches) }?.takeIf { constrained },
            )
            "number" -> PrimitiveType.Number(
                precision = if (string("format") == "float") PrimitiveType.Precision.P32 else PrimitiveType.Precision.P64,
                constraint = bound { it.takeIf(NUMBER::matches)?.let { number -> if ('.' in number) number else "$number.0" } }?.takeIf { constrained },
            )
            else -> PrimitiveType.Boolean
        }

        fun JsonObject.bound(format: (String) -> String?): PrimitiveType.Constraint.Bound? = PrimitiveType.Constraint.Bound(
            min = (get("minimum") as? JsonPrimitive)?.takeUnless { it.isString }?.content?.let(format),
            max = (get("maximum") as? JsonPrimitive)?.takeUnless { it.isString }?.content?.let(format),
        ).takeIf { it.min != null || it.max != null }

        /** A Wirespec regex ends at the first `/` that is not escaped. */
        fun String.escapeSlashes(): String = Regex("\\\\.|/").replace(this) { if (it.value == "/") "\\/" else it.value }

        fun String.componentKey(section: String): String? = removePrefix("#/components/$section/")
            .takeIf { it != this && '/' !in it }
            ?.unescapePointer()

        fun String.firstToLower(): String = replaceFirstChar(Char::lowercase)

        fun String.unique(taken: Set<String>): String = generateSequence(1) { it + 1 }
            .map { if (it == 1) this else "$this$it" }
            .first { it !in taken }

        /** A Wirespec type name: the words of [this] in Pascal case, starting with a letter and not naming a primitive. */
        fun String.toIdentifier(): String = split(Regex("[^a-zA-Z0-9]+"))
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
            .dropWhile { !it.isLetter() }
            .replaceFirstChar(Char::uppercase)
            .ifEmpty { "Unnamed" }
            .let { if (it in WirespecType.specificTypes) "AsyncAPI$it" else it }

        /** A field name or enum entry: the characters Wirespec cannot write become `_`. */
        fun String.toName(): String = takeIf(NAME_CHARACTERS::matches) ?: replace(Regex("[^a-zA-Z0-9_\\-]+"), "_").ifEmpty { "_" }

        fun String.toFieldIdentifier(): String = toName()
    }
}
