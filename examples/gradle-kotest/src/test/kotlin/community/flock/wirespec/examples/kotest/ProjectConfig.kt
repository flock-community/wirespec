package community.flock.wirespec.examples.kotest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.ValueMatcher
import community.flock.wirespec.integration.jvm.transport.HttpTransportation
import community.flock.wirespec.integration.kotest.extension.ChannelTransport
import community.flock.wirespec.integration.kotest.extension.MockServer
import community.flock.wirespec.integration.kotest.extension.MockStub
import community.flock.wirespec.integration.kotest.extension.WirespecChannelExtension
import community.flock.wirespec.integration.kotest.extension.WirespecEndpointExtension
import community.flock.wirespec.integration.kotest.extension.WirespecMockExtension
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
        ),
        WirespecMockExtension(
            server = inventoryMockServer,
            serialization = { serialization() },
        ),
    )
}

private suspend fun serialization(): Wirespec.Serialization = testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)

private suspend fun property(name: String): String = testContextManager().testContext.applicationContext.environment.getProperty(name)
    ?: error("Property '$name' is not set in the test context")

@Suppress("ktlint:standard:function-naming") // factory function for a ChannelTransport, named like the type it builds
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

val inventoryMockServer: WireMockMockServer = WireMockMockServer.start().also { server ->
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
}

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

    private fun matchesSafely(stub: MockStub, request: Request): Boolean = try {
        stub.matches(request.toRawRequest())
    } catch (_: Throwable) {
        false
    }

    companion object {
        fun start(port: Int = 0): WireMockMockServer {
            val options = WireMockConfiguration.options()
            if (port == 0) options.dynamicPort() else options.port(port)
            return WireMockMockServer(WireMockServer(options).apply { start() })
        }
    }
}
