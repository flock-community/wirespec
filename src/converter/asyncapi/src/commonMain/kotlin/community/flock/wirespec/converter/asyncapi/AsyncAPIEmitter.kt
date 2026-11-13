package community.flock.wirespec.converter.asyncapi

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.SpecInfo
import community.flock.wirespec.compiler.core.emit.SpecInfoEmitter
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.Action
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.CHANNEL
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.COMPONENTS
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DESCRIPTION
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DOCUMENT
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.INFO
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.INLINE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.MESSAGE
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.NAME
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.SECURITY_SCHEME
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.SECURITY_SCHEMES_POINTER
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.SERVER
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.TAG
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
 * Wirespec converted from AsyncAPI gives back the document it came from: its `@spec:AsyncAPIDocument` holds the
 * version and everything else that belongs to the document as a whole, and the other annotations hold what the
 * definitions cannot. Any other Wirespec gets an AsyncAPI 3.0 document in which every channel has one message, carrying
 * the type of the channel, and a `send` operation, unless annotations say otherwise. Types, enums, unions and refined
 * types become schemas. Endpoints and rpcs have no AsyncAPI counterpart and are left out.
 */
public object AsyncAPIEmitter : SpecInfoEmitter {

    override val extension: FileExtension = FileExtension.JSON

    override val supportsDefaults: Boolean = true

    private val json = Json { prettyPrint = true }

    /** The annotations that only the document as a whole has, so only the file header holds them. */
    private val documentOnly = setOf(DOCUMENT, COMPONENTS, INFO, SERVER, SECURITY_SCHEME, TAG)

    /** The spec annotations whose positional value is the key of what they declare. */
    private val keyed = setOf(SERVER, SECURITY_SCHEME, TAG)

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = emit(ast, SpecInfo(), logger)

    override fun withSpecInfo(info: SpecInfo): Emitter = object : SpecInfoEmitter by this {
        override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = emit(ast, info, logger)
    }

    private fun emit(ast: AST, info: SpecInfo, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .onEach { logger.info("Emitting AsyncAPI from ${it.fileUri.value}") }
        .flatMap { it.statements }
        .let { WirespecToAsyncAPI(ast.annotations.map(SpecAnnotation::annotation), it, info).document() }
        .let { nonEmptyListOf(Emitted("asyncapi.${extension.value}", json.encodeToString(JsonObject.serializer(), it))) }

    override fun validate(ast: AST): List<EmitterException> = ast.misplacedDocumentAnnotations() +
        ast.fileAnnotations() +
        ast.annotations.declaredTwice() +
        ast.annotations.withoutKey() +
        ast.annotations.declaredInTheDocumentToo() +
        ast.annotations.undeclaredSecuritySchemes()

    private fun AST.fileAnnotations(): List<EmitterException> = modules.toList().flatMap { module ->
        module.annotations.filter { it.name.startsWith("AsyncAPI") }.map {
            EmitterException(module.fileUri, "@file:${it.name} in ${module.fileUri.value}: an AsyncAPI document has no metadata per file, so write @spec:${it.name}")
        }
    }

    private fun List<SpecAnnotation>.withoutKey(): List<EmitterException> = filter { it.annotation.name in keyed && it.annotation.value() == null }
        .map { EmitterException(it.fileUri, "@spec:${it.annotation.name} in ${it.fileUri.value} has no name") }

    /** What a dedicated annotation declares cannot be in `@spec:AsyncAPIDocument` or `@spec:AsyncAPIComponents` as well. */
    private fun List<SpecAnnotation>.declaredInTheDocumentToo(): List<EmitterException> = map { it.annotation }.let { spec ->
        val document = spec.find(DOCUMENT)?.fields().orEmpty()
        val components = spec.find(COMPONENTS)?.fields().orEmpty()
        mapNotNull { declared ->
            when (declared.annotation.name) {
                INFO -> "The info of the document is declared in @spec:$INFO and in @spec:$DOCUMENT".takeIf { "info" in document }
                SERVER -> declared.annotation.value()
                    ?.takeIf { (document["servers"] as? JsonObject)?.containsKey(it) == true }
                    ?.let { "The server \"$it\" is declared in @spec:$SERVER and in the servers of @spec:$DOCUMENT" }
                SECURITY_SCHEME -> declared.annotation.value()
                    ?.takeIf { (components["securitySchemes"] as? JsonObject)?.containsKey(it) == true }
                    ?.let { "The security scheme \"$it\" is declared in @spec:$SECURITY_SCHEME and in the securitySchemes of @spec:$COMPONENTS" }
                else -> null
            }?.let { EmitterException(declared.fileUri, "$it (${declared.fileUri.value})") }
        }
    }

    /** A server names the security schemes it needs in `security: [...]`, and the spec has to declare them. */
    private fun List<SpecAnnotation>.undeclaredSecuritySchemes(): List<EmitterException> = map { it.annotation }.let { spec ->
        val declared = spec.named(SECURITY_SCHEME).mapNotNull { it.value() } + (spec.find(COMPONENTS)?.fields()?.get("securitySchemes") as? JsonObject)?.keys.orEmpty()
        filter { it.annotation.name == SERVER }.flatMap { server ->
            (server.annotation.fields()["security"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonPrimitive)?.takeIf { name -> name.isString }?.content }
                .filterNot { it in declared }
                .map {
                    EmitterException(
                        server.fileUri,
                        "@spec:$SERVER(\"${server.annotation.value()}\") in ${server.fileUri.value} names the security scheme $it, which no @spec:$SECURITY_SCHEME declares",
                    )
                }
        }
    }

    private fun AST.misplacedDocumentAnnotations(): List<EmitterException> = modules.toList()
        .flatMap { module -> module.statements.map { module.fileUri to it } }
        .flatMap { (fileUri, definition) ->
            (definition as? HasAnnotations)?.annotations.orEmpty()
                .filter { it.name in documentOnly }
                .map {
                    EmitterException(
                        fileUri,
                        "@${it.name} on ${definition.identifier.value} describes the whole AsyncAPI document, so it belongs in the file header: write it as @spec:${it.name}",
                    )
                }
        }

    /** All files of a spec make one document, so what it has once can be declared in one file only. */
    private fun List<SpecAnnotation>.declaredTwice(): List<EmitterException> = mapNotNull { spec -> spec.annotation.declaration()?.let { it to spec } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .map { (declaration, declared) ->
            EmitterException(
                declared[1].fileUri,
                "$declaration is declared ${declared.size} times (in ${declared.map { it.fileUri.value }.distinct().joinToString()}), but the AsyncAPI document can only have one",
            )
        }

    /**
     * What a `@spec:` annotation declares that an AsyncAPI document has only one of: the document, its components, info
     * and description, or a server, security scheme, tag, component message or operation by key.
     */
    private fun Annotation.declaration(): String? = when {
        name in setOf(DOCUMENT, COMPONENTS, INFO, DESCRIPTION) -> "@spec:$name"
        name == SERVER -> value()?.let { "The server \"$it\"" }
        name == SECURITY_SCHEME -> value()?.let { "The security scheme \"$it\"" }
        name == TAG -> value()?.let { "The tag \"$it\"" }
        name == MESSAGE -> value()?.let { "The component message \"$it\"" }
        Action.entries.any { it.annotationName == name } -> value()?.let { "The operation \"$it\"" }
        else -> null
    }
}

private class WirespecToAsyncAPI(private val spec: List<Annotation>, definitions: List<Definition>, private val info: SpecInfo) {

    private val document: Annotation? = spec.find(DOCUMENT)

    /** Wirespec converted from AsyncAPI says everything in annotations, so nothing is added by default. */
    private val fromAsyncAPI = document != null

    private val models = definitions.filterIsInstance<Model>()

    private val inlineDefinitions = models.filter { it.annotations.has(INLINE) }.associateBy { it.identifier.value }

    private val componentModels = models.filterNot { it.annotations.has(INLINE) }

    private val componentKeys = componentModels.associate { it.identifier.value to (it.annotations.single(NAME) ?: it.identifier.value) }

    private val writer = JsonSchemaWriter(inlineDefinitions::get) { componentKeys[it] ?: it }

    private val channels = definitions.filterIsInstance<Channel>()

    private val base: JsonObject = document?.fields() ?: DEFAULT_DOCUMENT

    private val majorVersion = if ((base["asyncapi"] as? JsonPrimitive)?.content?.startsWith("2.") == true) 2 else 3

    private val root: JsonObject = base.withSpecAnnotations().withInfo()

    /** The info, the servers and, in AsyncAPI 2, the tags of the document, with what their `@spec:` annotations add. */
    private fun JsonObject.withSpecAnnotations(): JsonObject = JsonObject(
        this + jsonObjectOf(
            "info" to (this["info"] as? JsonObject).withInfoAnnotations(),
            "servers" to (this["servers"] as? JsonObject).withServers(),
            "tags" to (this["tags"] as? JsonArray).withTags()?.takeIf { majorVersion == 2 },
        ),
    )

    /** The fields of `@spec:AsyncAPIInfo`, the `@spec:Description` and, in AsyncAPI 3, the `@spec:AsyncAPITag`s. */
    private fun JsonObject?.withInfoAnnotations(): JsonObject? = listOfNotNull(
        spec.find(INFO)?.fields(),
        spec.single(DESCRIPTION)?.let { jsonObjectOf("description" to JsonPrimitive(it)) },
        (this?.get("tags") as? JsonArray).withTags()?.takeIf { majorVersion == 3 }?.let { jsonObjectOf("tags" to it) },
    ).takeIf { it.isNotEmpty() }?.fold(JsonObject(orEmpty())) { info, fields -> JsonObject(info + fields) }

    private fun JsonArray?.withTags(): JsonArray? = spec.named(TAG)
        .mapNotNull { tag -> tag.value()?.let { JsonObject(mapOf("name" to JsonPrimitive(it)) + tag.fields()) } }
        .takeIf { it.isNotEmpty() }
        ?.let { JsonArray(orEmpty() + it) }

    private fun JsonObject?.withServers(): JsonObject? = spec.named(SERVER)
        .mapNotNull { server -> server.value()?.let { it to JsonObject(server.fields().mapValues { (field, value) -> if (field == "security") value.toSecurity() else value }) } }
        .takeIf { it.isNotEmpty() }
        ?.let { JsonObject(orEmpty() + it) }

    /** `security: ["saslScram"]` names security schemes: a reference to each in AsyncAPI 3, a requirement without scopes in AsyncAPI 2. */
    private fun JsonElement.toSecurity(): JsonElement = (this as? JsonArray)
        ?.takeIf { names -> names.all { it is JsonPrimitive && it.isString } }
        ?.let { names ->
            JsonArray(
                names.map { (it as JsonPrimitive).content }.map { name ->
                    when (majorVersion) {
                        2 -> jsonObjectOf(name to JsonArray(emptyList()))
                        else -> ref("$SECURITY_SCHEMES_POINTER${name.escapePointer()}")
                    }
                },
            )
        }
        ?: this

    /** The title and version of the build replace the ones of the document, each where it is set. */
    private fun JsonObject.withInfo(): JsonObject = listOfNotNull(info.title?.let { "title" to JsonPrimitive(it) }, info.version?.let { "version" to JsonPrimitive(it) })
        .takeIf { it.isNotEmpty() }
        ?.let { JsonObject(this + ("info" to JsonObject((this["info"] as? JsonObject).orEmpty() + it))) }
        ?: this

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
    }.toMap() + spec.operations().map { (annotation, action) -> annotation.value().orEmpty() to annotation.toOperation(action, channelPointer = null, messageKeys = emptySet()) }

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
        val fields = spec.find(COMPONENTS)?.fields()
        val securitySchemes = spec.named(SECURITY_SCHEME).mapNotNull { scheme -> scheme.value()?.let { it to scheme.fields() } }.toMap()
        val schemas = componentModels.associate { componentKeys.getValue(it.identifier.value) to with(writer) { it.schema() } }
        val messages = spec.named(MESSAGE).mapNotNull { message -> message.value()?.let { it to message.toMessage() } }.toMap() +
            channels.takeUnless { fromAsyncAPI }.orEmpty().filterNot { it.annotations.has(MESSAGE) }.associate { channel ->
                channel.defaultMessageKey() to jsonObjectOf("name" to JsonPrimitive(channel.defaultMessageKey()), "payload" to with(writer) { channel.reference.schema() })
            }
        return when {
            fields == null && schemas.isEmpty() && messages.isEmpty() && securitySchemes.isEmpty() -> null
            else -> JsonObject(
                fields.orEmpty() + jsonObjectOf(
                    "schemas" to schemas.takeIf { it.isNotEmpty() }?.let { JsonObject(((fields?.get("schemas") as? JsonObject).orEmpty()) + it) },
                    "messages" to messages.toJsonObjectOrNull(),
                    "securitySchemes" to securitySchemes.takeIf { it.isNotEmpty() }?.let { JsonObject(((fields?.get("securitySchemes") as? JsonObject).orEmpty()) + it) },
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
