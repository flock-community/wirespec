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

/** A channel message as the [community.flock.wirespec.integration.kotest.extension.ChannelTransport] publishes it. */
public data class RawMessage(
    public val topic: String,
    public val key: String?,
    public val body: ByteArray,
)

public suspend fun RawMessage.send(): Unit = currentChannelContext().transport.publish(topic, key, body)

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

    public suspend fun send(gen: Gen<P>): P = gen.draw(currentRandomSource()).also { payload -> rawMessage(payload).send() }

    public suspend fun raw(gen: Gen<P>): RawMessage = rawMessage(gen.draw(currentRandomSource()))

    @Suppress("UNCHECKED_CAST")
    private fun buildPayload(rs: RandomSource): P = when (val primitive = PrimitiveArbs.forTypeOrNull(payloadClass)) {
        null -> ArbReceiver(rs).generateModel(payloadClass)
        else -> primitive.draw(rs) as P
    }

    private suspend fun rawMessage(payload: P): RawMessage = RawMessage(
        topic = resolveTopic(),
        key = key,
        body = currentChannelContext().serialization.serializeBody(payload, payloadType),
    )

    private fun resolveTopic(): String = topic ?: channelClass.simpleName
        ?: error("$channelClass: cannot resolve a topic — pin one with topic(...).")
}
