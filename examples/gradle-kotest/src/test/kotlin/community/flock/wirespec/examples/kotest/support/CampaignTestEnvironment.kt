package community.flock.wirespec.examples.kotest.support

import community.flock.wirespec.examples.kotest.CampaignApplication
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker

/**
 * Process-wide test environment shared by every scenario spec: one in-JVM Kafka broker
 * and one Spring app instance (on a random port, pointed at that broker), started lazily
 * on first use and torn down by a JVM shutdown hook.
 *
 * The endpoint and channel transport contexts it exposes are what
 * [ScenarioContextProvider] hands to the generated `*.call { … }` DSL, so specs only
 * need `@ApplyExtension(WirespecExtension::class)` — no base class.
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
