package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen
import kotlin.reflect.KClass

/**
 * Build a [ResponseBuilder] for one response variant of an endpoint. Generated `*Dsl`
 * wrappers call this from their `responseNNN { … }` entry point.
 */
fun responseCall(
    endpointObject: Wirespec.Endpoint,
    variantClass: KClass<*>,
): ResponseBuilder = ResponseBuilder(endpointObject, variantClass)

/**
 * Builds a single random `Response<status>` instance for an endpoint. The body defaults to a
 * generated value (via the endpoint's model generators); each header field defaults to a
 * generated value too. Callers pin any field by registering a [Gen] — `body(...)` for the
 * whole response body, `headerGen(name, ...)` per header field. [build] resolves the rest
 * randomly against the ambient `RandomSource` and constructs the variant.
 */
@WirespecScenarioDsl
class ResponseBuilder internal constructor(
    internal val endpointObject: Wirespec.Endpoint,
    internal val variantClass: KClass<*>,
) {

    internal var bodyGen: Gen<*>? = null

    internal val headerGens: MutableMap<String, Gen<*>> = mutableMapOf()

    /** Pin the whole response body. Called by the generated `body = …` setter. */
    fun body(gen: Gen<*>): ResponseBuilder = apply { bodyGen = gen }

    /** Pin a single response header field. Called by the generated header setters. */
    fun headerGen(name: String, gen: Gen<*>): ResponseBuilder = apply { headerGens[name] = gen }

    /** Construct the random response variant. */
    suspend fun build(): Any = CallExecutor.buildResponse(this)
}
