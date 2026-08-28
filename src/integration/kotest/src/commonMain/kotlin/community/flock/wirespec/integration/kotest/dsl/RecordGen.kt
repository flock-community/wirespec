package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.generator.KotestWirespecGeneratorBuilder
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary

public inline fun <reified T : Any> recordGen(
    noinline overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null,
): Arb<T> = recordGen(T::class.java, overrides)

public fun <T : Any> recordGen(
    modelClass: Class<T>,
    overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?,
): Arb<T> = arbitrary { rs -> ArbReceiver(rs).generateModel(modelClass, overrides) }
