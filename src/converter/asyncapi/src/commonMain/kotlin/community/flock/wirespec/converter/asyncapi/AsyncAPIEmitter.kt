package community.flock.wirespec.converter.asyncapi

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.HasMetaData
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.ADDRESS
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.Action
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DESCRIPTION
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.single
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.value
import community.flock.wirespec.converter.common.toJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type as PrimitiveType

/**
 * Emits a single AsyncAPI 3.0 document for all modules. Every channel becomes a channel with one message, which
 * carries the channel's type as payload, and the operations named in `@AsyncAPISend` and `@AsyncAPIReceive`, or a
 * `send` operation when there are none. Types, enums, unions and refined types become schemas. Endpoints and rpcs have
 * no AsyncAPI counterpart and are left out.
 */
public object AsyncAPIEmitter : Emitter {

    private const val ASYNCAPI_VERSION = "3.0.0"
    private const val APPLICATION_JSON = "application/json"
    private const val REF = "\$ref"
    private val NULL_SCHEMA = JsonObject(mapOf("type" to JsonPrimitive("null")))

    override val extension: FileExtension = FileExtension.JSON

    override val supportsDefaults: Boolean = true

    private val json = Json { prettyPrint = true }

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .onEach { logger.info("Emitting AsyncAPI from ${it.fileUri.value}") }
        .flatMap { it.statements }
        .toAsyncAPI()
        .let { nonEmptyListOf(Emitted("asyncapi.${extension.value}", json.encodeToString(JsonObject.serializer(), it))) }

    private fun List<Definition>.toAsyncAPI(): JsonObject {
        val channels = filterIsInstance<Channel>()
        return jsonObjectOf(
            "asyncapi" to JsonPrimitive(ASYNCAPI_VERSION),
            "info" to jsonObjectOf("title" to JsonPrimitive("Wirespec"), "version" to JsonPrimitive("0.0.0")),
            "defaultContentType" to JsonPrimitive(APPLICATION_JSON),
            "channels" to channels.associate { it.identifier.value to it.toChannel() }.toJsonObjectOrNull(),
            "operations" to channels.flatMap { it.toOperations() }.toMap().toJsonObjectOrNull(),
            "components" to jsonObjectOf(
                "schemas" to filterIsInstance<Model>().associate { it.identifier.value to it.toSchema() }.toJsonObjectOrNull(),
                "messages" to channels.associate { it.messageName() to it.toMessage() }.toJsonObjectOrNull(),
            ).takeIf { it.isNotEmpty() },
        )
    }

    private fun Channel.toChannel(): JsonObject = jsonObjectOf(
        "address" to JsonPrimitive(annotations.single(ADDRESS) ?: identifier.value),
        "description" to description()?.let(::JsonPrimitive),
        "messages" to jsonObjectOf(messageName() to ref("#/components/messages/${messageName()}")),
    )

    private fun Channel.toOperations(): List<Pair<String, JsonObject>> = annotations
        .mapNotNull { annotation -> Action.entries.find { it.annotationName == annotation.name }?.let { it to annotation.value() } }
        .ifEmpty { listOf(Action.SEND to null) }
        .map { (action, id) ->
            (id ?: action.defaultOperationId(identifier.value)) to jsonObjectOf(
                "action" to JsonPrimitive(action.keyword),
                "channel" to ref("#/channels/${identifier.value}"),
                "messages" to JsonArray(listOf(ref("#/channels/${identifier.value}/messages/${messageName()}"))),
            )
        }

    private fun Channel.toMessage(): JsonObject = jsonObjectOf(
        "name" to JsonPrimitive(messageName()),
        "payload" to reference.toSchema(),
    )

    private fun Channel.messageName(): String = (reference as? Reference.Custom)?.value ?: "${identifier.value}Message"

    private fun Model.toSchema(): JsonObject = when (this) {
        is Type -> jsonObjectOf(
            "type" to JsonPrimitive("object"),
            "description" to description()?.let(::JsonPrimitive),
            "properties" to shape.fields.associate { it.identifier.value to it.toSchema() }.toJsonObjectOrNull(),
            "required" to shape.fields
                .filterNot { it.reference.isNullable }
                .map { JsonPrimitive(it.identifier.value) }
                .takeIf { it.isNotEmpty() }
                ?.let(::JsonArray),
        )
        is Enum -> jsonObjectOf(
            "type" to JsonPrimitive("string"),
            "description" to description()?.let(::JsonPrimitive),
            "enum" to JsonArray(entries.map(::JsonPrimitive)),
        )
        is Union -> jsonObjectOf(
            "description" to description()?.let(::JsonPrimitive),
            "oneOf" to JsonArray(entries.map { it.toSchema() }),
        )
        is Refined -> reference.type.toSchema().withEntries("description" to description()?.let(::JsonPrimitive))
    }

    /**
     * A field that may be left out is not `required`, so its own schema does not admit `null`. JSON Schema ignores what
     * sits next to a `$ref`, so a reference with a description or default is wrapped in an `allOf`.
     */
    private fun Field.toSchema(): JsonObject {
        val schema = reference.copy(isNullable = false).toSchema()
        val details = jsonObjectOf(
            "description" to annotations.single(DESCRIPTION)?.let(::JsonPrimitive),
            "default" to defaultValue?.takeUnless { it == DefaultValue.NullValue }?.toJsonElement(),
        )
        return when {
            details.isEmpty() -> schema
            REF in schema -> JsonObject(mapOf("allOf" to JsonArray(listOf(schema))) + details)
            else -> JsonObject(schema + details)
        }
    }

    private fun Reference.toSchema(): JsonObject = when (this) {
        is Reference.Custom -> ref("#/components/schemas/$value")
        is Reference.Primitive -> type.toSchema()
        is Reference.Iterable -> jsonObjectOf("type" to JsonPrimitive("array"), "items" to reference.toSchema())
        is Reference.Dict -> jsonObjectOf("type" to JsonPrimitive("object"), "additionalProperties" to reference.toSchema())
        is Reference.Any -> JsonObject(emptyMap())
        is Reference.Unit -> NULL_SCHEMA
    }.let { if (isNullable) it.nullable() else it }

    private fun PrimitiveType.toSchema(): JsonObject = when (this) {
        is PrimitiveType.String -> jsonObjectOf(
            "type" to JsonPrimitive("string"),
            "pattern" to constraint?.value?.toPattern()?.let(::JsonPrimitive),
        )
        is PrimitiveType.Integer -> jsonObjectOf(
            "type" to JsonPrimitive("integer"),
            "format" to JsonPrimitive(if (precision == PrimitiveType.Precision.P32) "int32" else "int64"),
            "minimum" to constraint?.min?.toJsonNumber(),
            "maximum" to constraint?.max?.toJsonNumber(),
        )
        is PrimitiveType.Number -> jsonObjectOf(
            "type" to JsonPrimitive("number"),
            "format" to JsonPrimitive(if (precision == PrimitiveType.Precision.P32) "float" else "double"),
            "minimum" to constraint?.min?.toJsonNumber(),
            "maximum" to constraint?.max?.toJsonNumber(),
        )
        is PrimitiveType.Boolean -> jsonObjectOf("type" to JsonPrimitive("boolean"))
        is PrimitiveType.Bytes -> jsonObjectOf("type" to JsonPrimitive("string"), "format" to JsonPrimitive("binary"))
    }

    /** A nullable schema also admits `null`: next to its type when it has one, or as an alternative. */
    private fun JsonObject.nullable(): JsonObject = when (val type = get("type")) {
        is JsonPrimitive -> if (type == NULL_SCHEMA.getValue("type")) this else withEntries("type" to JsonArray(listOf(type, JsonPrimitive("null"))))
        else -> if (isEmpty()) this else jsonObjectOf("oneOf" to JsonArray(listOf(this, NULL_SCHEMA)))
    }

    private fun HasMetaData.description(): String? = annotations.single(DESCRIPTION) ?: comment?.value

    /** The pattern of a Wirespec regex `/pattern/flags`, where a `/` in the pattern is escaped. */
    private fun String.toPattern(): String = substringAfter('/')
        .substringBeforeLast('/')
        .let { pattern -> Regex("\\\\.").replace(pattern) { if (it.value == "\\/") "/" else it.value } }

    private fun String.toJsonNumber(): JsonPrimitive? = toLongOrNull()?.let(::JsonPrimitive) ?: toDoubleOrNull()?.let(::JsonPrimitive)

    private fun ref(pointer: String): JsonObject = jsonObjectOf(REF to JsonPrimitive(pointer))

    private fun jsonObjectOf(vararg entries: Pair<String, JsonElement?>): JsonObject = JsonObject(entries.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())

    private fun JsonObject.withEntries(vararg entries: Pair<String, JsonElement?>): JsonObject = JsonObject(this + jsonObjectOf(*entries))

    private fun Map<String, JsonElement>.toJsonObjectOrNull(): JsonObject? = takeIf { it.isNotEmpty() }?.let(::JsonObject)
}
