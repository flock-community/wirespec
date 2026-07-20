package community.flock.wirespec.examples.kotest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.ValueMatcher
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.integration.kotest.ChannelTransport
import community.flock.wirespec.integration.kotest.MockServer
import community.flock.wirespec.integration.kotest.MockStub
import community.flock.wirespec.integration.kotest.extension.WirespecChannelExtension
import community.flock.wirespec.integration.kotest.extension.WirespecEndpointExtension
import community.flock.wirespec.integration.kotest.extension.WirespecMockExtension
import community.flock.wirespec.integration.java.transport.HttpTransportation
import community.flock.wirespec.integration.wiremock.kotlin.requestBuilder
import community.flock.wirespec.integration.wiremock.kotlin.responseBuilder
import community.flock.wirespec.integration.wiremock.kotlin.toRawRequest
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
 * context, which the framework-agnostic wirespec extensions then read from — via their `suspend`
 * factories calling `testContextManager()` — for the server port, the `Wirespec.Serialization` bean,
 * and the Kafka bootstrap servers. The channel extension builds one transportation per spec; the mock
 * extension resets [inventoryMockServer] before each test but leaves it open (it is owned here, its
 * base URL fed to the app via `@DynamicPropertySource` in `ProductAvailabilityMockTest`).
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
            transportation = { KafkaChannelTransport(property("spring.kafka.bootstrap-servers")) },
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
        ),
        WirespecMockExtension(
            server = inventoryMockServer,
            serialization = { serialization() },
        ),
    )
}

private suspend fun serialization(): Wirespec.Serialization =
    testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)

private suspend fun property(name: String): String =
    testContextManager().testContext.applicationContext.environment.getProperty(name)
        ?: error("Property '$name' is not set in the test context")

/**
 * The broker side of the channel scenario DSL, backed by a real Kafka producer. Because
 * [ChannelTransport] is a `fun interface`, this is a factory function that returns the publish
 * behaviour as a lambda closing over a producer — it carries no topic of its own and publishes to
 * whatever topic the DSL resolves for each call (backing `CampaignEvents.generate.message { … }.send(topic)`).
 *
 * The DSL is send-only: asserting on what the app published is the test's job, done with a plain
 * Kafka consumer (see `CampaignChannelScenarioTest`), not through this transport. A lambda transport
 * is not `AutoCloseable` for the channel extension to close per spec, so the producer is released on
 * JVM shutdown — fine for a short-lived test process.
 */
fun KafkaChannelTransport(bootstrapServers: String): ChannelTransport {
    val producer = KafkaProducer<String, ByteArray>(
        mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,
        ),
    )
    Runtime.getRuntime().addShutdownHook(Thread { producer.close() })
    return ChannelTransport { topic, key, body ->
        withContext(Dispatchers.IO) { producer.send(ProducerRecord(topic, key, body)).get() }
    }
}

/**
 * The suite-wide mock server standing in for the downstream inventory service (see
 * `ProductAvailabilityMockTest`). Started eagerly so its [baseUrl][WireMockMockServer.baseUrl] is
 * known before any Spring context boots — that spec's `@DynamicPropertySource` wires it into the
 * app's `inventory.base-url`. Closed on JVM shutdown; the mock extension only resets its stubs.
 */
val inventoryMockServer: WireMockMockServer = WireMockMockServer.start().also { server ->
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
}

/**
 * The WireMock-backed [MockServer] the response side of the scenario DSL drives — the mock
 * counterpart to [KafkaChannelTransport]: it implements the framework-neutral [MockServer] the DSL
 * consumes and carries no wirespec types of its own, translating each [MockStub] into a WireMock stub.
 *
 * The method/path match, the request mapping and the response body all come from the wirespec WireMock
 * integration ([requestBuilder]/[toRawRequest]/[responseBuilder]); on top of the method/path match it
 * defers to [MockStub.matches] — the lowered `.mock { req -> … }` predicate — via a WireMock
 * [ValueMatcher], replying with the drawn, already serialized [Wirespec.RawResponse]. [reset] drops all
 * stubs; [close] stops the server.
 */
class WireMockMockServer private constructor(
    private val server: WireMockServer,
) : MockServer,
    AutoCloseable {

    val baseUrl: String get() = server.baseUrl()

    val port: Int get() = server.port()

    override fun stub(stub: MockStub) {
        val mapping = requestBuilder(stub.method, stub.pathTemplate)
            .andMatching(
                ValueMatcher<Request> { request ->
                    if (matchesSafely(stub, request)) MatchResult.exactMatch() else MatchResult.noMatch()
                },
            )
            .willReturn(responseBuilder(stub.response))
        server.stubFor(mapping)
    }

    override fun reset() = server.resetAll()

    override fun close() = server.stop()

    // A request that slips through the method/path matcher but does not belong to this endpoint
    // can fail to deserialize; treat that as "no match" rather than failing the whole request.
    private fun matchesSafely(stub: MockStub, request: Request): Boolean = try {
        stub.matches(request.toRawRequest())
    } catch (_: Throwable) {
        false
    }

    companion object {
        /** Start a WireMock server on [port] (0 selects a free dynamic port). Stop it via [close]. */
        fun start(port: Int = 0): WireMockMockServer {
            val options = WireMockConfiguration.options()
            if (port == 0) options.dynamicPort() else options.port(port)
            return WireMockMockServer(WireMockServer(options).apply { start() })
        }
    }
}

