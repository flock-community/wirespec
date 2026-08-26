package community.flock.wirespec.integration.kotest.dsl

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.RandomSource

fun <T> Gen<T>.draw(rs: RandomSource): T = when (this) {
    is Arb -> sample(rs).value
    is Exhaustive -> values.random(rs.random)
}

fun <T> Gen<T>.draw(): T = draw(RandomSource.default())
