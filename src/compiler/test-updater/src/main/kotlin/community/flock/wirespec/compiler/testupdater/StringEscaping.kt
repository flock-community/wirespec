package community.flock.wirespec.compiler.testupdater

/**
 * Escapes emitter output for embedding in Kotlin raw strings (triple-quoted with trimMargin).
 *
 * Handles:
 * - `$` → `${'$'}` (dollar signs in raw strings)
 * - `"""` → `${"\"\"\""}` (triple quotes in raw strings)
 * - Each line prefixed with `|` for trimMargin()
 */
fun escapeForRawString(input: String): String {
    val placeholder = "\u0000TRIPLE_QUOTE\u0000"
    return input
        .replace("\"\"\"", placeholder)
        .replace("$", "\${'$'}")
        .replace(placeholder, "\${\"\\\"\\\"\\\"\"}")
        .lines()
        .joinToString("\n") { "            |$it" }
}
