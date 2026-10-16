package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.parse.ast.Annotation

/**
 * The annotations that carry what a Wirespec channel has no syntax for: where the channel lives and which operations
 * the application performs on it.
 *
 * Wirespec keeps a string annotation value exactly as written between its quotes, escape sequences included, so values
 * are stored escaped ([toLiteral]) and read back with [fromLiteral]. The AST the parser produces is then the same AST
 * the Wirespec parser produces from the emitted source.
 */
internal object AsyncAPIAnnotations {
    const val DESCRIPTION = "Description"
    const val ADDRESS = "AsyncAPIAddress"

    private const val DEFAULT_PARAMETER = "default"

    enum class Action(val keyword: String) {
        SEND("send"),
        RECEIVE("receive"),
        ;

        val annotationName: String get() = "AsyncAPI${keyword.replaceFirstChar(Char::uppercase)}"

        /** The operation id that needs no annotation value: `sendTodoUpdates` for channel `TodoUpdates`. */
        fun defaultOperationId(channel: String): String = "$keyword$channel"
    }

    fun annotation(name: String, value: String? = null): Annotation = Annotation(
        name = name,
        parameters = listOfNotNull(value?.let { Annotation.Parameter(DEFAULT_PARAMETER, Annotation.Value.Single(it.toLiteral())) }),
    )

    fun String?.toDescription(): List<Annotation> = this?.takeIf { it.isNotBlank() }?.let { listOf(annotation(DESCRIPTION, it)) }.orEmpty()

    fun List<Annotation>.named(name: String): List<Annotation> = filter { it.name == name }

    fun List<Annotation>.single(name: String): String? = firstOrNull { it.name == name }?.value()

    fun Annotation.value(): String? = (parameters.find { it.name == DEFAULT_PARAMETER }?.value as? Annotation.Value.Single)?.value?.fromLiteral()

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
