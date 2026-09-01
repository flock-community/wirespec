package community.flock.wirespec.integration.kotest.dsl

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.RandomSource

public fun <T> Gen<T>.draw(rs: RandomSource = RandomSource.default()): T = when (this) {
    is Arb -> sample(rs).value
    is Exhaustive -> values.random(rs.random)
}
