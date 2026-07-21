package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.KotestWirespecGeneratorBuilder
import community.flock.wirespec.integration.kotest.context.WirespecChannelContext
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator
import community.flock.wirespec.integration.kotest.runtime.PrimitiveArbs
import community.flock.wirespec.integration.kotest.runtime.currentAmbient
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Build a [ChannelCallBuilder] for a channel. Generated `*Dsl` wrappers call this
 * from their `message { … }` entry point, passing the channel object's [KClass] and —
 * via the reified [P] — the payload type used for (de)serialization and generation.
 */
inline fun <reified P : Any> channelCall(channelClass: KClass<*>): ChannelCallBuilder<P> = ChannelCallBuilder(channelClass, typeOf<P>(), P::class.java)

/**
 * Eager channel scenario runner. `send*` generates (or accepts) a typed payload,
 * serializes it via the ambient [WirespecChannelContext] and publishes it through
 * the broker [transport][community.flock.wirespec.integration.kotest.context.ChannelTransport].
 * Asserting on what the app published is left to the test's own broker consumer.
 *
 * The destination topic is `topic(...)` if pinned, else the channel object's simple name.
 */
@WirespecScenarioDsl
class ChannelCallBuilder<P : Any> @PublishedApi internal constructor(
    @PublishedApi internal val channelClass: KClass<*>,
    @PublishedApi internal val payloadType: KType,
    @PublishedApi internal val payloadClass: Class<*>,
) {
    private var topic: String? = null
    private var key: String? = null

    fun topic(value: String): ChannelCallBuilder<P> = apply { topic = value }
    fun key(value: String): ChannelCallBuilder<P> = apply { key = value }

    // ---- build (no publish) ----

    /**
     * Generate a random payload without publishing it. Backs the generated
     * `<Channel>.generate.message { … }` entry point; sending happens by chaining
     * the returned message's `send()`.
     */
    suspend fun buildMessage(): P = generatePayload(overrides = null)

    /** Generate a payload with per-field [overrides] without publishing it. */
    suspend fun buildMessageFields(overrides: KotestWirespecGeneratorBuilder.() -> Unit): P = generatePayload(overrides)

    /**
     * A [Gen] materialising a random payload on each draw, optionally applying per-field
     * [overrides]. Backs the generated `<Channel>.generate.message { … }`; `Gen<P>.send()`
     * draws one and publishes it. A primitive payload accepts no overrides.
     */
    fun messageGen(overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null): Gen<P> = arbitrary { rs ->
        @Suppress("UNCHECKED_CAST")
        when (val primitive = PrimitiveArbs.forTypeOrNull(payloadClass)) {
            null -> {
                val receiver = ArbReceiver(rs)
                val generator = overrides
                    ?.let { kotestWirespecKotlinGenerator(seed = rs.random.nextLong()) { it() } }
                    ?: receiver.generator
                receiver.generatorFor(payloadClass).generate(generator, emptyList()) as P
            }
            else -> {
                require(overrides == null) {
                    "${channelClass.simpleName}: per-field message { } overrides are only supported for record " +
                        "payloads, not the primitive payload ${payloadClass.simpleName}."
                }
                primitive.draw(rs) as P
            }
        }
    }

    // ---- send terminals ----

    /** Generate a random payload, publish it, and return it. */
    suspend fun send(): P = generatePayload(overrides = null).also { publish(it) }

    /** Publish a concrete [payload] as-is and return it. Backs the generated `<Channel>Message.send()`. */
    suspend fun send(payload: P): P {
        publish(payload)
        return payload
    }

    /** Draw a payload from [gen], publish it, and return it. */
    suspend fun send(gen: Gen<P>): P {
        val payload = gen.draw(currentAmbient().randomSource)
        publish(payload)
        return payload
    }

    /** Generate a payload with per-field [overrides], publish it, and return it. */
    suspend fun sendFields(overrides: KotestWirespecGeneratorBuilder.() -> Unit): P = generatePayload(overrides).also { publish(it) }

    // ---- internals ----

    private suspend fun generatePayload(overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?): P {
        val rs = currentAmbient().randomSource
        @Suppress("UNCHECKED_CAST")
        return when (val primitive = PrimitiveArbs.forTypeOrNull(payloadClass)) {
            null -> {
                val receiver = ArbReceiver(rs)
                val generator = overrides
                    ?.let { kotestWirespecKotlinGenerator(seed = rs.random.nextLong()) { it() } }
                    ?: receiver.generator
                receiver.generatorFor(payloadClass).generate(generator, emptyList()) as P
            }
            else -> {
                require(overrides == null) {
                    "${channelClass.simpleName}: per-field send { } overrides are only supported for record payloads, " +
                        "not the primitive payload ${payloadClass.simpleName}."
                }
                primitive.draw(rs) as P
            }
        }
    }

    private suspend fun publish(payload: P) {
        val ctx = currentAmbient().channelContext()
        val body = ctx.serialization.serializeBody(payload, payloadType)
        ctx.transport.publish(resolveTopic(), key, body)
    }

    private fun resolveTopic(): String = topic ?: channelClass.simpleName
        ?: error("$channelClass: cannot resolve a topic — pin one with topic(...).")
}
