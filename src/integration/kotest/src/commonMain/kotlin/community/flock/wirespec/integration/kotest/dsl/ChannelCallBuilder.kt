package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.KotestWirespecGeneratorBuilder
import community.flock.wirespec.integration.kotest.context.WirespecChannelContext
import community.flock.wirespec.integration.kotest.runtime.PrimitiveArbs
import community.flock.wirespec.integration.kotest.runtime.currentAmbient
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** Build a [ChannelCallBuilder] for a channel. Called by generated `*Dsl` wrappers. */
inline fun <reified P : Any> channelCall(channelClass: KClass<*>): ChannelCallBuilder<P> = ChannelCallBuilder(channelClass, typeOf<P>(), P::class.java)

/**
 * Eager channel scenario runner. `send*` generates (or accepts) a typed payload, serializes it via
 * the ambient [WirespecChannelContext] and publishes it through the broker transport. The
 * destination topic is `topic(...)` if pinned, else the channel object's simple name.
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
     * A [Gen] materialising a random payload on each draw, optionally applying per-field
     * [overrides]. Backs the generated `<Channel>.generate.message { … }`; `Gen<P>.send()`
     * draws one and publishes it. A primitive payload accepts no overrides.
     */
    fun messageGen(overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null): Gen<P> = arbitrary { rs -> buildPayload(rs, overrides) }

    // ---- send terminal ----

    /** Draw a payload from [gen], publish it, and return it. Backs the generated `Gen<Payload>.send()`. */
    suspend fun send(gen: Gen<P>): P {
        val payload = gen.draw(currentAmbient().randomSource)
        publish(payload)
        return payload
    }

    // ---- internals ----

    /**
     * Materialise a payload from [rs]: a record payload is drawn through the wirespec generator
     * (honouring per-field [overrides]); a primitive payload is drawn directly and rejects [overrides].
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildPayload(rs: RandomSource, overrides: (KotestWirespecGeneratorBuilder.() -> Unit)?): P = when (val primitive = PrimitiveArbs.forTypeOrNull(payloadClass)) {
        null -> ArbReceiver(rs).generateModel(payloadClass, overrides)
        else -> {
            require(overrides == null) {
                "${channelClass.simpleName}: per-field message { } overrides are only supported for record " +
                    "payloads, not the primitive payload ${payloadClass.simpleName}."
            }
            primitive.draw(rs) as P
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
