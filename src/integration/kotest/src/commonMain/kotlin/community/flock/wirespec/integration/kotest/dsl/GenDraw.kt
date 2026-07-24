package community.flock.wirespec.integration.kotest.dsl

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.RandomSource

/** Draw a single value from any [Gen] (an [Arb] sample or an [Exhaustive] pick), seeded by [rs]. */
fun <T> Gen<T>.draw(rs: RandomSource): T = when (this) {
    is Arb -> sample(rs).value
    is Exhaustive -> values.random(rs.random)
}

/** Draw a single value from any [Gen] against a fresh [RandomSource]. */
fun <T> Gen<T>.draw(): T = draw(RandomSource.default())
