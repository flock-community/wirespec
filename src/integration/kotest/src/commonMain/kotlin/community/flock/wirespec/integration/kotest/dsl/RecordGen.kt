package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.KotestWirespecGeneratorBuilder
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arbitrary

/**
 * A [Gen] that materialises a random record instance of [T] via the IR-emitted `<T>Generator`,
 * optionally applying per-field [overrides]. Backs the generated `<Type>.generate { … }` entry point.
 * Each draw builds a fresh instance seeded by the sample's [io.kotest.property.RandomSource].
 */
inline fun <reified T : Any> recordGen(
    noinline overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null,
): Gen<T> = recordGen(T::class.java, overrides)

fun <T : Any> recordGen(
    modelClass: Class<T>,
    overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?,
): Arb<T> = arbitrary { rs ->
    val receiver = ArbReceiver(rs)
    val generator = overrides
        ?.let { kotestWirespecKotlinGenerator(seed = rs.random.nextLong(), block = it) }
        ?: receiver.generator
    @Suppress("UNCHECKED_CAST")
    receiver.generatorFor(modelClass).generate(generator, emptyList()) as T
}
