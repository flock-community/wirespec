package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import kotlin.test.assertEquals

fun assertRight(expected:String, either: Either<out WireSpecException.CompilerException, String>) {
    when(either){
        is Either.Left -> error("Left expected")
        is Either.Right -> assertEquals(expected, either.value)
    }
}

fun assertLeft(expected:String, either: Either<out WireSpecException.CompilerException, String>) {
    when(either){
        is Either.Left -> assertEquals(expected, either.value.message)
        is Either.Right -> error("Left expected")
    }
}