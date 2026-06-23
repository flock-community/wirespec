package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.dsl.ArbReceiver
import community.flock.wirespec.integration.kotest.dsl.EndpointCallBuilder
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator
import community.flock.wirespec.integration.kotest.validation.ContractValidator
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.int

/**
 * Executes a single endpoint call eagerly against the ambient context: slot
 * resolution → typed transport → contract validation → user assertion. Reads the
 * context from [currentAmbient]. Each call constructs a fresh [ArbReceiver] from the
 * ambient [RandomSource]; because the source advances on every call, repeated
 * same-endpoint calls draw distinct bodies.
 */
internal object CallExecutor {

    /** Run the endpoint call; returns the validated typed response. */
    suspend fun executeEndpoint(call: EndpointCallBuilder<*, *, *>): Any {
        val ambient = currentAmbient()
        val ctx = ambient.endpointContext()
        val rs = ambient.randomSource
        val arb = ArbReceiver(rs)
        val reflection = call.reflection
        val request = reflection.buildRequest(resolveSlots(call, reflection, rs, arb))

        @Suppress("UNCHECKED_CAST")
        val starClient = call.client as Wirespec.Client<Wirespec.Request<Any>, Wirespec.Response<*>>
        val clientEdge = starClient.client(ctx.serialization)

        @Suppress("UNCHECKED_CAST")
        val rawRequest = clientEdge.to(request as Wirespec.Request<Any>)
        val rawResponse = ctx.transportation.transport(rawRequest)

        val validator = ContractValidator(reflection, ctx.serialization)
        val typedResponse = try {
            validator.validate(rawResponse, expectedStatuses = call.expectedStatuses)
        } catch (t: Throwable) {
            throw AssertionError("${reflection.endpointName} failed (wirespec seed=${ambient.seed}): ${t.message}", t)
        }
        call.customAssertion?.let { assertion ->
            try {
                assertion.invoke(typedResponse)
            } catch (t: Throwable) {
                throw AssertionError(
                    "${reflection.endpointName} assertion failed (wirespec seed=${ambient.seed}): ${t.message}",
                    t,
                )
            }
        }
        return typedResponse
    }

    private fun resolveSlots(
        call: EndpointCallBuilder<*, *, *>,
        reflection: EndpointReflection,
        rs: RandomSource,
        arb: ArbReceiver,
    ): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>()

        // A per-call generator honoring any `bodyFields { }` overrides, else the
        // receiver's default. Built lazily so non-body endpoints pay nothing.
        fun bodyGenerator(): Wirespec.Generator = call.bodyFieldOverrides
            ?.let { overrides -> kotestWirespecKotlinGenerator(seed = rs.random.nextLong()) { overrides() } }
            ?: arb.generator

        when {
            reflection.hasBody && reflection.bodyElementClass != null -> {
                val size = (call.bodyListSizeGen ?: Arb.int(1..3)).firstValue(rs)
                val elementGen = arb.generatorFor(reflection.bodyElementClass)
                args["body"] = (0 until size).map { i -> elementGen.generate(bodyGenerator(), listOf("$i")) }
            }
            reflection.hasBody -> {
                val bodyType = reflection.requestConstructor.parameters
                    .firstOrNull { it.name == "body" }?.type
                    ?: error("${reflection.endpointName}: hasBody=true but no `body` constructor param.")
                args["body"] = arb.generatorFor(bodyType).generate(bodyGenerator(), emptyList())
            }
        }

        // Fill every flattened path/query/header field. The request constructor's
        // params are the source of truth (real names + primitive types). Field names
        // are unique across path/query/header (all flattened into one constructor),
        // so the three gen maps merge without collision.
        val slotGens: Map<String, Gen<*>> = call.pathGens + call.queryGens + call.headerGens
        // The generated DSL registers gens under their *wire* names (`supplier-id`,
        // `X-Namespace`), but the Request constructor params are the identifier-safe
        // camelCase forms (`supplierId`, `xNamespace`). Match them by a normalization
        // that strips separators and case, so a pinned multi-word slot reaches the
        // request instead of being overwritten by a random PrimitiveArbs draw.
        // Exact-name matches still take priority.
        val slotGensByNormalizedName: Map<String, Gen<*>> =
            slotGens.entries.associate { (key, gen) -> normalizeSlotName(key) to gen }
        reflection.requestConstructor.parameters.forEach { param ->
            val name = param.name ?: return@forEach
            if (name == "body" || args.containsKey(name)) return@forEach
            val gen = slotGens[name] ?: slotGensByNormalizedName[normalizeSlotName(name)]
            args[name] = (gen ?: PrimitiveArbs.forType(param.type)).firstValue(rs)
        }
        return args
    }

    /**
     * Normalize a path/query/header slot name for matching: strip the `-`/`_`
     * separators used in wire names and lowercase, so `supplier-id`, `supplier_id`
     * and the constructor's `supplierId` all collapse to the same key.
     */
    private fun normalizeSlotName(name: String): String = name.replace("-", "").replace("_", "").lowercase()
}
