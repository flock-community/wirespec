package community.flock.wirespec.compiler.core.validate

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.parse.AST

fun AST.validate(): Either<WireSpecException, AST> = either { this }
