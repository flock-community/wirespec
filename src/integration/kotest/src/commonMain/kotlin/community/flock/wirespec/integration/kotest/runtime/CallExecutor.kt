package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.dsl.ArbReceiver
import community.flock.wirespec.integration.kotest.dsl.EndpointCallBuilder
import community.flock.wirespec.integration.kotest.dsl.ResponseBuilder
import community.flock.wirespec.integration.kotest.dsl.draw
import community.flock.wirespec.integration.kotest.extension.currentEndpointContext
import community.flock.wirespec.integration.kotest.validation.ContractValidator
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int

/** Executes a single endpoint call eagerly: slot resolution, typed transport, contract validation. */
internal object CallExecutor {

    fun buildRequestGen(call: EndpointCallBuilder<*, *, *>): Arb<Any> = arbitrary { rs -> buildRequestWith(call, rs) }

    private fun buildRequestWith(call: EndpointCallBuilder<*, *, *>, rs: RandomSource): Any {
        val arb = ArbReceiver(rs)
        val reflection = call.reflection
        return reflection.buildRequest(resolveSlots(call, reflection, rs, arb))
    }

    /** Send a pre-built typed request as-is; returns the response validated against the contract. */
    suspend fun executeRequest(
        client: Wirespec.Client<*, *>,
        endpointObject: Wirespec.Endpoint,
        request: Any,
    ): Any = transportAndValidate(client, EndpointReflection.of(endpointObject), request)

    /** Typed transport of one request through the installed endpoint context, then contract validation. */
    private suspend fun transportAndValidate(
        client: Wirespec.Client<*, *>,
        reflection: EndpointReflection,
        request: Any,
    ): Any {
        val ctx = currentEndpointContext()

        @Suppress("UNCHECKED_CAST")
        val starClient = client as Wirespec.Client<Wirespec.Request<Any>, Wirespec.Response<*>>
        val clientEdge = starClient.client(ctx.serialization)

        @Suppress("UNCHECKED_CAST")
        val rawRequest = clientEdge.to(request as Wirespec.Request<Any>)
        val rawResponse = ctx.transportation.transport(rawRequest)

        val validator = ContractValidator(reflection, ctx.serialization)
        return try {
            validator.validate(rawResponse)
        } catch (t: Throwable) {
            throw AssertionError("${reflection.endpointName} failed (wirespec seed=${currentSeed()}): ${t.message}", t)
        }
    }

    fun buildResponseGen(builder: ResponseBuilder): Arb<Any> = arbitrary { rs -> buildResponseWith(builder, rs) }

    private fun buildResponseWith(builder: ResponseBuilder, rs: RandomSource): Any {
        val arb = ArbReceiver(rs)
        val reflection = EndpointReflection.of(builder.endpointObject)
        val variant = reflection.responseVariant(builder.variantClass.java)

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

    private fun resolveResponseBody(
        builder: ResponseBuilder,
        variant: EndpointReflection.ResponseVariantReflection,
        arb: ArbReceiver,
        rs: RandomSource,
    ): Any? = when (val bodyGen = builder.bodyGen) {
        null -> resolveGeneratedBody(variant, arb, rs)
        else -> bodyGen.draw(rs)
    }

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

    private fun defaultValueFor(type: Class<*>, arb: ArbReceiver, rs: RandomSource): Any? = PrimitiveArbs.forTypeOrNull(type)?.draw(rs)
        ?: arb.generatorFor(type).generate(arb.generator, emptyList())

    private fun resolveSlots(
        call: EndpointCallBuilder<*, *, *>,
        reflection: EndpointReflection,
        rs: RandomSource,
        arb: ArbReceiver,
    ): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>()

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

        val slotGens: Map<String, Gen<*>> = call.pathGens + call.queryGens + call.headerGens
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

    private fun normalizeSlotName(name: String): String = name.replace("-", "").replace("_", "").lowercase()
}
