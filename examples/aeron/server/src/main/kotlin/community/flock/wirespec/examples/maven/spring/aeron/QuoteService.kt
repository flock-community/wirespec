package community.flock.wirespec.examples.maven.spring.aeron

import community.flock.wirespec.generated.examples.aeron.model.Money
import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.model.QuoteList
import community.flock.wirespec.generated.examples.aeron.model.Tick
import community.flock.wirespec.generated.examples.aeron.model.Venue
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
        Quote(
            symbol = "AAPL",
            last = Money(178.25, "USD"),
            previousClose = Money(176.1, "USD"),
            venue = Venue("XNAS", "New York"),
            history = listOf(Tick("09:30", Money(177.5, "USD")), Tick("16:00", Money(178.25, "USD"))),
        ),
        Quote(
            symbol = "GOOG",
            last = Money(141.8, "USD"),
            previousClose = Money(140.05, "USD"),
            venue = Venue("XNAS", "New York"),
            history = listOf(Tick("16:00", Money(141.8, "USD"))),
        ),
        Quote(
            symbol = "FLCK",
            last = Money(42.0, "EUR"),
            previousClose = null,
            venue = Venue("XAMS", "Amsterdam"),
            history = listOf(Tick("16:00", Money(42.0, "EUR"))),
        ),
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
