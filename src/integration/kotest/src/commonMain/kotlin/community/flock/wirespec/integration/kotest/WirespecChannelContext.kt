package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

/**
 * Framework-neutral broker handle the channel scenario DSL consumes. A
 * [ChannelTransport] publishes raw message bodies; a [Wirespec.Serialization] turns
 * the channel's typed payload into those bytes.
 *
 * [defaultTopic] is used when a `generate.message { … }.send()` scenario does not pin
 * one with `topic(...)`; when it too is `null` the runtime falls back to the channel
 * object's simple name.
 */
class WirespecChannelContext(
    val transport: ChannelTransport,
    val serialization: Wirespec.Serialization,
    val defaultTopic: String? = null,
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
