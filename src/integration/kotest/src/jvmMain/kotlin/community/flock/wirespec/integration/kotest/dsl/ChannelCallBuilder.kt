package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.KotestWirespecGeneratorBuilder
import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator
import community.flock.wirespec.integration.kotest.runtime.PrimitiveArbs
import community.flock.wirespec.integration.kotest.runtime.currentAmbient
import community.flock.wirespec.integration.kotest.runtime.firstValue
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Build a [ChannelCallBuilder] for a channel. Generated `*Dsl` wrappers call this
 * from their `call { … }` entry point, passing the channel object's [KClass] and —
 * via the reified [P] — the payload type used for (de)serialization and generation.
 */
inline fun <reified P : Any> channelCall(channelClass: KClass<*>): ChannelCallBuilder<P> = ChannelCallBuilder(channelClass, typeOf<P>(), P::class.java)

/**
 * Eager channel scenario runner. `send*` generates (or accepts) a typed payload,
 * serializes it via the ambient [WirespecChannelContext] and publishes it through
 * the broker [transport][community.flock.wirespec.integration.kotest.ChannelTransport];
 * `expecting`/`collecting` receive raw bodies and deserialize them back into [P].
 *
 * The destination topic is `topic(...)` if pinned, else the context's
 * `defaultTopic`, else the channel object's simple name.
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
        if (PrimitiveArbs.supports(payloadClass)) {
            require(overrides == null) {
                "${channelClass.simpleName}: per-field message { } overrides are only supported for record " +
                    "payloads, not the primitive payload ${payloadClass.simpleName}."
            }
            PrimitiveArbs.forType(payloadClass).firstValue(rs) as P
        } else {
            val receiver = ArbReceiver(rs)
            val generator = overrides
                ?.let { kotestWirespecKotlinGenerator(seed = rs.random.nextLong()) { it() } }
                ?: receiver.generator
            receiver.generatorFor(payloadClass).generate(generator, emptyList()) as P
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
        val payload = gen.firstValue(currentAmbient().randomSource)
        publish(payload)
        return payload
    }

    /** Generate a payload with per-field [overrides], publish it, and return it. */
    suspend fun sendFields(overrides: KotestWirespecGeneratorBuilder.() -> Unit): P = generatePayload(overrides).also { publish(it) }

    // ---- receive terminals ----

    /** Receive a single payload from the topic. */
    suspend fun expecting(): P = receive(count = 1, timeout = DEFAULT_TIMEOUT).single()

    /** Receive a single payload and run [block] against it. */
    suspend fun expecting(block: (P) -> Unit): P = expecting().also(block)

    /** Receive [count] payloads (within [DEFAULT_TIMEOUT]) and run [block] against them. */
    suspend fun collecting(count: Int, block: (List<P>) -> Unit): List<P> = receive(count = count, timeout = DEFAULT_TIMEOUT).also(block)

    /** Receive every payload arriving within [duration] and run [block] against them. */
    suspend fun collecting(duration: Duration, block: (List<P>) -> Unit): List<P> = receive(count = Int.MAX_VALUE, timeout = duration).also(block)

    /** Receive a single payload and project it. */
    suspend fun <T> returning(projection: (P) -> T): T = projection(expecting())

    // ---- internals ----

    private suspend fun generatePayload(overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?): P {
        val rs = currentAmbient().randomSource
        @Suppress("UNCHECKED_CAST")
        return if (PrimitiveArbs.supports(payloadClass)) {
            require(overrides == null) {
                "${channelClass.simpleName}: per-field send { } overrides are only supported for record payloads, " +
                    "not the primitive payload ${payloadClass.simpleName}."
            }
            PrimitiveArbs.forType(payloadClass).firstValue(rs) as P
        } else {
            val receiver = ArbReceiver(rs)
            val generator = overrides
                ?.let { kotestWirespecKotlinGenerator(seed = rs.random.nextLong()) { it() } }
                ?: receiver.generator
            receiver.generatorFor(payloadClass).generate(generator, emptyList()) as P
        }
    }

    private suspend fun publish(payload: P) {
        val ctx = currentAmbient().channelContext()
        val body = ctx.serialization.serializeBody(payload, payloadType)
        ctx.transport.publish(resolveTopic(ctx), key, body)
    }

    private suspend fun receive(count: Int, timeout: Duration): List<P> {
        val ctx = currentAmbient().channelContext()
        return ctx.transport.receive(resolveTopic(ctx), count, timeout)
            .map { ctx.serialization.deserializeBody<P>(it, payloadType) }
    }

    private fun resolveTopic(ctx: WirespecChannelContext): String = topic ?: ctx.defaultTopic ?: channelClass.simpleName
        ?: error("$channelClass: cannot resolve a topic — pin one with topic(...) or set a defaultTopic.")

    private companion object {
        val DEFAULT_TIMEOUT: Duration = 5.seconds
    }
}
