package community.flock.wirespec.compiler.util

import arrow.core.Validated
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import kotlin.test.assertEquals

fun Validated<WirespecException.CompilerException, String>.assertValid(expected: String) {
    when (this) {
        is Validated.Invalid -> error("Valid expected")
        is Validated.Valid -> assertEquals(expected, value)
    }
}

fun <B> Validated<WirespecException.CompilerException, B>.assertInvalid(expected: String) {
    when (this) {
        is Validated.Invalid -> assertEquals(expected, value.message)
        is Validated.Valid -> error("Invalid expected")
    }
}
