package community.flock.wirespec.converter.asyncapi

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.WirespecType
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.ADDRESS
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.Action
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.annotation
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toDescription
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.converter.common.toDefaultValue
import community.flock.wirespec.converter.common.withValidEnumDefaults
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type as PrimitiveType

/**
 * Converts an AsyncAPI document, version 2.x or 3.x written in JSON, into Wirespec.
 *
 * Every channel becomes a `channel` that carries the payload of its messages, and the schemas become types, enums,
 * unions and refined types. The address of a channel and the operations the application performs on it are kept in
 * annotations, which [AsyncAPIEmitter] reads to write the channels back.
 */
public object AsyncAPIParser : Parser {

    override fun parse(moduleContent: ModuleContent, strict: Boolean): AST = Json.parseToJsonElement(moduleContent.content)
        .let { it as? JsonObject ?: error("Invalid AsyncAPI document; expected a JSON object") }
        .let { AsyncAPIConverter(it).convert() }
        .let { definitions -> definitions.map { it.withValidEnumDefaults(definitions) } }
        .toNonEmptyListOrNull()
        ?.let { AST(nonEmptyListOf(Module(moduleContent.fileUri, it))) }
        ?: error("Cannot yield an empty AST from AsyncAPI document ${moduleContent.fileUri.value}")
}

/** A converted schema: the reference to it and the definitions it introduced. */
private data class Converted(val reference: Reference, val definitions: List<Definition> = emptyList())

/** A node of the document and the JSON pointer it was found at, after following its `$ref`s. */
private data class Located(val pointer: String, val node: JsonObject)

private data class Message(val location: Located, val name: String)

private data class ChannelSource(
    val key: String,
    val address: String?,
    val description: String?,
    val messages: List<Message>,
    val operations: List<Pair<Action, String?>>,
)

private class AsyncAPIConverter(private val document: JsonObject) {

    private val majorVersion: Int = document.string("asyncapi")
        ?.let { version -> version.substringBefore('.').toIntOrNull()?.takeIf { it in SUPPORTED_VERSIONS } ?: error("Unsupported AsyncAPI version $version; only 2.x and 3.x can be converted") }
        ?: error("Invalid AsyncAPI document; missing 'asyncapi' property")

    private val taken = mutableSetOf<String>()

    private val schemas: Map<String, JsonElement> = document.obj("components")?.obj("schemas").orEmpty()

    /** The component schemas claim their names first, so they keep them. */
    private val schemaNames: Map<String, String> = schemas.keys.associateWith { claim(it.toIdentifier()) }

    /**
     * The reference each schema converted to, by JSON pointer. A schema found again, through a `$ref` or an `allOf`,
     * is not converted again, so the definitions it introduces are declared once: where it was first found.
     */
    private val converted = mutableMapOf<String, Reference>()

    fun convert(): List<Definition> = when (majorVersion) {
        2 -> channelsV2()
        else -> channelsV3()
    }.flatMap { it.convert() } +
        componentMessages().flatMap { it.payload().definitions } +
        schemaNames.keys.flatMap { component(it).definitions }

    private fun channelsV3(): List<ChannelSource> {
        val operations = document.obj("operations").orEmpty().map { (key, operation) -> key to resolve("#/operations/${key.escapePointer()}", operation) }
        return document.obj("channels").orEmpty().map { (key, element) ->
            val channel = resolve("#/channels/${key.escapePointer()}", element)
            ChannelSource(
                key = key,
                address = channel.node.string("address"),
                description = channel.node.string("description"),
                messages = channel.node.obj("messages").orEmpty().map { (messageKey, message) ->
                    resolve("${channel.pointer}/messages/${messageKey.escapePointer()}", message).toMessage(messageKey)
                },
                operations = operations
                    .filter { (_, operation) -> operation.node["channel"]?.let { resolve("${operation.pointer}/channel", it).pointer } == channel.pointer }
                    .mapNotNull { (operationKey, operation) -> Action.entries.find { it.keyword == operation.node.string("action") }?.let { it to operationKey } },
            )
        }
    }

    /** AsyncAPI 2 describes the operations from the client's side: a client subscribes to what the application sends. */
    private fun channelsV2(): List<ChannelSource> = document.obj("channels").orEmpty().map { (address, element) ->
        val channel = resolve("#/channels/${address.escapePointer()}", element)
        val operations = Action.entries.mapNotNull { action ->
            channel.node.obj(action.v2Operation)?.let { action to Located("${channel.pointer}/${action.v2Operation}", it) }
        }
        ChannelSource(
            key = address,
            address = address,
            description = channel.node.string("description"),
            messages = operations.flatMap { (_, operation) ->
                operation.node["message"]?.let { messagesV2("${operation.pointer}/message", it, "${address}_message") }.orEmpty()
            },
            operations = operations.map { (action, operation) -> action to operation.node.string("operationId") },
        )
    }

    private fun messagesV2(pointer: String, element: JsonElement, fallbackName: String): List<Message> = resolve(pointer, element).let { message ->
        (message.node["oneOf"] as? JsonArray)
            ?.mapIndexed { index, it -> resolve("${message.pointer}/oneOf/$index", it).toMessage("$fallbackName$index") }
            ?: listOf(message.toMessage(fallbackName))
    }

    private fun componentMessages(): List<Message> = document.obj("components")?.obj("messages").orEmpty().map { (key, message) ->
        resolve("#/components/messages/${key.escapePointer()}", message).toMessage(key)
    }

    private fun Located.toMessage(fallbackName: String) = Message(
        location = this,
        name = node.string("name") ?: node.string("messageId") ?: pointer.componentKey("messages") ?: fallbackName,
    )

    private fun ChannelSource.convert(): List<Definition> {
        val identifier = key.toIdentifier().let { if (it in taken) "${it}Channel" else it }.let(::claim)
        val payloads = messages.distinctBy { it.location.pointer }.map { it.payload() }
        val references = payloads.map { it.reference }.distinct()
        val union = references.takeIf { it.size > 1 && it.all { reference -> reference is Reference.Custom } }?.let { members ->
            Union(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(claim("${identifier}Message")),
                entries = members.map { it.copy(isNullable = false) }.toSet(),
            )
        }
        val reference = when {
            union != null -> Reference.Custom(union.identifier.value, isNullable = false)
            references.size == 1 -> references.single()
            else -> Reference.Any(isNullable = false)
        }
        return listOfNotNull(
            Channel(comment = null, annotations = annotations(identifier), identifier = DefinitionIdentifier(identifier), reference = reference),
            union,
        ) + payloads.flatMap { it.definitions }
    }

    /** Leaves out what the emitter writes by default: the identifier as address and a single `send` operation. */
    private fun ChannelSource.annotations(identifier: String): List<Annotation> = description.toDescription() +
        listOfNotNull(address?.takeUnless { it == identifier }?.let { annotation(ADDRESS, it) }) +
        operations
            .map { (action, id) -> annotation(action.annotationName, id?.takeUnless { it == action.defaultOperationId(identifier) }) }
            .takeUnless { it == listOf(annotation(Action.SEND.annotationName)) }
            .orEmpty()

    private fun Message.payload(): Converted {
        val payload = location.node["payload"]
        // An AsyncAPI 3 payload can be a multi format schema: the schema next to its format.
        val multiFormat = (payload as? JsonObject)?.takeIf { "schemaFormat" in it && "schema" in it }
        val format = multiFormat?.string("schemaFormat") ?: location.node.string("schemaFormat")
        val schema = multiFormat?.get("schema") ?: payload
        return when {
            schema == null || format?.isJsonSchema() == false -> Converted(Reference.Any(isNullable = false))
            else -> convertAt(
                pointer = "${location.pointer}/payload${if (multiFormat != null) "/schema" else ""}",
                schema = schema,
                name = name.toIdentifier().let { if (it in taken) "${it}Payload" else it },
                isNullable = false,
            )
        }
    }

    private fun component(key: String): Converted = convertAt(
        pointer = "#/components/schemas/${key.escapePointer()}",
        schema = schemas.getValue(key),
        name = schemaNames.getValue(key),
        isNullable = false,
        component = true,
    )

    private fun reference(ref: String, isNullable: Boolean): Converted = ref.componentSchemaKey()
        ?.let { key -> componentReference(key, isNullable) ?: error("Cannot resolve reference $ref") }
        ?: convertAt(ref, lookup(ref), ref.refName(), isNullable)

    /** A component that is a definition of its own is referred to by name, before or without converting it. */
    private fun componentReference(key: String, isNullable: Boolean): Converted? = schemas[key]?.let { schema ->
        when ((schema as? JsonObject)?.ownsDefinition(component = true)) {
            true -> Converted(Reference.Custom(schemaNames.getValue(key), isNullable))
            else -> component(key).let { it.copy(reference = it.reference.nullableIf(isNullable)) }
        }
    }

    /**
     * Converts the schema found at [pointer], once. A schema that needs a definition of its own gets one, called after
     * [name]. A component schema keeps the name it was given, and only a component becomes a refined type.
     */
    private fun convertAt(pointer: String, schema: JsonElement, name: String, isNullable: Boolean, component: Boolean = false): Converted = (converted[pointer]?.let(::Converted) ?: convertSchema(pointer, schema, name, component))
        .let { it.copy(reference = it.reference.nullableIf(isNullable)) }

    private fun convertSchema(pointer: String, schema: JsonElement, name: String, component: Boolean): Converted = when (schema) {
        is JsonObject -> schema.ref()?.let { reference(it, isNullable = false) } ?: schema.convertObject(pointer, name, schema.allowsNull(), component)
        else -> Converted(Reference.Any(isNullable = false))
    }.also { converted[pointer] = it.reference }

    private fun JsonObject.convertObject(pointer: String, name: String, isNullable: Boolean, component: Boolean): Converted {
        val wrapped = wrapped()
        val branches = nonNullBranches()
        return when {
            wrapped != null -> wrapped.let { (path, schema) -> convertAt("$pointer/$path", schema, name, isNullable, component) }
            branches != null -> when {
                ownsDefinition(component) -> union(pointer, identifier(name, component), branches, isNullable)
                else -> Converted(Reference.Any(isNullable))
            }
            "allOf" in this -> type(pointer, identifier(name, component), allOfParts(pointer), isNullable)
            get("enum") is JsonArray -> enum(identifier(name, component), isNullable)
            else -> when (val type = primaryType()) {
                "object", null -> when {
                    "properties" in this -> type(pointer, identifier(name, component), listOf(Located(pointer, this)), isNullable)
                    else -> additionalProperties()
                        ?.let { values -> convertAt("$pointer/additionalProperties", values, "${name}_value", isNullable = false) }
                        ?.let { values -> Converted(Reference.Dict(values.reference, isNullable), values.definitions) }
                        ?: when {
                            type == "object" && component -> type(pointer, name, listOf(Located(pointer, this)), isNullable)
                            type == "object" -> Converted(Reference.Dict(Reference.Any(isNullable = false), isNullable))
                            else -> Converted(Reference.Any(isNullable))
                        }
                }
                "array" -> (get("items") as? JsonObject)
                    ?.let { convertAt("$pointer/items", it, "${name}_item", isNullable = false) }
                    .let { items -> Converted(Reference.Iterable(items?.reference ?: Reference.Any(isNullable = false), isNullable), items?.definitions.orEmpty()) }
                in PRIMITIVE_TYPES -> when {
                    component -> Converted(
                        Reference.Custom(name, isNullable),
                        listOf(Refined(comment = null, annotations = description().toDescription(), identifier = DefinitionIdentifier(name), reference = Reference.Primitive(primitive(type, constrained = true), isNullable = false))),
                    )
                    else -> Converted(Reference.Primitive(primitive(type, constrained = false), isNullable))
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
            get("enum") is JsonArray -> true
            else -> when (primaryType()) {
                "object" -> "properties" in this || component && additionalProperties() == null
                null -> "properties" in this
                in PRIMITIVE_TYPES -> component
                else -> false
            }
        }
        else -> wrapped() == null && branches.size > 1 && branches.all { it.value.isNamed(component = false) }
    }

    /** Whether a reference to the schema is to a definition, as a union needs of every member. */
    private fun JsonElement.isNamed(component: Boolean): Boolean = (this as? JsonObject)?.let { schema ->
        when (val ref = schema.ref()) {
            null -> schema.ownsDefinition(component) || schema.wrapped()?.second?.isNamed(component) == true
            else -> ref.componentSchemaKey()?.let { key -> schemas[key]?.isNamed(component = true) } ?: lookup(ref).isNamed(component = false)
        }
    } ?: false

    private fun JsonObject.union(pointer: String, identifier: String, branches: List<IndexedValue<JsonElement>>, isNullable: Boolean): Converted = branches
        .map { (index, branch) -> convertAt("$pointer/${branchKeyword()}/$index", branch, (branch as? JsonObject)?.string("title") ?: "$identifier$index", isNullable = false) }
        .let { members ->
            Converted(
                Reference.Custom(identifier, isNullable),
                listOf(
                    Union(
                        comment = null,
                        annotations = description().toDescription(),
                        identifier = DefinitionIdentifier(identifier),
                        entries = members.map { it.reference.copy(isNullable = false) }.toSet(),
                    ),
                ) + members.flatMap { it.definitions },
            )
        }

    private fun JsonObject.enum(identifier: String, isNullable: Boolean): Converted = Converted(
        Reference.Custom(identifier, isNullable),
        listOf(
            Enum(
                comment = null,
                annotations = description().toDescription(),
                identifier = DefinitionIdentifier(identifier),
                entries = (get("enum") as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.takeUnless { value -> value is JsonNull }?.content }.toSet(),
            ),
        ),
    )

    /** A type with the properties of all [parts]: the schema itself and the schemas it combines with `allOf`. */
    private fun type(pointer: String, identifier: String, parts: List<Located>, isNullable: Boolean): Converted {
        // Registered up front, so a property that refers back to the type finds it.
        converted[pointer] = Reference.Custom(identifier, isNullable)
        val required = parts.flatMap { it.node.required() }.toSet()
        val fields = parts
            .flatMap { part -> part.node.obj("properties").orEmpty().map { (key, property) -> Triple(part.pointer, key, property) } }
            .distinctBy { (_, key) -> key }
            .map { (partPointer, key, property) ->
                val result = convertAt("$partPointer/properties/${key.escapePointer()}", property.withoutDescription(), "${identifier}_$key", isNullable = key !in required)
                result to Field(
                    annotations = (property as? JsonObject)?.description().toDescription(),
                    identifier = FieldIdentifier(key),
                    reference = result.reference,
                    defaultValue = (property as? JsonObject)?.get("default")?.toDefaultValue(result.reference),
                )
            }
        return Converted(
            Reference.Custom(identifier, isNullable),
            listOf(
                Type(
                    comment = null,
                    annotations = parts.first().node.description().toDescription(),
                    identifier = DefinitionIdentifier(identifier),
                    shape = Shape(fields.map { (_, field) -> field }),
                    extends = emptyList(),
                ),
            ) + fields.flatMap { (result) -> result.definitions },
        )
    }

    private fun JsonObject.allOfParts(pointer: String): List<Located> = listOf(Located(pointer, this)) +
        (get("allOf") as? JsonArray).orEmpty().flatMapIndexed { index, part ->
            resolve("$pointer/allOf/$index", part).let { it.node.allOfParts(it.pointer) }
        }

    private fun identifier(name: String, component: Boolean): String = if (component) name else claim(name.toIdentifier())

    private fun claim(name: String): String = generateSequence(1) { it + 1 }
        .map { if (it == 1) name else "$name$it" }
        .first { it !in taken }
        .also { taken += it }

    private tailrec fun resolve(pointer: String, element: JsonElement, seen: Set<String> = emptySet()): Located = when (val ref = (element as? JsonObject)?.ref()) {
        null -> Located(pointer, element as? JsonObject ?: error("Expected an object at $pointer"))
        in seen -> error("Circular reference $ref")
        else -> resolve(ref, lookup(ref), seen + ref)
    }

    private fun lookup(ref: String): JsonElement = when {
        !ref.startsWith("#") -> error("Cannot resolve reference $ref; only references within the document are supported")
        else -> ref.removePrefix("#").split("/").drop(1)
            .map { it.unescapePointer() }
            .fold<String, JsonElement?>(document) { node, segment ->
                when (node) {
                    is JsonObject -> node[segment]
                    is JsonArray -> segment.toIntOrNull()?.let(node::getOrNull)
                    else -> null
                }
            }
            ?: error("Cannot resolve reference $ref")
    }

    private companion object {
        val SUPPORTED_VERSIONS = setOf(2, 3)
        val PRIMITIVE_TYPES = setOf("string", "integer", "number", "boolean")
        val INTEGER = Regex("-?[0-9]+")
        val NUMBER = Regex("-?[0-9]+\\.[0-9]+")

        val Action.v2Operation: String get() = when (this) {
            Action.SEND -> "subscribe"
            Action.RECEIVE -> "publish"
        }

        fun String.isJsonSchema(): Boolean = listOf("application/vnd.aai.asyncapi", "application/schema+json", "application/schema+yaml").any { startsWith(it) }

        fun Reference.nullableIf(isNullable: Boolean): Reference = copy(isNullable = isNullable || this.isNullable)

        fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject

        fun JsonObject.ref(): String? = string("\$ref")

        fun JsonObject?.description(): String? = this?.string("description")

        fun JsonObject.required(): List<String> = (get("required") as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }

        fun JsonObject.additionalProperties(): JsonObject? = get("additionalProperties") as? JsonObject

        fun JsonElement.withoutDescription(): JsonElement = (this as? JsonObject)?.let { JsonObject(it - "description") } ?: this

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
         * `allOf` part next to no properties, which is how a reference gets a description or default in JSON Schema.
         */
        fun JsonObject.wrapped(): Pair<String, JsonElement>? = nonNullBranches()
            ?.singleOrNull()
            ?.let { (index, branch) -> "${branchKeyword()}/$index" to branch }
            ?: (get("allOf") as? JsonArray)?.singleOrNull()?.takeIf { "properties" !in this }?.let { "allOf/0" to it }

        fun JsonObject.allowsNull(): Boolean = "null" in types() ||
            (get("nullable") as? JsonPrimitive)?.content == "true" ||
            (get("enum") as? JsonArray)?.any { it is JsonNull } == true ||
            branches()?.any { it.isNullSchema() } == true

        fun JsonObject.primitive(type: String?, constrained: Boolean): PrimitiveType = when (type) {
            "string" -> when (string("format")) {
                "binary" -> PrimitiveType.Bytes
                else -> PrimitiveType.String(string("pattern")?.takeIf { constrained }?.let { PrimitiveType.Constraint.RegExp("/${it.escapeSlashes()}/g") })
            }
            "integer" -> PrimitiveType.Integer(
                precision = if (string("format") == "int32") PrimitiveType.Precision.P32 else PrimitiveType.Precision.P64,
                constraint = bound { it.takeIf(INTEGER::matches) }?.takeIf { constrained },
            )
            "number" -> PrimitiveType.Number(
                precision = if (string("format") == "float") PrimitiveType.Precision.P32 else PrimitiveType.Precision.P64,
                constraint = bound { if (INTEGER.matches(it)) "$it.0" else it.takeIf(NUMBER::matches) }?.takeIf { constrained },
            )
            else -> PrimitiveType.Boolean
        }

        fun JsonObject.bound(format: (String) -> String?): PrimitiveType.Constraint.Bound? = PrimitiveType.Constraint.Bound(
            min = (get("minimum") as? JsonPrimitive)?.content?.let(format),
            max = (get("maximum") as? JsonPrimitive)?.content?.let(format),
        ).takeIf { it.min != null || it.max != null }

        /** A Wirespec regex ends at the first `/` that is not escaped. */
        fun String.escapeSlashes(): String = Regex("\\\\.|/").replace(this) { if (it.value == "/") "\\/" else it.value }

        fun String.escapePointer(): String = replace("~", "~0").replace("/", "~1")

        fun String.unescapePointer(): String = replace("~1", "/").replace("~0", "~")

        fun String.componentKey(section: String): String? = removePrefix("#/components/$section/")
            .takeIf { it != this && '/' !in it }
            ?.unescapePointer()

        fun String.componentSchemaKey(): String? = componentKey("schemas")

        /** The name for a schema found at [this] pointer: its key, or its key with its message for a payload. */
        fun String.refName(): String = split("/")
            .map { it.unescapePointer() }
            .let { segments -> segments.takeLast(if (segments.last() in setOf("payload", "schema")) 2 else 1) }
            .joinToString("_")

        /** A Wirespec type name: the words of [this] in Pascal case, starting with a letter and not naming a primitive. */
        fun String.toIdentifier(): String = split(Regex("[^a-zA-Z0-9]+"))
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
            .dropWhile { !it.isLetter() }
            .replaceFirstChar(Char::uppercase)
            .ifEmpty { "Unnamed" }
            .let { if (it in WirespecType.specificTypes) "AsyncAPI$it" else it }
    }
}
