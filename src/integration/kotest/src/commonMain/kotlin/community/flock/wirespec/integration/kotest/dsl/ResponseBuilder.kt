package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.Gen
import kotlin.reflect.KClass

public fun responseCall(
    endpointObject: Wirespec.Endpoint,
    variantClass: KClass<*>,
): ResponseBuilder = ResponseBuilder(endpointObject, variantClass)

@WirespecScenarioDsl
public class ResponseBuilder internal constructor(
    internal val endpointObject: Wirespec.Endpoint,
    internal val variantClass: KClass<*>,
) {

    internal var bodyGen: Gen<*>? = null

    internal val headerGens: MutableMap<String, Gen<*>> = mutableMapOf()

    public fun body(gen: Gen<*>): ResponseBuilder = apply { bodyGen = gen }

    public fun headerGen(name: String, gen: Gen<*>): ResponseBuilder = apply { headerGens[name] = gen }

    public fun buildGen(): Arb<Any> = CallExecutor.buildResponseGen(this)
}
