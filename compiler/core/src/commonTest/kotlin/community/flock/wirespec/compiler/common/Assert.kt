package community.flock.wirespec.compiler.common

import arrow.core.Either
import arrow.core.Nel
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun Either<Nel<WirespecException.CompilerException>, String>.assertValid(expected: String) {
    when (this) {
        is Either.Left -> error("Valid expected")
        is Either.Right -> assertEquals(expected, value)
    }
}

fun <B> Either<Nel<WirespecException.CompilerException>, B>.assertInvalid(expected: String) {
    when (this) {
        is Either.Left -> assertTrue(value.map { it.message }.contains(expected))
        is Either.Right -> error("Invalid expected")
    }
}
