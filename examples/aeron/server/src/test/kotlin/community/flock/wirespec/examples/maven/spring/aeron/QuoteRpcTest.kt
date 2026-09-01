package community.flock.wirespec.examples.maven.spring.aeron

import community.flock.wirespec.generated.examples.aeron.model.Money
import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.model.QuoteList
import community.flock.wirespec.generated.examples.aeron.model.Tick
import community.flock.wirespec.generated.examples.aeron.model.Venue
import community.flock.wirespec.generated.examples.aeron.model.Watchlist
import community.flock.wirespec.integration.aeron.AeronRpcClient
import community.flock.wirespec.integration.aeron.AeronRpcServer
import community.flock.wirespec.integration.aeron.RpcResult
import community.flock.wirespec.kotlin.Wirespec
import io.aeron.Aeron
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class QuoteRpcTest(
    @Autowired private val aeron: Aeron,
    @Autowired private val serialization: Wirespec.Serialization,
    @Value("\${wirespec.aeron.requestChannel:aeron:udp?endpoint=localhost:40123}") private val requestChannel: String,
    @Value("\${wirespec.aeron.watchlistChannel:aeron:udp?endpoint=localhost:40125}") private val watchlistChannel: String,
) {

    private val aapl = Quote(
        symbol = "AAPL",
        last = Money(178.25, "USD"),
        previousClose = Money(176.1, "USD"),
        venue = Venue("XNAS", "New York"),
        history = listOf(Tick("09:30", Money(177.5, "USD")), Tick("16:00", Money(178.25, "USD"))),
    )
    private val flck = Quote(
        symbol = "FLCK",
        last = Money(42.0, "EUR"),
        previousClose = null,
        venue = Venue("XAMS", "Amsterdam"),
        history = listOf(Tick("16:00", Money(42.0, "EUR"))),
    )

    private fun <T> call(block: suspend (AeronRpcClient<Wirespec.Serialization>) -> T): T =
        AeronRpcClient(
            aeron,
            serialization,
            requestChannel = requestChannel,
            replyChannel = "aeron:udp?endpoint=localhost:40224",
            replyStreamId = 2001,
        ).use { client ->
            client.start()
            runBlocking { block(client) }
        }

    @Test
    fun `ping answers pong`() {
        assertEquals("pong", call { it.call<String>("Ping") })
    }

    @Test
    fun `a known symbol answers its quote`() {
        assertEquals(
            RpcResult.Success(aapl),
            call { it.callResult<GetQuoteParams, Quote, QuoteError>("GetQuote", GetQuoteParams("AAPL")) },
        )
    }

    @Test
    fun `an unknown symbol answers the rpc's error type`() {
        assertEquals(
            RpcResult.Failure(QuoteError("UNKNOWN_SYMBOL", "No quote for symbol 'NOPE'")),
            call { it.callResult<GetQuoteParams, Quote, QuoteError>("GetQuote", GetQuoteParams("NOPE")) },
        )
    }

    @Test
    fun `the server calls back into the client's watchlist service`() {
        // Play the client's role: serve GetWatchlist on the stream the server calls out on.
        AeronRpcServer(aeron, serialization, channel = watchlistChannel, streamId = AeronConfiguration.WATCHLIST_STREAM_ID).use { watchlistService ->
            watchlistService.bindEmpty("GetWatchlist") { Watchlist(listOf("AAPL", "FLCK")) }
            watchlistService.start()
            assertEquals(
                QuoteList(listOf(aapl, flck)),
                call { it.call<QuoteList>("GetWatchlistQuotes") },
            )
        }
    }
}
