package community.flock.wirespec.integration.kotest.runtime

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.RandomSource

/** Draw a single value from any [Gen] (Arb sample or Exhaustive pick). */
internal fun <T> Gen<T>.firstValue(rs: RandomSource): T = when (this) {
    is Arb -> sample(rs).value
    is Exhaustive -> values.random(rs.random)
}
