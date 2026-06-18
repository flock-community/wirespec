package community.flock.wirespec.integration.kotest.extension

/**
 * Renders Wirespec field/parameter names as Kotlin identifiers. Wirespec allows names that
 * are not valid Kotlin identifiers (e.g. the header `Refresh-Token`) or that collide with
 * hard keywords (`object`, `is`, …); those are wrapped in backticks so the generated DSL
 * compiles. Wire names used as string literals (the runtime registration keys) keep their
 * raw form — only Kotlin-level declarations and references are escaped.
 */
internal object KotlinIdentifier {

    private val valid = Regex("[A-Za-z_][A-Za-z0-9_]*")

    // Hard keywords that match the identifier pattern and therefore must be backtick-escaped
    // when used as a name. Soft/modifier keywords are contextual and need no escaping.
    private val hardKeywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
        "true", "try", "typealias", "typeof", "val", "var", "when", "while",
    )

    /** Escapes [name] for use as a Kotlin identifier, backticking when it isn't already valid. */
    fun escape(name: String): String = if (valid.matches(name) && name !in hardKeywords) name else "`$name`"
}
