package community.flock.wirespec.examples.maven.spring.aeron

import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.model.QuoteList
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
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class QuoteRpcTest(
    @Autowired private val aeron: Aeron,
    @Autowired private val serialization: Wirespec.Serialization,
) {

    private fun <T> call(block: suspend (AeronRpcClient<Wirespec.Serialization>) -> T): T =
        AeronRpcClient(aeron, serialization, replyStreamId = 2001).use { client ->
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
            RpcResult.Success(Quote("AAPL", 178.25, "USD")),
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
        AeronRpcServer(aeron, serialization, streamId = AeronConfiguration.WATCHLIST_STREAM_ID).use { watchlistService ->
            watchlistService.bindEmpty("GetWatchlist") { Watchlist(listOf("AAPL", "FLCK")) }
            watchlistService.start()
            assertEquals(
                QuoteList(listOf(Quote("AAPL", 178.25, "USD"), Quote("FLCK", 42.0, "EUR"))),
                call { it.call<QuoteList>("GetWatchlistQuotes") },
            )
        }
    }
}
