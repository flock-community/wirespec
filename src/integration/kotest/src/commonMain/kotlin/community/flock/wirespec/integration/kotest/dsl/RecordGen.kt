package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.generator.KotestWirespecGeneratorBuilder
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arbitrary

/** A [Gen] materialising a random record instance of [T], optionally applying per-field [overrides]. */
inline fun <reified T : Any> recordGen(
    noinline overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null,
): Gen<T> = recordGen(T::class.java, overrides)

fun <T : Any> recordGen(
    modelClass: Class<T>,
    overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?,
): Arb<T> = arbitrary { rs -> ArbReceiver(rs).generateModel(modelClass, overrides) }
