package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.generator.KotestWirespecGeneratorBuilder
import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import community.flock.wirespec.integration.kotest.extension.WirespecChannelContext
import community.flock.wirespec.integration.kotest.extension.currentChannelContext
import community.flock.wirespec.integration.kotest.runtime.PrimitiveArbs
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** Build a [ChannelCallBuilder] for a channel. Called by generated `*Dsl` wrappers. */
inline fun <reified P : Any> channelCall(channelClass: KClass<*>): ChannelCallBuilder<P> = ChannelCallBuilder(channelClass, typeOf<P>(), P::class.java)

/** Eager channel scenario runner that generates a typed payload and publishes it through the broker transport. */
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

    /** A [Gen] materialising a random payload on each draw, optionally applying per-field [overrides]. */
    fun messageGen(overrides: (KotestWirespecGeneratorBuilder.() -> Unit)? = null): Gen<P> = arbitrary { rs -> buildPayload(rs, overrides) }

    /** Draw a payload from [gen], publish it, and return it. */
    suspend fun send(gen: Gen<P>): P {
        val payload = gen.draw(currentRandomSource())
        publish(payload)
        return payload
    }

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
        val ctx = currentChannelContext()
        val body = ctx.serialization.serializeBody(payload, payloadType)
        ctx.transport.publish(resolveTopic(), key, body)
    }

    private fun resolveTopic(): String = topic ?: channelClass.simpleName
        ?: error("$channelClass: cannot resolve a topic — pin one with topic(...).")
}
