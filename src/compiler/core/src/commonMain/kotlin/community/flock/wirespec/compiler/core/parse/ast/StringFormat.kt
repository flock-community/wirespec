package community.flock.wirespec.compiler.core.parse.ast

import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Constraint.RegExp

// Every pattern must mean the same in each emitter's regex engine, so stick to the common
// subset: character classes instead of `\d`, no lookaround and no flags. A `/` is escaped,
// because it would end the regex literal.
private const val HEX = "[0-9a-fA-F]"
private const val FULL_DATE = "[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])"
private const val PARTIAL_TIME = "([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?"
private const val TIME_OFFSET = "(Z|[+-]([01][0-9]|2[0-3]):[0-5][0-9])"

// java.util.regex recurses once per repetition of a group, so those stop at the limits of RFC 5321: 64 characters
// before the `@`, 253 after it, 63 per label.
private const val ATOM = "[a-zA-Z0-9!#$%&'*+\\/=?^_`{|}~-]+"
private const val LABEL = "[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"

private fun uuid(version: String, variant: String = "[89abAB]") = "$HEX{8}-$HEX{4}-$version$HEX{3}-$variant$HEX{3}-$HEX{12}"

public enum class StringFormat(public val label: String, pattern: String) {
    DATE("date", FULL_DATE),
    DATE_TIME("date-time", "${FULL_DATE}T$PARTIAL_TIME$TIME_OFFSET"),
    EMAIL("email", "$ATOM(\\.$ATOM){0,31}@($LABEL\\.){1,126}$LABEL"),
    UUID("uuid", uuid(version = HEX, variant = HEX)),
    UUID_V1("uuid-v1", uuid(version = "1")),
    UUID_V2("uuid-v2", uuid(version = "2")),
    UUID_V3("uuid-v3", uuid(version = "3")),
    UUID_V4("uuid-v4", uuid(version = "4")),
    UUID_V5("uuid-v5", uuid(version = "5")),
    UUID_V6("uuid-v6", uuid(version = "6")),
    UUID_V7("uuid-v7", uuid(version = "7")),
    UUID_V8("uuid-v8", uuid(version = "8")),
    ;

    public val regExp: RegExp = RegExp("/^$pattern$/")

    public companion object {
        public fun of(label: String): StringFormat? = entries.find { it.label == label }

        public fun of(regExp: RegExp): StringFormat? = entries.find { it.regExp == regExp }
    }
}
