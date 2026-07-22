package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.dsl.ArbReceiver
import community.flock.wirespec.integration.kotest.dsl.EndpointCallBuilder
import community.flock.wirespec.integration.kotest.dsl.ResponseBuilder
import community.flock.wirespec.integration.kotest.dsl.draw
import community.flock.wirespec.integration.kotest.validation.ContractValidator
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

/**
 * Executes a single endpoint call eagerly against the ambient context: slot resolution → typed
 * transport → contract validation → user assertion. The ambient [RandomSource] advances on every
 * call, so repeated same-endpoint calls draw distinct bodies.
 */
internal object CallExecutor {

    /** Build the typed request from the call's slot/body gens without sending it. */
    suspend fun buildRequest(call: EndpointCallBuilder<*, *, *>): Any = buildRequestWith(call, currentAmbient().randomSource)

    fun buildRequestGen(call: EndpointCallBuilder<*, *, *>): Arb<Any> = arbitrary { rs -> buildRequestWith(call, rs) }

    private fun buildRequestWith(call: EndpointCallBuilder<*, *, *>, rs: RandomSource): Any {
        val arb = ArbReceiver(rs)
        val reflection = call.reflection
        return reflection.buildRequest(resolveSlots(call, reflection, rs, arb))
    }

    /** Run the endpoint call and return the validated typed response. */
    suspend fun executeEndpoint(call: EndpointCallBuilder<*, *, *>): Any {
        val ambient = currentAmbient()
        val reflection = call.reflection
        val request = buildRequestWith(call, ambient.randomSource)
        val typedResponse = transportAndValidate(call.client, reflection, request, call.expectedStatuses, ambient)
        call.customAssertion?.let { assertion ->
            try {
                assertion.invoke(request, typedResponse)
            } catch (t: Throwable) {
                throw AssertionError(
                    "${reflection.endpointName} assertion failed (wirespec seed=${ambient.seed}): ${t.message}",
                    t,
                )
            }
        }
        return typedResponse
    }

    /** Send a pre-built typed request as-is; returns the response validated against any declared status. */
    suspend fun executeRequest(
        client: Wirespec.Client<*, *>,
        endpointObject: Wirespec.Endpoint,
        request: Any,
    ): Any = transportAndValidate(client, EndpointReflection.of(endpointObject), request, expectedStatuses = null, ambient = currentAmbient())

    /** Typed transport of one request through the ambient context, then contract validation. */
    private suspend fun transportAndValidate(
        client: Wirespec.Client<*, *>,
        reflection: EndpointReflection,
        request: Any,
        expectedStatuses: Set<Int>?,
        ambient: WirespecAmbient,
    ): Any {
        val ctx = ambient.endpointContext()

        @Suppress("UNCHECKED_CAST")
        val starClient = client as Wirespec.Client<Wirespec.Request<Any>, Wirespec.Response<*>>
        val clientEdge = starClient.client(ctx.serialization)

        @Suppress("UNCHECKED_CAST")
        val rawRequest = clientEdge.to(request as Wirespec.Request<Any>)
        val rawResponse = ctx.transportation.transport(rawRequest)

        val validator = ContractValidator(reflection, ctx.serialization)
        return try {
            validator.validate(rawResponse, expectedStatuses = expectedStatuses)
        } catch (t: Throwable) {
            throw AssertionError("${reflection.endpointName} failed (wirespec seed=${ambient.seed}): ${t.message}", t)
        }
    }

    fun buildResponseGen(builder: ResponseBuilder): Arb<Any> = arbitrary { rs -> buildResponseWith(builder, rs) }

    /**
     * Build a single random `Response<status>` variant: pinned gens win, every other constructor
     * param (body + header fields) is generated against the ambient [RandomSource].
     */
    private fun buildResponseWith(builder: ResponseBuilder, rs: RandomSource): Any {
        val arb = ArbReceiver(rs)
        val reflection = EndpointReflection.of(builder.endpointObject)
        val variant = reflection.responseVariant(builder.variantClass.java)

        // Header gens registered under wire names; match to (camelCase) constructor params by the
        // same normalization used for request slots. Exact-name matches take priority.
        val headerGensByNormalizedName: Map<String, Gen<*>> =
            builder.headerGens.mapKeys { (key, _) -> normalizeSlotName(key) }

        val args = variant.constructor.parameters.map { param ->
            val name = param.name ?: error("${builder.variantClass.simpleName}: unnamed constructor parameter.")
            if (name == "body") {
                resolveResponseBody(builder, variant, arb, rs)
            } else {
                val gen = builder.headerGens[name] ?: headerGensByNormalizedName[normalizeSlotName(name)]
                gen?.draw(rs) ?: defaultValueFor(param.type, arb, rs)
            }
        }
        return try {
            variant.constructor.newInstance(*args.toTypedArray())
        } catch (t: Throwable) {
            error("Failed to build ${builder.variantClass.simpleName} with args=$args: ${t.cause?.message ?: t.message}")
        }
    }

    /** Resolve the response body: a whole-value override wins, else a generated object/list. */
    private fun resolveResponseBody(
        builder: ResponseBuilder,
        variant: EndpointReflection.ResponseVariantReflection,
        arb: ArbReceiver,
        rs: RandomSource,
    ): Any? = when (val bodyGen = builder.bodyGen) {
        // whole-value override wins; a nullable body may legitimately draw `null`, so branch on the
        // gen itself rather than `?:`-falling-through on a null draw.
        null -> resolveGeneratedBody(variant, arb, rs)
        else -> bodyGen.draw(rs)
    }

    /** Generate the response body from reflection when no whole-value override is pinned. */
    private fun resolveGeneratedBody(
        variant: EndpointReflection.ResponseVariantReflection,
        arb: ArbReceiver,
        rs: RandomSource,
    ): Any? = when {
        variant.bodyElementClass != null -> {
            val size = Arb.int(1..3).draw(rs)
            val elementGen = arb.generatorFor(variant.bodyElementClass)
            (0 until size).map { i -> elementGen.generate(arb.generator, listOf("$i")) }
        }
        variant.bodyClass != null -> arb.generatorFor(variant.bodyClass).generate(arb.generator, emptyList())
        else -> error("${variant.constructor.declaringClass.simpleName}: `body` param has no resolvable type.")
    }

    /** Default value for a non-body response constructor param: primitive Arb or a generated model. */
    private fun defaultValueFor(type: Class<*>, arb: ArbReceiver, rs: RandomSource): Any? = PrimitiveArbs.forTypeOrNull(type)?.draw(rs)
        ?: arb.generatorFor(type).generate(arb.generator, emptyList())

    private fun resolveSlots(
        call: EndpointCallBuilder<*, *, *>,
        reflection: EndpointReflection,
        rs: RandomSource,
        arb: ArbReceiver,
    ): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>()

        // The body is always drawn from the contract default first; the generated DSL's
        // `bodyTransform` (if any) then reconstructs it with the per-field overrides
        // applied (`base.copy(field = gen.draw(rs))`). Un-overridden fields keep the
        // generator's default value.
        fun withBodyTransform(default: Any): Any = call.bodyTransform?.invoke(default, rs) ?: default

        when {
            reflection.hasBody && reflection.bodyElementClass != null -> {
                val size = (call.bodyListSizeGen ?: Arb.int(1..3)).draw(rs)
                val elementGen = arb.generatorFor(reflection.bodyElementClass)
                val default = (0 until size).map { i -> elementGen.generate(arb.generator, listOf("$i")) }
                args["body"] = withBodyTransform(default)
            }
            reflection.hasBody -> {
                val bodyType = reflection.requestConstructor.parameters
                    .firstOrNull { it.name == "body" }?.type
                    ?: error("${reflection.endpointName}: hasBody=true but no `body` constructor param.")
                args["body"] = withBodyTransform(arb.generatorFor(bodyType).generate(arb.generator, emptyList()))
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
            slotGens.mapKeys { (key, _) -> normalizeSlotName(key) }
        reflection.requestConstructor.parameters.forEach { param ->
            val name = param.name ?: return@forEach
            if (name == "body" || args.containsKey(name)) return@forEach
            val gen = slotGens[name] ?: slotGensByNormalizedName[normalizeSlotName(name)]
            args[name] = (gen ?: PrimitiveArbs.forType(param.type)).draw(rs)
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
