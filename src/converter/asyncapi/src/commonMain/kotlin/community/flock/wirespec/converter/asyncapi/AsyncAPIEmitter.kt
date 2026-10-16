package community.flock.wirespec.converter.asyncapi

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.Action
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.CHANNEL
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.COMPONENTS
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DESCRIPTION
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DOCUMENT
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.INLINE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.MESSAGE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.NAME
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.fields
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.find
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.has
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.named
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.single
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.value
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Emits a single AsyncAPI document for all modules.
 *
 * Wirespec converted from AsyncAPI gives back the document it came from: its `@AsyncAPIDocument` holds the version
 * and everything else that belongs to the document as a whole, and the other annotations hold what the definitions
 * cannot. Any other Wirespec gets an AsyncAPI 3.0 document in which every channel has one message, carrying the type
 * of the channel, and a `send` operation, unless annotations say otherwise. Types, enums, unions and refined types
 * become schemas. Endpoints and rpcs have no AsyncAPI counterpart and are left out.
 */
public object AsyncAPIEmitter : Emitter {

    override val extension: FileExtension = FileExtension.JSON

    override val supportsDefaults: Boolean = true

    private val json = Json { prettyPrint = true }

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .onEach { logger.info("Emitting AsyncAPI from ${it.fileUri.value}") }
        .flatMap { it.statements }
        .let { WirespecToAsyncAPI(it).document() }
        .let { nonEmptyListOf(Emitted("asyncapi.${extension.value}", json.encodeToString(JsonObject.serializer(), it))) }
}

private class WirespecToAsyncAPI(definitions: List<Definition>) {

    private val holder: Type? = definitions.filterIsInstance<Type>().find { it.annotations.has(DOCUMENT) }

    /** Wirespec converted from AsyncAPI says everything in annotations, so nothing is added by default. */
    private val fromAsyncAPI = holder != null

    private val models = definitions.filterIsInstance<Model>().filterNot { it == holder }

    private val inlineDefinitions = models.filter { it.annotations.has(INLINE) }.associateBy { it.identifier.value }

    private val componentModels = models.filterNot { it.annotations.has(INLINE) }

    private val componentKeys = componentModels.associate { it.identifier.value to (it.annotations.single(NAME) ?: it.identifier.value) }

    private val writer = JsonSchemaWriter(inlineDefinitions::get) { componentKeys[it] ?: it }

    private val channels = definitions.filterIsInstance<Channel>()

    private val root: JsonObject = holder?.annotations?.find(DOCUMENT)?.fields() ?: DEFAULT_DOCUMENT

    private val majorVersion = if ((root["asyncapi"] as? JsonPrimitive)?.content?.startsWith("2.") == true) 2 else 3

    fun document(): JsonObject = JsonObject(
        root + jsonObjectOf(
            "channels" to channels.associate { it.key() to it.toChannel() }.toJsonObjectOrNull(),
            "operations" to operations().takeIf { majorVersion == 3 }?.toJsonObjectOrNull(),
            "components" to components(),
        ),
    )

    private fun Channel.toChannel(): JsonObject {
        val fields = annotations.find(CHANNEL)?.fields().orEmpty()
        return when {
            REF in fields -> fields
            majorVersion == 2 -> JsonObject(
                fields + jsonObjectOf("description" to description()) +
                    annotations.operations().associate { (annotation, action) -> action.v2Keyword to annotation.toOperationV2(messages()) },
            )
            else -> JsonObject(
                mapOf("address" to JsonPrimitive(key())).takeUnless { fromAsyncAPI }.orEmpty() +
                    fields +
                    jsonObjectOf("description" to description(), "messages" to messages().toJsonObjectOrNull()),
            )
        }
    }

    private fun Channel.key(): String = annotations.single(NAME) ?: identifier.value.replaceFirstChar(Char::lowercase)

    private fun Channel.description(): JsonPrimitive? = (annotations.single(DESCRIPTION) ?: comment?.value)?.let(::JsonPrimitive)

    /** The messages of the channel by their key, or one message that carries the type of the channel. */
    private fun Channel.messages(): Map<String, JsonElement> = annotations.named(MESSAGE)
        .mapNotNull { message -> message.value()?.let { it to message.toMessage() } }
        .toMap()
        .ifEmpty { if (fromAsyncAPI) emptyMap() else mapOf(defaultMessageKey() to ref("#/components/messages/${defaultMessageKey().escapePointer()}")) }

    private fun Channel.defaultMessageKey(): String = (reference as? Reference.Custom)?.value ?: "${identifier.value}Message"

    private fun Annotation.toMessage(): JsonObject = JsonObject(fields().mapValues { (field, value) -> if (field in SCHEMA_FIELDS) value.toSchema() else value })

    /** A schema in a message: a string names the Wirespec type it is, anything else is the schema itself. */
    private fun JsonElement.toSchema(): JsonElement = (this as? JsonPrimitive)?.takeIf { it.isString }?.let { with(writer) { it.content.toReference().schema() } } ?: this

    private fun operations(): Map<String, JsonElement> = channels.flatMap { channel ->
        channel.annotations.operations()
            .ifEmpty { if (fromAsyncAPI) emptyList() else listOf(AsyncAPIAnnotations.annotation(Action.SEND.annotationName) to Action.SEND) }
            .map { (annotation, action) ->
                (annotation.value() ?: action.defaultOperationId(channel.identifier.value)) to
                    annotation.toOperation(action, "#/channels/${channel.key().escapePointer()}", channel.messages().keys)
            }
    }.toMap() + holder?.annotations.orEmpty().operations().map { (annotation, action) -> annotation.value().orEmpty() to annotation.toOperation(action, channelPointer = null, messageKeys = emptySet()) }

    private fun List<Annotation>.operations(): List<Pair<Annotation, Action>> = mapNotNull { annotation ->
        Action.entries.find { it.annotationName == annotation.name }?.let { annotation to it }
    }

    /** An operation on the channel at [channelPointer], which it refers to unless it says otherwise. */
    private fun Annotation.toOperation(action: Action, channelPointer: String?, messageKeys: Set<String>): JsonObject {
        val fields = fields()
        return when {
            REF in fields -> fields
            else -> JsonObject(
                mapOf("action" to JsonPrimitive(action.keyword)) +
                    channelPointer?.let { mapOf("channel" to ref(it)) }.orEmpty() +
                    channelPointer?.takeUnless { fromAsyncAPI }?.let { mapOf("messages" to messageKeys.toMessageRefs(it)) }.orEmpty() +
                    fields.mapValues { (field, value) -> if (field == "messages" && channelPointer != null) value.toMessageRefs(channelPointer) else value },
            )
        }
    }

    /** Messages named by their key in the channel, as references to those messages. */
    private fun JsonElement.toMessageRefs(channelPointer: String): JsonElement = (this as? JsonArray)
        ?.takeIf { messages -> messages.all { it is JsonPrimitive && it.isString } }
        ?.let { keys -> keys.map { (it as JsonPrimitive).content }.toSet().toMessageRefs(channelPointer) }
        ?: this

    private fun Set<String>.toMessageRefs(channelPointer: String): JsonArray = JsonArray(map { ref("$channelPointer/messages/${it.escapePointer()}") })

    /** An AsyncAPI 2 operation, which holds its message itself, or the messages it may be in a `oneOf`. */
    private fun Annotation.toOperationV2(messages: Map<String, JsonElement>): JsonObject = JsonObject(
        value()?.let { mapOf("operationId" to JsonPrimitive(it)) }.orEmpty() +
            fields().mapValues { (field, value) -> if (field == "message") value.toMessage(messages) else value },
    )

    private fun JsonElement.toMessage(messages: Map<String, JsonElement>): JsonElement = when {
        this is JsonPrimitive && isString -> messages[content] ?: this
        this is JsonArray && all { it is JsonPrimitive && it.isString } -> jsonObjectOf("oneOf" to JsonArray(map { messages[(it as JsonPrimitive).content] ?: it }))
        else -> this
    }

    private fun components(): JsonObject? {
        val fields = holder?.annotations?.find(COMPONENTS)?.fields()
        val schemas = componentModels.associate { componentKeys.getValue(it.identifier.value) to with(writer) { it.schema() } }
        val messages = holder?.annotations.orEmpty().named(MESSAGE).mapNotNull { message -> message.value()?.let { it to message.toMessage() } }.toMap() +
            channels.takeUnless { fromAsyncAPI }.orEmpty().filterNot { it.annotations.has(MESSAGE) }.associate { channel ->
                channel.defaultMessageKey() to jsonObjectOf("name" to JsonPrimitive(channel.defaultMessageKey()), "payload" to with(writer) { channel.reference.schema() })
            }
        return when {
            fields == null && schemas.isEmpty() && messages.isEmpty() -> null
            else -> JsonObject(
                fields.orEmpty() + jsonObjectOf(
                    "schemas" to schemas.takeIf { it.isNotEmpty() }?.let { JsonObject(((fields?.get("schemas") as? JsonObject).orEmpty()) + it) },
                    "messages" to messages.toJsonObjectOrNull(),
                ),
            )
        }
    }

    private companion object {
        const val REF = "\$ref"
        val SCHEMA_FIELDS = setOf("payload", "headers")
        val DEFAULT_DOCUMENT = jsonObjectOf(
            "asyncapi" to JsonPrimitive("3.0.0"),
            "info" to jsonObjectOf("title" to JsonPrimitive("Wirespec"), "version" to JsonPrimitive("0.0.0")),
            "defaultContentType" to JsonPrimitive("application/json"),
        )
    }
}
