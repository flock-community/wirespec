package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The annotations that carry what Wirespec has no syntax for, so an AsyncAPI document survives the conversion.
 *
 * An annotation holds JSON in its parameters: an object becomes a dictionary, an array of strings, numbers or booleans
 * an array, and any other array its JSON text. A string is written as it is, unless it reads as JSON itself, such as
 * `"true"` or `"1"`, in which case it is written as a JSON string literal. That keeps every value apart.
 *
 * Wirespec keeps a string annotation value exactly as written between its quotes, escape sequences included. Values
 * and keys are therefore stored escaped ([toLiteral]), so the AST the parser produces is the same AST the Wirespec
 * parser produces from the emitted source.
 */
internal object AsyncAPIAnnotations {
    const val DESCRIPTION = "Description"
    const val NAME = "AsyncAPIName"
    const val DOCUMENT = "AsyncAPIDocument"
    const val COMPONENTS = "AsyncAPIComponents"
    const val CHANNEL = "AsyncAPIChannel"
    const val MESSAGE = "AsyncAPIMessage"
    const val SCHEMA = "AsyncAPISchema"
    const val INLINE = "AsyncAPIInline"

    /** The parameter of [SCHEMA] that lists the keywords to leave out. */
    const val OMIT = "omit"

    const val DEFAULT_PARAMETER = "default"

    /** What the application does on a channel: `send` and `receive` in AsyncAPI 3, `subscribe` and `publish` in 2. */
    enum class Action(val keyword: String, val v2Keyword: String) {
        SEND("send", "subscribe"),
        RECEIVE("receive", "publish"),
        ;

        val annotationName: String get() = "AsyncAPI${keyword.replaceFirstChar(Char::uppercase)}"

        /** The id of the operation a channel gets when no annotation names one: `sendTodoUpdates`. */
        fun defaultOperationId(channel: String): String = "$keyword$channel"
    }

    fun annotation(name: String, value: String? = null, parameters: List<Annotation.Parameter> = emptyList()): Annotation = Annotation(
        name = name,
        parameters = listOfNotNull(value?.let { Annotation.Parameter(DEFAULT_PARAMETER, Annotation.Value.Single(it.toLiteral())) }) + parameters,
    )

    fun String?.toDescription(): List<Annotation> = this?.let { listOf(annotation(DESCRIPTION, it)) }.orEmpty()

    fun parameter(key: String, value: Annotation.Value): Annotation.Parameter = Annotation.Parameter(key.toLiteral(), value)

    /** A string as an annotation value. */
    fun String.toStringValue(): Annotation.Value.Single = JsonPrimitive(this).toSingle()

    fun JsonObject.toParameters(): List<Annotation.Parameter> = map { (key, value) -> parameter(key, value.toAnnotationValue()) }

    fun JsonElement.toAnnotationValue(): Annotation.Value = when (this) {
        is JsonObject -> Annotation.Value.Dict(toParameters())
        is JsonArray -> when {
            all { it is JsonPrimitive } -> Annotation.Value.Array(map { (it as JsonPrimitive).toSingle() })
            else -> Annotation.Value.Single(toString().toLiteral())
        }
        is JsonPrimitive -> toSingle()
    }

    fun Annotation.Value.toJson(): JsonElement = when (this) {
        is Annotation.Value.Single -> value.fromLiteral().let { it.asJson() ?: JsonPrimitive(it) }
        is Annotation.Value.Array -> JsonArray(value.map { it.toJson() })
        is Annotation.Value.Dict -> value.toJsonObject()
    }

    private fun List<Annotation.Parameter>.toJsonObject(): JsonObject = JsonObject(associate { it.name.fromLiteral() to it.value.toJson() })

    fun List<Annotation>.named(name: String): List<Annotation> = filter { it.name == name }

    fun List<Annotation>.find(name: String): Annotation? = firstOrNull { it.name == name }

    fun List<Annotation>.has(name: String): Boolean = any { it.name == name }

    /** The positional value of the first annotation called [name], as a string. */
    fun List<Annotation>.single(name: String): String? = find(name)?.value()

    fun Annotation.value(): String? = (positional() as? Annotation.Value.Single)?.value?.fromLiteral()

    fun Annotation.positional(): Annotation.Value? = parameters.find { it.name == DEFAULT_PARAMETER }?.value

    /** The named parameters as the JSON object they stand for; the positional parameter is no field. */
    fun Annotation.fields(): JsonObject = parameters.filterNot { it.name == DEFAULT_PARAMETER }.toJsonObject()

    private fun JsonPrimitive.toSingle(): Annotation.Value.Single = Annotation.Value.Single(
        when {
            this is JsonNull -> "null"
            isString && content.asJson() == null -> content
            else -> toString()
        }.toLiteral(),
    )

    /** The JSON value that [this] reads as, or null for a string that does not read as JSON. */
    private fun String.asJson(): JsonElement? = when {
        this == "null" -> JsonNull
        this == "true" || this == "false" -> JsonPrimitive(toBooleanStrict())
        JSON_NUMBER.matches(this) || firstOrNull() in JSON_TEXT -> runCatching { Json.parseToJsonElement(this) }.getOrNull()
        else -> null
    }

    private val JSON_NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

    private val JSON_TEXT = setOf('{', '[', '"')

    private fun String.toLiteral(): String = buildString {
        this@toLiteral.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> when {
                    char < ' ' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    else -> append(char)
                }
            }
        }
    }

    private fun String.fromLiteral(): String = buildString {
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
