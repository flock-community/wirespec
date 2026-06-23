package community.flock.wirespec.examples.kotest.scenario

import community.flock.wirespec.integration.kotest.ChannelTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration as JavaDuration
import java.util.Properties
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A [ChannelTransport] over the (embedded) Kafka broker. The consumer is assigned
 * to [topic] and positioned at the log end on construction, so `receive` only ever
 * observes events produced *during the spec* — both those the scenario sends itself
 * and those the application publishes in reaction to an endpoint call.
 */
class KafkaChannelTransport(
    bootstrapServers: String,
    private val topic: String,
) : ChannelTransport, AutoCloseable {

    private val producer = KafkaProducer<String, String>(
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        },
    )

    private val consumer = KafkaConsumer<String, String>(
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "scenario-${UUID.randomUUID()}")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        },
    ).apply {
        val partitions = partitionsFor(topic).map { TopicPartition(it.topic(), it.partition()) }
        assign(partitions)
        seekToEnd(partitions)
        // Force the lazy seek to resolve now (at the current log end) so events
        // published after construction — but before the first receive — are seen.
        partitions.forEach { position(it) }
    }

    override suspend fun publish(topic: String, key: String?, body: ByteArray): Unit =
        withContext(Dispatchers.IO) {
            producer.send(ProducerRecord(topic, key, String(body))).get()
            Unit
        }

    override suspend fun receive(topic: String, count: Int, timeout: Duration): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val deadline = TimeSource.Monotonic.markNow() + timeout
            val out = mutableListOf<ByteArray>()
            while (out.size < count && deadline.hasNotPassedNow()) {
                consumer.poll(JavaDuration.ofMillis(200)).forEach { out.add(it.value().toByteArray()) }
            }
            out
        }

    override fun close() {
        producer.close()
        consumer.close()
    }
}
