package community.flock.wirespec.openapi.v3

import community.flock.wirespec.openapi.common.jsonDefault
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Translates an OpenAPI 3.1 / 3.2 document into a 3.0-equivalent JSON form so it can
 * be parsed by the OpenAPI 3.0 model. The translation handles JSON Schema 2020-12
 * nullability — `type: ["string", "null"]` (or `type: "null"`) — by collapsing the
 * type array to its non-null member(s) and setting `nullable: true`.
 *
 * Returns the original input untouched for 3.0 documents.
 */
internal object OpenAPIV3Normalizer {

    fun normalize(jsonString: String): String {
        val json = jsonDefault(strict = false)
        val root = json.decodeFromString(JsonObject.serializer(), jsonString)
        val openapi = root["openapi"]?.jsonPrimitive?.contentOrNull
        if (openapi == null || !needsNormalization(openapi)) return jsonString
        val normalized = buildJsonObject {
            root.forEach { (key, value) ->
                when (key) {
                    "openapi" -> put("openapi", JsonPrimitive("3.0.0"))
                    else -> put(key, normalizeElement(value))
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), normalized)
    }

    private fun needsNormalization(openapi: String): Boolean = openapi.startsWith("3.1") || openapi.startsWith("3.2")

    private fun normalizeElement(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> normalizeObject(element)
        is JsonArray -> JsonArray(element.map { normalizeElement(it) })
        is JsonPrimitive -> element
    }

    private fun normalizeObject(obj: JsonObject): JsonObject {
        // If this object has a `type` field that's an array (3.1/3.2 type-as-array),
        // collapse it to a single 3.0-compatible type plus an explicit `nullable` flag.
        val original = obj
        val typeField = original["type"]
        val (collapsedType, becomesNullable) = collapseType(typeField)
        return buildJsonObject {
            original.forEach { (key, value) ->
                when {
                    key == "type" -> if (collapsedType != null) put("type", collapsedType)
                    // V30 schema expects `exclusiveMinimum`/`exclusiveMaximum` as Boolean;
                    // 3.1/3.2 redefined them as numeric bounds. Strip numeric values so
                    // the V30 decoder does not fail on a type mismatch.
                    (key == "exclusiveMinimum" || key == "exclusiveMaximum") &&
                        value is JsonPrimitive &&
                        value.booleanOrNull == null -> Unit
                    else -> put(key, normalizeElement(value))
                }
            }
            if (becomesNullable) {
                val existing = original["nullable"]?.jsonPrimitive?.booleanOrNull
                if (existing != true) put("nullable", JsonPrimitive(true))
            }
        }
    }

    /**
     * Returns the collapsed `type` value plus whether the original encoded a `null` member.
     * - `null`               → (null, false)         no change
     * - `"string"`           → ("string", false)     no change
     * - `"null"`             → (null, true)          drop type, mark nullable
     * - `["string", "null"]` → ("string", true)      keep non-null, mark nullable
     * - `["string"]`         → ("string", false)     unwrap single-element array
     */
    private fun collapseType(typeField: JsonElement?): Pair<JsonElement?, Boolean> = when (typeField) {
        null -> null to false
        is JsonArray -> {
            val (nulls, nonNulls) = typeField.partition { it is JsonPrimitive && it.contentOrNull == "null" }
            val becomesNullable = nulls.isNotEmpty()
            val collapsed: JsonElement? = when (nonNulls.size) {
                0 -> null
                1 -> nonNulls.single()
                else -> JsonArray(nonNulls)
            }
            collapsed to becomesNullable
        }

        is JsonPrimitive -> if (typeField.contentOrNull == "null") {
            null to true
        } else {
            typeField to false
        }

        is JsonObject -> typeField to false
    }
}
