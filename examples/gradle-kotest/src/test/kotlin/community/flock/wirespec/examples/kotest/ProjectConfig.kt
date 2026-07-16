package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.integration.kotest.ChannelTransport
import community.flock.wirespec.integration.kotest.WirespecChannelExtension
import community.flock.wirespec.integration.kotest.WirespecEndpointExtension
import community.flock.wirespec.integration.kotest.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.testContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration.ofMillis
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration

/**
 * The single Kotest project config, registering every extension once for the whole suite — so specs
 * carry no `@ApplyExtension` or in-body `extension(...)` wiring. It lives in this package (rather
 * than the default `io.kotest.provided.ProjectConfig`) because the test task points Kotest at it via
 * the `kotest.framework.config.fqn` system property (see `build.gradle.kts`).
 *
 * `SpringExtension` is listed first so it wraps the others: it loads each spec's `@SpringBootTest`
 * context, which the two framework-agnostic wirespec extensions then read from — via their `suspend`
 * factories calling `testContextManager()` — for the server port, the `Wirespec.Serialization` bean,
 * and the Kafka bootstrap servers. The channel extension builds one transport per spec, so the same
 * instance serves both the endpoint spec and the `@EmbeddedKafka` channel spec correctly.
 */
class ProjectConfig : AbstractProjectConfig() {
    override val extensions: List<Extension> = listOf(
        SpringExtension(),
        WirespecEndpointExtension(
            serialization = { serialization() },
            transportation = { HttpTransportation("http://localhost:${property("local.server.port")}") },
        ),
        WirespecChannelExtension(
            serialization = { serialization() },
            transport = { KafkaChannelTransport(property("spring.kafka.bootstrap-servers")) },
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
            reset = { it.clear() },
        ),
    )
}

private suspend fun serialization(): Wirespec.Serialization =
    testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)

private suspend fun property(name: String): String =
    testContextManager().testContext.applicationContext.environment.getProperty(name)
        ?: error("Property '$name' is not set in the test context")

/**
 * The broker side of the channel scenario DSL, backed by real Kafka. It implements the
 * framework-neutral [ChannelTransport] the DSL consumes and carries no topic of its own — it uses
 * whatever topic the DSL resolves for each call:
 *
 * - `publish` produces one message to its `topic` (this backs `CampaignEvents.generate.message { … }.send(topic)`);
 * - `receive` returns messages seen on its `topic` (this backs `listen { expecting { … } }`; the
 *   topic comes from the channel context's `defaultTopic` or an explicit `topic(...)`).
 *
 * A background consumer subscribes on the first `receive`, to that call's topic, and buffers every
 * record from `earliest` — so a message the app publishes is captured even if the test only starts
 * listening afterwards. Call [clear] between tests to drop anything left over from a previous
 * scenario. All receives are expected to use the same topic (one channel → one topic).
 */
class KafkaChannelTransport(
    bootstrapServers: String,
) : ChannelTransport,
    AutoCloseable {

    private val producer = KafkaProducer<String, ByteArray>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,
        ),
    )

    private val consumer = KafkaConsumer<String, ByteArray>(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "kotest-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
        ),
    )

    private val buffer = LinkedBlockingQueue<ByteArray>()
    private val running = AtomicBoolean(true)
    private val polling = AtomicBoolean(false)

    override suspend fun publish(topic: String, key: String?, body: ByteArray): Unit = withContext(Dispatchers.IO) {
        producer.send(ProducerRecord(topic, key, body)).get()
    }

    override suspend fun receive(topic: String, count: Int, timeout: Duration): List<ByteArray> = withContext(Dispatchers.IO) {
        startPolling(topic)
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        val result = mutableListOf<ByteArray>()
        while (result.size < count) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) break
            result += buffer.poll(remaining, TimeUnit.NANOSECONDS) ?: break
        }
        result
    }

    /** Start the background consumer on the first receive, subscribed to that call's [topic]. */
    private fun startPolling(topic: String) {
        if (!polling.compareAndSet(false, true)) return
        thread(name = "kotest-kafka-consumer", isDaemon = true) {
            consumer.subscribe(listOf(topic))
            try {
                while (running.get()) {
                    consumer.poll(ofMillis(200)).forEach { buffer.add(it.value()) }
                }
            } catch (_: WakeupException) {
                // expected on close()
            } finally {
                consumer.close()
            }
        }
    }

    /** Drop any buffered records so the next scenario starts from a clean slate. */
    fun clear() = buffer.clear()

    override fun close() {
        running.set(false)
        // The poll thread owns the consumer and closes it on wakeup; close it here only if it never started.
        if (polling.get()) consumer.wakeup() else consumer.close()
        producer.close()
    }
}
