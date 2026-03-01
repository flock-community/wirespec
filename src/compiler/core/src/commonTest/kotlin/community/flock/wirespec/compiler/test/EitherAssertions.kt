package community.flock.wirespec.compiler.test

import arrow.core.Either

fun <L, R> Either<L, R>.shouldBeRight(): R = when (this) {
    is Either.Right -> value
    is Either.Left -> throw AssertionError("Expected Either.Right but got Either.Left: $value")
}

fun <L, R> Either<L, R>.shouldBeRight(errorMessage: (L) -> String): R = when (this) {
    is Either.Right -> value
    is Either.Left -> throw AssertionError("Expected Either.Right but got Either.Left: ${errorMessage(value)}")
}

infix fun <L, R> Either<L, R>.shouldBeRight(expected: R): R = when (this) {
    is Either.Right -> {
        if (value != expected) throw AssertionError("Expected Either.Right($expected) but got Either.Right($value)")
        value
    }
    is Either.Left -> throw AssertionError("Expected Either.Right but got Either.Left: $value")
}

fun <L, R> Either<L, R>.shouldBeLeft(): L = when (this) {
    is Either.Left -> value
    is Either.Right -> throw AssertionError("Expected Either.Left but got Either.Right: $value")
}
