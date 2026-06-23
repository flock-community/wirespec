package community.flock.wirespec.examples.kotest.scenario

import community.flock.wirespec.examples.kotest.CampaignApplication
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecExtension
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.spec.style.FunSpec
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker

/**
 * Base for scenario specs. Boots an in-JVM Kafka broker and the real Spring app
 * (on a random port, pointed at that broker) once per spec, then exposes the
 * endpoint and channel transport contexts that back the generated `*.call { … }`
 * DSL — resolved through [ScenarioContextProvider].
 *
 * Concrete specs add their tests in their own `init { }` block; setup/teardown is
 * registered here and runs before/after them regardless of order.
 */
abstract class CampaignScenarioSpec : FunSpec(), ScenarioContext {

    private lateinit var broker: EmbeddedKafkaKraftBroker
    private lateinit var app: ConfigurableApplicationContext
    private lateinit var endpointContext: WirespecTestContext
    private lateinit var channelContext: WirespecChannelContext
    private var kafkaTransport: KafkaChannelTransport? = null

    override fun endpointContext(): WirespecTestContext = endpointContext
    override fun channelContext(): WirespecChannelContext = channelContext

    /** The running application context, for asserting against beans (e.g. the listener). */
    protected fun application(): ConfigurableApplicationContext = app

    init {
        extension(WirespecExtension())

        beforeSpec {
            broker = EmbeddedKafkaKraftBroker(1, 1, CAMPAIGN_EVENTS_TOPIC).apply { afterPropertiesSet() }
            val bootstrap = broker.brokersAsString

            app = SpringApplicationBuilder(CampaignApplication::class.java)
                .properties(
                    "server.port=0",
                    "spring.kafka.bootstrap-servers=$bootstrap",
                )
                .run()

            val port = (app as ServletWebServerApplicationContext).webServer.port
            val serialization = app.getBean(Wirespec.Serialization::class.java)

            endpointContext = WirespecTestContext(
                transportation = HttpClientTransportation("http://localhost:$port"),
                serialization = serialization,
            )
            kafkaTransport = KafkaChannelTransport(bootstrap, CAMPAIGN_EVENTS_TOPIC)
            channelContext = WirespecChannelContext(
                transport = kafkaTransport!!,
                serialization = serialization,
                defaultTopic = CAMPAIGN_EVENTS_TOPIC,
            )
        }

        afterSpec {
            kafkaTransport?.close()
            if (::app.isInitialized) app.close()
            if (::broker.isInitialized) broker.destroy()
        }
    }
}
