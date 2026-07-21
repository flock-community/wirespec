package community.flock.wirespec.integration.kotest.dsl

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.RandomSource

/**
 * Draw a single value from any [Gen] (an [Arb] sample or an [Exhaustive] pick),
 * seeded by [rs]. Used both internally and by the generated `*Dsl` body transforms
 * (which live in a downstream module) to reconstruct the request body from per-field
 * override `Gen`s.
 */
fun <T> Gen<T>.draw(rs: RandomSource): T = when (this) {
    is Arb -> sample(rs).value
    is Exhaustive -> values.random(rs.random)
}

/**
 * Draw a single value from any [Gen] against a fresh [RandomSource]. Convenience for pulling one
 * value out of a `Gen<…>` returned by the DSL (`TodoDto.generate { }.draw()`,
 * `endpoint.generate.response200 { }.draw()`) when a full property run isn't needed.
 */
fun <T> Gen<T>.draw(): T = draw(RandomSource.default())
