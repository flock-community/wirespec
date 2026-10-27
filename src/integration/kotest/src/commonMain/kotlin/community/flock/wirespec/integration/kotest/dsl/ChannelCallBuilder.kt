package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.extension.currentChannelContext
import community.flock.wirespec.integration.kotest.runtime.PrimitiveArbs
import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public inline fun <reified P : Any> channelCall(channelClass: KClass<*>): ChannelCallBuilder<P> = ChannelCallBuilder(channelClass, typeOf<P>(), P::class.java)

@WirespecScenarioDsl
public class ChannelCallBuilder<P : Any> @PublishedApi internal constructor(
    @PublishedApi internal val channelClass: KClass<*>,
    @PublishedApi internal val payloadType: KType,
    @PublishedApi internal val payloadClass: Class<*>,
) {
    private var topic: String? = null
    private var key: String? = null

    public fun topic(value: String): ChannelCallBuilder<P> = apply { topic = value }
    public fun key(value: String): ChannelCallBuilder<P> = apply { key = value }

    public fun messageGen(): Arb<P> = arbitrary { rs -> buildPayload(rs) }

    public suspend fun send(gen: Gen<P>): P {
        val payload = gen.draw(currentRandomSource())
        publish(payload)
        return payload
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildPayload(rs: RandomSource): P = when (val primitive = PrimitiveArbs.forTypeOrNull(payloadClass)) {
        null -> ArbReceiver(rs).generateModel(payloadClass)
        else -> primitive.draw(rs) as P
    }

    private suspend fun publish(payload: P) {
        val ctx = currentChannelContext()
        val body = ctx.serialization.serializeBody(payload, payloadType)
        ctx.transport.publish(resolveTopic(), key, body)
    }

    private fun resolveTopic(): String = topic ?: channelClass.simpleName
        ?: error("$channelClass: cannot resolve a topic — pin one with topic(...).")
}
