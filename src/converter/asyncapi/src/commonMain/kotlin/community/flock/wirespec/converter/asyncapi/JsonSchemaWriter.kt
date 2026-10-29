package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.StringFormat
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DEFAULT_PARAMETER
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.DESCRIPTION
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.NAME
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.OMIT
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.SCHEMA
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.fields
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.find
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.parameter
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.positional
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.single
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toAnnotationValue
import community.flock.wirespec.converter.asyncapi.AsyncAPIAnnotations.toJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type as PrimitiveType

/**
 * Writes the JSON Schema of Wirespec definitions, fields and references. A definition marked `@AsyncAPIInline` is
 * written where it is used, any other definition is referred to in `components.schemas`.
 *
 * The parser writes every schema it converts with this writer too: what the schema has that the writer does not
 * write, it keeps in `@AsyncAPISchema` ([schemaPatch]), which the writer applies on top. That makes the conversion of
 * JSON Schema lossless.
 */
internal class JsonSchemaWriter(
    private val inlineDefinition: (String) -> Model?,
    private val componentKey: (String) -> String,
) {

    fun Model.schema(): JsonElement = schema(emptySet())

    fun Model.canonical(): JsonElement = canonical(emptySet())

    fun Field.canonical(): JsonElement = canonical(emptySet())

    fun Reference.schema(): JsonElement = schema(emptySet())

    private fun Model.schema(inlining: Set<String>): JsonElement = canonical(inlining).patched(annotations)

    private fun Model.canonical(inlining: Set<String>): JsonElement = when (this) {
        is Type -> jsonObjectOf(
            "type" to JsonPrimitive("object"),
            "description" to description(),
            "properties" to shape.fields.associate { it.key() to it.schema(inlining) }.toJsonObjectOrNull(),
            "required" to shape.fields.filterNot { it.reference.isNullable }.map { JsonPrimitive(it.key()) }.takeIf { it.isNotEmpty() }?.let(::JsonArray),
        )
        is Enum -> jsonObjectOf(
            "type" to JsonPrimitive("string"),
            "description" to description(),
            "enum" to JsonArray(entries.map(::JsonPrimitive)),
        )
        is Union -> jsonObjectOf(
            "description" to description(),
            "oneOf" to JsonArray(entries.map { it.schema(inlining) }),
        )
        is Refined -> reference.type.schema().withEntries("description" to description())
    }

    private fun Field.schema(inlining: Set<String>): JsonElement = canonical(inlining).patched(annotations)

    private fun Field.canonical(inlining: Set<String>): JsonElement = reference.copy(isNullable = false).schema(inlining).withEntries(
        "description" to annotations.single(DESCRIPTION)?.let(::JsonPrimitive),
        "default" to defaultValue?.toJson(),
    )

    // A definition is not written inside itself: a recursive inline definition refers to itself instead.
    private fun Reference.schema(inlining: Set<String>): JsonElement = when (this) {
        is Reference.Custom -> inlineDefinition(value)?.takeIf { value !in inlining }?.schema(inlining + value)
            ?: ref("#/components/schemas/${componentKey(value).escapePointer()}")
        is Reference.Primitive -> type.schema()
        is Reference.Iterable -> jsonObjectOf("type" to JsonPrimitive("array"), "items" to reference.schema(inlining))
        is Reference.Dict -> jsonObjectOf("type" to JsonPrimitive("object"), "additionalProperties" to reference.schema(inlining))
        is Reference.Any -> JsonObject(emptyMap())
        is Reference.Unit -> NULL_SCHEMA
    }.let { if (isNullable) it.nullable() else it }

    private fun PrimitiveType.schema(): JsonObject = when (this) {
        is PrimitiveType.String -> constraint?.let(StringFormat::of)
            ?.let { format -> jsonObjectOf("type" to JsonPrimitive("string"), "format" to JsonPrimitive(format.label)) }
            ?: jsonObjectOf(
                "type" to JsonPrimitive("string"),
                "pattern" to constraint?.value?.toPattern()?.let(::JsonPrimitive),
            )
        is PrimitiveType.Integer -> jsonObjectOf(
            "type" to JsonPrimitive("integer"),
            "format" to JsonPrimitive("int32").takeIf { precision == PrimitiveType.Precision.P32 },
            "minimum" to constraint?.min?.toJsonNumber(),
            "maximum" to constraint?.max?.toJsonNumber(),
        )
        is PrimitiveType.Number -> jsonObjectOf(
            "type" to JsonPrimitive("number"),
            "format" to JsonPrimitive("float").takeIf { precision == PrimitiveType.Precision.P32 },
            "minimum" to constraint?.min?.toJsonNumber(),
            "maximum" to constraint?.max?.toJsonNumber(),
        )
        is PrimitiveType.Boolean -> jsonObjectOf("type" to JsonPrimitive("boolean"))
        is PrimitiveType.Bytes -> jsonObjectOf("type" to JsonPrimitive("string"), "format" to JsonPrimitive("binary"))
    }

    private fun Model.description(): JsonPrimitive? = (annotations.single(DESCRIPTION) ?: comment?.value)?.let(::JsonPrimitive)

    private fun Field.key(): String = annotations.single(NAME) ?: identifier.value

    private fun JsonElement.patched(annotations: List<Annotation>): JsonElement = annotations.find(SCHEMA)?.let { patch ->
        when (val replacement = patch.positional()?.toJson()) {
            null, is JsonObject -> JsonObject(
                (this as? JsonObject).orEmpty() + (replacement as? JsonObject).orEmpty() + (patch.fields() - OMIT) - patch.omitted().toSet(),
            )
            else -> replacement
        }
    } ?: this

    private fun Annotation.omitted(): List<String> = (parameters.find { it.name == OMIT }?.value?.toJson() as? JsonArray)
        .orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.content }

    private fun DefaultValue.toJson(): JsonElement = when (this) {
        is DefaultValue.StringValue -> JsonPrimitive(value)
        is DefaultValue.IntegerValue -> value.toJsonNumber()
        is DefaultValue.NumberValue -> value.toJsonNumber()
        is DefaultValue.BooleanValue -> JsonPrimitive(value)
        is DefaultValue.EnumValue -> JsonPrimitive(value)
        is DefaultValue.NullValue -> JsonNull
    }

    /** A schema that also admits `null`: `null` next to its type when it has one, or `null` as an alternative. */
    private fun JsonElement.nullable(): JsonElement = when {
        this !is JsonObject || isEmpty() -> this
        else -> when (val type = get("type")) {
            is JsonArray -> if (NULL_TYPE in type) this else JsonObject(this + ("type" to JsonArray(type + NULL_TYPE)))
            is JsonPrimitive -> if (type == NULL_TYPE) this else JsonObject(this + ("type" to JsonArray(listOf(type, NULL_TYPE))))
            else -> jsonObjectOf("oneOf" to JsonArray(listOf(this, NULL_SCHEMA)))
        }
    }

    /** The pattern of a Wirespec regex `/pattern/flags`, where a `/` in the pattern is escaped. */
    private fun String.toPattern(): String = substringAfter('/')
        .substringBeforeLast('/')
        .let { pattern -> Regex("\\\\.").replace(pattern) { if (it.value == "\\/") "/" else it.value } }

    private companion object {
        val NULL_TYPE = JsonPrimitive("null")
        val NULL_SCHEMA = JsonObject(mapOf("type" to NULL_TYPE))
    }
}

/**
 * What [original] has that the writer does not write itself, the [canonical] schema, as `@AsyncAPISchema`: the keywords
 * to set as parameters and the keywords to leave out in `omit`. A keyword called `default` or `omit` is set in the
 * positional dictionary instead, and a schema that is not an object, such as `true`, is the positional value itself.
 */
internal fun schemaPatch(canonical: JsonElement, original: JsonElement): Annotation? = when {
    canonical == original -> null
    canonical is JsonObject && original is JsonObject -> {
        val (positional, named) = original.entries.filter { (key, value) -> canonical[key] != value }.partition { it.key == DEFAULT_PARAMETER || it.key == OMIT }
        val omitted = canonical.keys - original.keys
        Annotation(
            SCHEMA,
            listOfNotNull(positional.takeIf { it.isNotEmpty() }?.let { entries -> Annotation.Parameter(DEFAULT_PARAMETER, JsonObject(entries.associate { it.toPair() }).toAnnotationValue()) }) +
                named.map { (key, value) -> parameter(key, value.toAnnotationValue()) } +
                listOfNotNull(omitted.takeIf { it.isNotEmpty() }?.let { keys -> parameter(OMIT, JsonArray(keys.map(::JsonPrimitive)).toAnnotationValue()) }),
        )
    }
    else -> Annotation(SCHEMA, listOf(Annotation.Parameter(DEFAULT_PARAMETER, original.toAnnotationValue())))
}

internal fun jsonObjectOf(vararg entries: Pair<String, JsonElement?>): JsonObject = JsonObject(entries.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())

/** Sets the entries that have a value, in the place of an entry with the same key. */
internal fun JsonElement.withEntries(vararg entries: Pair<String, JsonElement?>): JsonElement = (this as? JsonObject)
    ?.let { JsonObject(it + jsonObjectOf(*entries)) }
    ?: this

internal fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())

internal fun Map<String, JsonElement>.toJsonObjectOrNull(): JsonObject? = takeIf { it.isNotEmpty() }?.let(::JsonObject)

internal fun ref(pointer: String): JsonObject = jsonObjectOf("\$ref" to JsonPrimitive(pointer))

internal fun String.escapePointer(): String = replace("~", "~0").replace("/", "~1")

internal fun String.unescapePointer(): String = replace("~1", "/").replace("~0", "~")

/** The number as written, so a default or bound keeps its exact spelling on every platform. */
private fun String.toJsonNumber(): JsonElement = runCatching { Json.parseToJsonElement(this) }.getOrNull() as? JsonPrimitive ?: JsonPrimitive(this)
