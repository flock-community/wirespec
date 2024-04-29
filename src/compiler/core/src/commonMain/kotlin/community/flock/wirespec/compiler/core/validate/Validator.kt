package community.flock.wirespec.compiler.core.validate

import arrow.core.Either
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST

fun AST.validate(): Either<NonEmptyList<WirespecException>, AST> = Either.Right(this)
