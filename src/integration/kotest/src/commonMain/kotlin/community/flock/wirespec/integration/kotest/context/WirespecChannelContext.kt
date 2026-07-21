package community.flock.wirespec.integration.kotest.context

import community.flock.wirespec.kotlin.Wirespec

/**
 * Framework-neutral broker handle the channel scenario DSL consumes. A
 * [ChannelTransport] publishes raw message bodies; a [Wirespec.Serialization] turns
 * the channel's typed payload into those bytes.
 *
 * A `generate.message { … }.send()` scenario publishes to the topic pinned with
 * `topic(...)`; when none is pinned the runtime falls back to the channel object's
 * simple name.
 */
class WirespecChannelContext(
    val transport: ChannelTransport,
    val serialization: Wirespec.Serialization,
)

/**
 * The minimal publish surface a message broker must expose to back the send side of
 * the channel scenario DSL. Implementations (e.g. a Kafka producer) are supplied
 * per-spec to
 * [WirespecChannelExtension][community.flock.wirespec.integration.kotest.extension.WirespecChannelExtension]
 * as part of a [WirespecChannelContext]. Asserting on what the app published is left to
 * the test's own broker consumer.
 */
fun interface ChannelTransport {
    /** Publish a single serialized message [body] to [topic] under an optional [key]. */
    suspend fun publish(topic: String, key: String?, body: ByteArray)
}
