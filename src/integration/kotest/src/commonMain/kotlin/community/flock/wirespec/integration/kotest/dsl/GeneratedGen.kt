package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.generator.kotestWirespecKotlinGenerator
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary

public fun <T> generatedGen(generate: (generator: Wirespec.Generator, rs: RandomSource) -> T): Arb<T> = arbitrary { rs -> generate(kotestWirespecKotlinGenerator(seed = rs.random.nextLong()), rs) }
