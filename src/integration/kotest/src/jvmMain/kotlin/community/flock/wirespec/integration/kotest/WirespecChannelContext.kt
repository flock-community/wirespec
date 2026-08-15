package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import kotlin.time.Duration

/**
 * Framework-neutral broker handle the channel scenario DSL consumes. A
 * [ChannelTransport] publishes and receives raw message bodies; a
 * [Wirespec.Serialization] turns those bytes into the channel's typed payload.
 *
 * [defaultTopic] is used when a `*.call { … }` scenario does not pin one with
 * `topic(...)`; when it too is `null` the runtime falls back to the channel
 * object's simple name.
 */
class WirespecChannelContext(
    val transport: ChannelTransport,
    val serialization: Wirespec.Serialization,
    val defaultTopic: String? = null,
)

/**
 * The minimal publish/receive surface a message broker must expose to back the
 * channel scenario DSL. Implementations (e.g. a Kafka producer/consumer pair) are
 * supplied per-spec to
 * [WirespecExtension][community.flock.wirespec.integration.kotest.WirespecExtension]
 * as part of a [WirespecChannelContext].
 */
interface ChannelTransport {
    /** Publish a single serialized message [body] to [topic] under an optional [key]. */
    suspend fun publish(topic: String, key: String?, body: ByteArray)

    /**
     * Receive up to [count] serialized message bodies from [topic], returning early
     * once [count] have arrived and otherwise after [timeout] elapses (so the
     * `collecting(duration)` form can gather everything seen within a window).
     */
    suspend fun receive(topic: String, count: Int, timeout: Duration): List<ByteArray>
}
