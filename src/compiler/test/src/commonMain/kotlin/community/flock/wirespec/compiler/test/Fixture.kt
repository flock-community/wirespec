package community.flock.wirespec.compiler.test

import arrow.core.Either
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException

internal typealias Compiler = (() -> Emitter) -> Either<NonEmptyList<WirespecException>, String>

public interface Fixture {
    public val source: String
    public val compiler: Compiler
}
