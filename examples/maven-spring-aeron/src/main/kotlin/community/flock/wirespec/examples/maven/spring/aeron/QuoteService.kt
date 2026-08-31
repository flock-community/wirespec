package community.flock.wirespec.examples.maven.spring.aeron

import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.rpc.GetQuote
import community.flock.wirespec.generated.examples.aeron.rpc.Ping
import org.springframework.stereotype.Service

@Service
class QuoteService :
    GetQuote.Service,
    Ping.Service {

    private val quotes = listOf(
        Quote("AAPL", 178.25, "USD"),
        Quote("GOOG", 141.8, "USD"),
        Quote("FLCK", 42.0, "EUR"),
    ).associateBy(Quote::symbol)

    override suspend fun getQuote(symbol: String): GetQuote.Response = quotes[symbol]
        ?.let { GetQuote.Result(it) }
        ?: GetQuote.Error(QuoteError("UNKNOWN_SYMBOL", "No quote for symbol '$symbol'"))

    override suspend fun ping(): String = "pong"
}
