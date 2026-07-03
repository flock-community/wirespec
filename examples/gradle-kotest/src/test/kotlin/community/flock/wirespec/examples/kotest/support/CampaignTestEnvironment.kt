package community.flock.wirespec.examples.kotest.support

import community.flock.wirespec.examples.kotest.CampaignApplication
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.integration.jvm.HttpClientTransportation
import community.flock.wirespec.integration.kotest.ChannelTransport
import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.kotlin.Wirespec
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
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker
import java.time.Duration as JavaDuration
import java.util.Properties
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Process-wide test environment shared by every scenario spec: one in-JVM Kafka broker
 * and one Spring app instance (on a random port, pointed at that broker), started lazily
 * on first use and torn down by a JVM shutdown hook.
 *
 * The endpoint and channel transport contexts it exposes are what each spec hands to
 * `WirespecExtension` for the generated `*.call { … }` DSL, so specs only need a single
 * `extension(WirespecExtension(…))` line — no base class.
 */
object CampaignTestEnvironment {

    private val broker: EmbeddedKafkaKraftBroker by lazy {
        EmbeddedKafkaKraftBroker(1, 1, CAMPAIGN_EVENTS_TOPIC).apply { afterPropertiesSet() }
    }

    /** The running application context, for asserting against beans (e.g. the listener). */
    val application: ConfigurableApplicationContext by lazy {
        val context = SpringApplicationBuilder(CampaignApplication::class.java)
            .properties(
                "server.port=0",
                "spring.kafka.bootstrap-servers=${broker.brokersAsString}",
            )
            .run()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runCatching { channelTransport.close() }
                runCatching { context.close() }
                runCatching { broker.destroy() }
            },
        )
        context
    }

    private val serialization: Wirespec.Serialization by lazy {
        application.getBean(Wirespec.Serialization::class.java)
    }

    private val channelTransport: KafkaChannelTransport by lazy {
        KafkaChannelTransport(broker.brokersAsString, CAMPAIGN_EVENTS_TOPIC)
    }

    val endpointContext: WirespecTestContext by lazy {
        val port = (application as ServletWebServerApplicationContext).webServer.port
        WirespecTestContext(
            transportation = HttpClientTransportation("http://localhost:$port"),
            serialization = serialization,
        )
    }

    val channelContext: WirespecChannelContext by lazy {
        WirespecChannelContext(
            transport = channelTransport,
            serialization = serialization,
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
        )
    }

    /**
     * Reposition the shared channel consumer at the current log end, so a scenario's
     * `expecting`/`collecting` only observe events published *during that scenario* —
     * not leftovers from earlier tests on the shared broker. Call from `beforeEach { }`
     * in channel specs.
     */
    fun watchChannelFromNow() {
        channelTransport.seekToEnd()
    }
}

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

    /**
     * Move the consumer to the current log end and resolve the position eagerly. Lets a
     * spec start each scenario "watching from now", so `receive` only returns events
     * published afterwards (not leftovers from earlier tests on the shared broker).
     */
    fun seekToEnd() {
        val partitions = consumer.assignment()
        consumer.seekToEnd(partitions)
        partitions.forEach { consumer.position(it) }
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
