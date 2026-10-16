package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment

internal interface WirespecMetaDataEmitter {

    fun Annotation.emit(): String = "@$name${parameters.takeIf { it.isNotEmpty() }?.joinToString(", ", "(", ")") { it.emit() }.orEmpty()}"

    fun Comment.emit(): String = "/* $value */"

    private fun Annotation.Parameter.emit(): String = when (name) {
        DEFAULT_PARAMETER -> value.emit()
        else -> emitEntry()
    }

    /** A named parameter or a dictionary entry. Only a parameter of the annotation itself can be positional. */
    private fun Annotation.Parameter.emitEntry(): String = "${name.emitKey()}: ${value.emit()}"

    private fun Annotation.Value.emit(): String = when (this) {
        is Annotation.Value.Single -> "\"${value.escapeQuotes()}\""
        is Annotation.Value.Array -> if (value.isEmpty()) "[ ]" else value.joinToString(", ", "[", "]") { it.emit() }
        is Annotation.Value.Dict -> value.joinToString(", ", "{ ", " }") { it.emitEntry() }
    }

    /** A key that does not read as one identifier, such as `$ref` or `x-foo.bar`, is written as a string. */
    private fun String.emitKey(): String = if (KEY.matches(this)) this else "\"${escapeQuotes()}\""

    /**
     * The parser keeps a string value exactly as written between its quotes, escapes included, so only a
     * quote that is not escaped yet needs escaping to keep the value inside its literal.
     */
    private fun String.escapeQuotes(): String = buildString {
        var index = 0
        while (index < this@escapeQuotes.length) {
            when (val char = this@escapeQuotes[index]) {
                '\\' -> append(this@escapeQuotes.getOrNull(index + 1)?.let { "\\$it" } ?: "\\\\").also { index++ }
                '"' -> append("\\\"")
                else -> append(char)
            }
            index++
        }
    }
}

private const val DEFAULT_PARAMETER = "default"

// A keyword followed by a dash, such as `type-x`, would read as the keyword.
private val KEY = Regex("(?!(?:type|enum|endpoint|channel|rpc|part)-)[a-z][a-zA-Z0-9_\\-]*|[A-Z][a-zA-Z0-9_]*")
