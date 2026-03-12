package community.flock.wirespec.compiler.test

import arrow.core.Either
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException

interface Fixture {
    val source: String
    val compiler: (() -> Emitter) -> Either<NonEmptyList<WirespecException>, String>
}
