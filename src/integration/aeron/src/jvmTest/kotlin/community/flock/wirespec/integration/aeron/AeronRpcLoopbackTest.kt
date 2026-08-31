package community.flock.wirespec.integration.aeron

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.kotlin.Wirespec
import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import io.aeron.driver.ThreadingMode
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KType
import kotlin.reflect.javaType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AeronRpcLoopbackTest {

    private data class GetQuoteParams(val symbol: String)
    private data class Quote(val symbol: String, val price: Double, val currency: String)
    private data class QuoteError(val code: String, val message: String)

    private lateinit var driver: MediaDriver
    private lateinit var aeron: Aeron
    private lateinit var server: AeronRpcServer<JacksonBodySerialization>
    private lateinit var client: AeronRpcClient<JacksonBodySerialization>

    @BeforeTest
    fun start() {
        driver = MediaDriver.launchEmbedded(
            MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true),
        )
        aeron = Aeron.connect(Aeron.Context().aeronDirectoryName(driver.aeronDirectoryName()))
        server = AeronRpcServer(aeron, JacksonBodySerialization).apply {
            bindEmpty("Ping") { "pong" }
            bind<GetQuoteParams, Quote>("EchoQuote") { params -> Quote(params.symbol, 1.5, "USD") }
            bindResult<GetQuoteParams, Quote, QuoteError>("GetQuote") { params ->
                when (params.symbol) {
                    "AAPL" -> RpcResult.Success(Quote("AAPL", 1.5, "USD"))
                    else -> RpcResult.Failure(QuoteError("UNKNOWN_SYMBOL", "No quote for ${params.symbol}"))
                }
            }
            bind<String, String>("EchoText") { it }
            start()
        }
        client = AeronRpcClient(aeron, JacksonBodySerialization).apply { start() }
    }

    @AfterTest
    fun stop() {
        client.close()
        server.close()
        aeron.close()
        driver.close()
    }

    @Test
    fun `parameterless rpc answers directly`() = runBlocking {
        assertEquals("pong", client.call<String>("Ping"))
    }

    @Test
    fun `rpc without error type answers with its result`() = runBlocking {
        assertEquals(Quote("FLCK", 1.5, "USD"), client.call<GetQuoteParams, Quote>("EchoQuote", GetQuoteParams("FLCK")))
    }

    @Test
    fun `rpc with error type answers success`() = runBlocking {
        val result = client.callResult<GetQuoteParams, Quote, QuoteError>("GetQuote", GetQuoteParams("AAPL"))
        assertEquals(RpcResult.Success(Quote("AAPL", 1.5, "USD")), result)
    }

    @Test
    fun `rpc with error type answers failure`() = runBlocking {
        val result = client.callResult<GetQuoteParams, Quote, QuoteError>("GetQuote", GetQuoteParams("NOPE"))
        assertEquals(RpcResult.Failure(QuoteError("UNKNOWN_SYMBOL", "No quote for NOPE")), result)
    }

    @Test
    fun `unknown method surfaces as an exception`() = runBlocking {
        val exception = assertFailsWith<AeronRpcException> { client.call<String>("Nope") }
        assertEquals("Unknown rpc method: Nope", exception.errorBody)
    }

    @Test
    fun `payloads larger than a single fragment survive the trip`() = runBlocking {
        val text = "wirespec".repeat(100_000)
        assertEquals(text, client.call<String, String>("EchoText", text))
    }

    @Test
    fun `concurrent calls correlate to their own responses`() = runBlocking {
        val symbols = ('A'..'Z').map { "SYM$it" }
        val quotes = symbols.map { client.call<GetQuoteParams, Quote>("EchoQuote", GetQuoteParams(it)) }
        assertEquals(symbols, quotes.map(Quote::symbol))
    }

    private object JacksonBodySerialization : Wirespec.BodySerialization {
        private val mapper = ObjectMapper().registerKotlinModule()

        override fun <T : Any> serializeBody(t: T, kType: KType): ByteArray = when (t) {
            is String -> t.encodeToByteArray()
            else -> mapper.writeValueAsBytes(t)
        }

        @Suppress("UNCHECKED_CAST")
        @OptIn(ExperimentalStdlibApi::class)
        override fun <T : Any> deserializeBody(raw: ByteArray, kType: KType): T = when (kType.classifier) {
            String::class -> raw.decodeToString() as T
            else -> mapper.readValue(raw, mapper.constructType(kType.javaType))
        }
    }
}
