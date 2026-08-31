package community.flock.wirespec.examples.maven.spring.aeron

import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.model.QuoteList
import community.flock.wirespec.generated.examples.aeron.model.Watchlist
import community.flock.wirespec.generated.examples.aeron.rpc.GetQuote
import community.flock.wirespec.generated.examples.aeron.rpc.GetWatchlistQuotes
import community.flock.wirespec.generated.examples.aeron.rpc.Ping
import community.flock.wirespec.integration.aeron.AeronRpcClient
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.stereotype.Service

@Service
class QuoteService(
    private val watchlistClient: AeronRpcClient<Wirespec.Serialization>,
) : GetQuote.Service,
    Ping.Service,
    GetWatchlistQuotes.Service {

    private val quotes = listOf(
        Quote("AAPL", 178.25, "USD"),
        Quote("GOOG", 141.8, "USD"),
        Quote("FLCK", 42.0, "EUR"),
    ).associateBy(Quote::symbol)

    override suspend fun getQuote(symbol: String): GetQuote.Response = quotes[symbol]
        ?.let { GetQuote.Result(it) }
        ?: GetQuote.Error(QuoteError("UNKNOWN_SYMBOL", "No quote for symbol '$symbol'"))

    override suspend fun ping(): String = "pong"

    /** The reverse direction: this server calls the client's GetWatchlist rpc. */
    override suspend fun getWatchlistQuotes(): QuoteList = watchlistClient
        .call<Watchlist>("GetWatchlist")
        .let { watchlist -> QuoteList(watchlist.symbols.mapNotNull(quotes::get)) }
}
