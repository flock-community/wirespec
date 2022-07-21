package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.Either.Companion.left
import community.flock.wirespec.compiler.core.Either.Companion.right
import community.flock.wirespec.compiler.core.Either.Left
import community.flock.wirespec.compiler.core.Either.Right
import community.flock.wirespec.compiler.core.exceptions.WireSpecException

sealed class Either<out A, out B> private constructor() {

    internal abstract val isLeft: Boolean
    internal abstract val isRight: Boolean

    data class Left<out A>(val value: A) : Either<A, Nothing>() {
        override val isLeft = true
        override val isRight = false
    }

    data class Right<out B>(val value: B) : Either<Nothing, B>() {
        override val isLeft = false
        override val isRight = true
    }

    companion object {
        fun <A> A.left() = Left(this)
        fun <B> B.right() = Right(this)
    }

    inline fun <C> map(f: (B) -> C): Either<A, C> = flatMap { Right(f(it)) }

    inline fun <C> fold(ifLeft: (A) -> C, ifRight: (B) -> C): C = when (this) {
        is Right -> ifRight(value)
        is Left -> ifLeft(value)
    }

}

inline fun <A, B, C> Either<A, B>.flatMap(f: (B) -> Either<A, C>): Either<A, C> = when (this) {
    is Right -> f(value)
    is Left -> this
}

fun <A, B> Either<A, B>.orNull() = when (this) {
    is Left -> null
    is Right -> value
}

fun <A, B> Either<A, B>.getOrHandle(default: (A) -> B) = when (this) {
    is Left -> default(value)
    is Right -> value
}

fun <T> either(block: () -> T): Either<WireSpecException, T> = try {
    block().right()
} catch (e: Throwable) {
    when (e) {
        is WireSpecException -> e.left()
        else -> throw e
    }
}
