package community.flock.wirespec.ir.generator

/** Joins to string only if non-empty; returns empty string otherwise. */
internal inline fun <T> Iterable<T>.joinNonEmpty(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    noinline transform: ((T) -> CharSequence)? = null,
): String = if (none()) "" else joinToString(separator, prefix, postfix, transform = transform)

/** Wraps the string with a prefix when it is not empty. */
internal fun String.prefixIfNotEmpty(prefix: String): String = if (isEmpty()) this else "$prefix$this"

/** Wraps the string with a prefix and suffix when it is not empty. */
internal fun String.wrapIfNotEmpty(prefix: String, suffix: String): String = if (isEmpty()) this else "$prefix$this$suffix"

/** Indents every non-empty line by `level * width` spaces. */
internal fun String.indentLines(level: Int, width: Int = 2): String {
    if (level <= 0) return this
    val pad = " ".repeat(level * width)
    return lines().joinToString("\n") { if (it.isEmpty()) it else "$pad$it" }
}

/** Removes empty lines and ensures the result ends with a newline. */
internal fun String.compact(): String = lines().filter { it.isNotEmpty() }.joinToString("\n", postfix = "\n")
