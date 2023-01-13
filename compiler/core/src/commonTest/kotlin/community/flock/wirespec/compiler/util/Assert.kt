package community.flock.wirespec.compiler.util

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import kotlin.test.assertEquals

fun Either<WirespecException.CompilerException, String>.assertRight(expected: String) {
    when (this) {
        is Either.Left -> error("Right expected")
        is Either.Right -> assertEquals(expected, value)
    }
}

fun <B> Either<WirespecException.CompilerException, B>.assertLeft(expected: String) {
    when (this) {
        is Either.Left -> assertEquals(expected, value.message)
        is Either.Right -> error("Left expected")
    }
}
