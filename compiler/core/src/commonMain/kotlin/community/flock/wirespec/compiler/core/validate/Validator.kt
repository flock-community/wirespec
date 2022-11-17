package community.flock.wirespec.compiler.core.validate

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException
import community.flock.wirespec.compiler.core.parse.AST

fun AST.validate(): Either<CompilerException, AST> = either { this }
