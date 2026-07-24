package community.flock.wirespec.integration.kotest.runtime

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.char
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string

/** Default kotest [Arb]s for primitive path/query/header field types, keyed by reflected Java [Class]. */
internal object PrimitiveArbs {
    fun forType(type: Class<*>): Arb<*> = forTypeOrNull(type) ?: error(
        "No default generator for path/query/header field type ${type.name}. Pass this field explicitly as a Gen.",
    )

    /** The default [Arb] for [type], or `null` when [type] is not a known primitive/String. */
    fun forTypeOrNull(type: Class<*>): Arb<*>? = when (type) {
        String::class.java -> Arb.string(minSize = 1, maxSize = 20, codepoints = Codepoint.alphanumeric())
        Int::class.javaPrimitiveType, Integer::class.java -> Arb.int()
        Long::class.javaPrimitiveType, java.lang.Long::class.java -> Arb.long()
        Short::class.javaPrimitiveType, java.lang.Short::class.java -> Arb.short()
        Byte::class.javaPrimitiveType, java.lang.Byte::class.java -> Arb.byte()
        Boolean::class.javaPrimitiveType, java.lang.Boolean::class.java -> Arb.boolean()
        Double::class.javaPrimitiveType, java.lang.Double::class.java -> Arb.double()
        Float::class.javaPrimitiveType, java.lang.Float::class.java -> Arb.float()
        Char::class.javaPrimitiveType, Character::class.java -> Arb.char()
        else -> null
    }
}
