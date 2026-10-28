package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Constraint.RegExp

// Every pattern must mean the same in each emitter's regex engine, so stick to the common
// subset: character classes instead of `\d`, no lookaround and no flags.
private const val HEX = "[0-9a-fA-F]"
private const val DATE = "[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])"
private const val TIME = "([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.[0-9]+)?"
private const val OFFSET = "(Z|[+-]([01][0-9]|2[0-3]):[0-5][0-9])"

private fun uuid(version: String, variant: String) = "$HEX{8}-$HEX{4}-$version$HEX{3}-$variant$HEX{3}-$HEX{12}"

internal val stringFormats: Map<String, RegExp> = mapOf(
    "date" to DATE,
    "date-time" to "${DATE}T$TIME$OFFSET",
    "uuid" to uuid(version = HEX, variant = HEX),
)
    .plus((1..8).associate { "uuid-v$it" to uuid(version = "$it", variant = "[89abAB]") })
    .mapValues { (_, pattern) -> RegExp("/^$pattern$/") }
