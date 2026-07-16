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
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer

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
        ),
    )
}

private suspend fun serialization(): Wirespec.Serialization =
    testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)

private suspend fun property(name: String): String =
    testContextManager().testContext.applicationContext.environment.getProperty(name)
        ?: error("Property '$name' is not set in the test context")

/**
 * The broker side of the channel scenario DSL, backed by a real Kafka producer. It implements the
 * framework-neutral [ChannelTransport] the DSL consumes and carries no topic of its own — it publishes
 * to whatever topic the DSL resolves for each call (this backs
 * `CampaignEvents.generate.message { … }.send(topic)`).
 *
 * The DSL is send-only: asserting on what the app published is the test's job, done with a plain
 * Kafka consumer (see `CampaignChannelScenarioTest`), not through this transport.
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

    override suspend fun publish(topic: String, key: String?, body: ByteArray): Unit = withContext(Dispatchers.IO) {
        producer.send(ProducerRecord(topic, key, body)).get()
    }

    override fun close() = producer.close()
}
