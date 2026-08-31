package community.flock.wirespec.examples.maven.spring.aeron

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import community.flock.wirespec.generated.examples.aeron.model.Quote
import community.flock.wirespec.generated.examples.aeron.model.QuoteError
import community.flock.wirespec.generated.examples.aeron.rpc.GetQuote
import community.flock.wirespec.integration.aeron.AeronRpcClient
import community.flock.wirespec.integration.aeron.AeronRpcServer
import community.flock.wirespec.integration.aeron.RpcResult
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.kotlin.Wirespec
import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import io.aeron.driver.ThreadingMode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

/** The map a GetQuote request payload carries: the rpc's parameters by name. */
data class GetQuoteParams(val symbol: String)

/**
 * Channels are `aeron:udp` endpoints, so the client can be anywhere on the
 * network; each side attaches to its own local media driver, and the drivers
 * carry the frames. Every channel names a resolvable host - the side that
 * subscribes on it binds that interface, the side that publishes sends to it.
 */
@Configuration
open class AeronConfiguration {

    private val log = LoggerFactory.getLogger(AeronConfiguration::class.java)

    @Bean(destroyMethod = "close")
    open fun mediaDriver(@Value("\${wirespec.aeron.dir:}") dir: String): MediaDriver = MediaDriver.launch(
        MediaDriver.Context()
            .aeronDirectoryName(dir.ifBlank { Paths.get(System.getProperty("java.io.tmpdir"), "wirespec-aeron").toString() })
            .threadingMode(ThreadingMode.SHARED)
            // Aeron's default term buffers put a ~192MB (ipc) or ~48MB (udp) log
            // behind every publication; 4MB terms (512KB max message) are plenty
            // here and keep the demo comfortably inside a 1GB /dev/shm.
            .ipcTermBufferLength(4 * 1024 * 1024)
            .publicationTermBufferLength(4 * 1024 * 1024)
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true),
    )

    @Bean(destroyMethod = "close")
    open fun aeron(mediaDriver: MediaDriver): Aeron = Aeron.connect(Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()))

    /** CBOR: the same Jackson mapping as JSON, in a compact binary encoding. */
    @Bean
    open fun serialization(): Wirespec.Serialization = WirespecSerialization(CBORMapper.builder().addModule(kotlinModule()).build())

    /** Outbound side of the reverse direction: calls rpcs the client serves on [WATCHLIST_STREAM_ID]. */
    @Bean(destroyMethod = "close")
    open fun watchlistClient(
        aeron: Aeron,
        serialization: Wirespec.Serialization,
        @Value("\${wirespec.aeron.watchlistChannel:aeron:udp?endpoint=localhost:40125}") watchlistChannel: String,
        @Value("\${wirespec.aeron.watchlistReplyChannel:aeron:udp?endpoint=localhost:40126}") watchlistReplyChannel: String,
    ): AeronRpcClient<Wirespec.Serialization> = AeronRpcClient(
        aeron,
        serialization,
        requestChannel = watchlistChannel,
        requestStreamId = WATCHLIST_STREAM_ID,
        replyChannel = watchlistReplyChannel,
        replyStreamId = WATCHLIST_REPLY_STREAM_ID,
    ).apply { start() }

    @Bean(destroyMethod = "close")
    open fun rpcServer(
        aeron: Aeron,
        serialization: Wirespec.Serialization,
        service: QuoteService,
        @Value("\${wirespec.aeron.requestChannel:aeron:udp?endpoint=localhost:40123}") requestChannel: String,
    ): AeronRpcServer<Wirespec.Serialization> = AeronRpcServer(aeron, serialization, channel = requestChannel).apply {
        bindEmpty("Ping") { service.ping() }
        bindResult<GetQuoteParams, Quote, QuoteError>("GetQuote") { params ->
            when (val response = service.getQuote(params.symbol)) {
                is GetQuote.Result -> RpcResult.Success(response.value)
                is GetQuote.Error -> RpcResult.Failure(response.value)
            }
        }
        bindEmpty("GetWatchlistQuotes") { service.getWatchlistQuotes() }
        start()
        log.info("Wirespec Aeron rpc server listening on {} stream 1001, media driver at {}", requestChannel, aeron.context().aeronDirectoryName())
    }

    companion object {
        /** The stream the client serves its rpcs on; this server calls it there. */
        const val WATCHLIST_STREAM_ID = 3001

        /** The stream this server receives the client's answers on. */
        const val WATCHLIST_REPLY_STREAM_ID = 3002
    }
}
