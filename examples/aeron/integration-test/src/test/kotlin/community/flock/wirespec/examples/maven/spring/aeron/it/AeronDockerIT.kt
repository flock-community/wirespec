package community.flock.wirespec.examples.maven.spring.aeron.it

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Paths
import java.time.Duration

/**
 * Runs the Kotlin Spring Boot backend and the Rust client together in Docker,
 * connected over the network: every channel is an `aeron:udp` endpoint on the
 * container network, and the payloads are CBOR. The backend embeds its media
 * driver; the client pod is two containers sharing one IPC namespace — a
 * media-driver sidecar (the pod's network identity, alias `client`) and the
 * Rust client attached to it over /dev/shm.
 *
 * The client calls the backend (Ping, GetQuote) and then GetWatchlistQuotes,
 * which makes the backend call the client's own GetWatchlist rpc: the reverse,
 * server-to-client direction — over UDP both ways.
 */
class AeronDockerIT {

    private val exampleRoot = Paths.get("..").toAbsolutePath().normalize()

    // Overridable for environments where Docker Hub is unreachable.
    private val serverBase = System.getenv("AERON_EXAMPLE_SERVER_BASE_IMAGE") ?: "eclipse-temurin:21-jre"
    private val clientBase = System.getenv("AERON_EXAMPLE_CLIENT_BASE_IMAGE") ?: "ubuntu:24.04"

    @Test
    fun `the Rust client and the Kotlin backend exchange rpcs over UDP`() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available; skipping")

        val serverImage = ImageFromDockerfile("wirespec-aeron-server", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.server"))
            .withFileFromPath("server.jar", exampleRoot.resolve("server/target/server.jar"))
            .withBuildArg("BASE_IMAGE", serverBase)
        val driverImage = ImageFromDockerfile("wirespec-aeron-driver", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.driver"))
            .withFileFromPath("driver-libs", Paths.get("target/driver-libs"))
            .withBuildArg("BASE_IMAGE", serverBase)
        val clientImage = ImageFromDockerfile("wirespec-aeron-client", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.client"))
            .withFileFromPath("client", exampleRoot.resolve("client/target/release/client"))
            .withBuildArg("BASE_IMAGE", clientBase)

        Network.newNetwork().use { network ->
            val server = GenericContainer(serverImage)
                .withNetwork(network)
                .withNetworkAliases("server")
                .withEnv("WIRESPEC_AERON_REQUESTCHANNEL", "aeron:udp?endpoint=server:40123")
                .withEnv("WIRESPEC_AERON_WATCHLISTCHANNEL", "aeron:udp?endpoint=client:40125")
                .withEnv("WIRESPEC_AERON_WATCHLISTREPLYCHANNEL", "aeron:udp?endpoint=server:40126")
                .withCreateContainerCmdModifier { cmd -> cmd.hostConfig!!.withShmSize(1L shl 30) }
                .waitingFor(Wait.forLogMessage(".*Wirespec Aeron rpc server listening.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2))
            // The client's own media driver: the pod's network identity, doing the UDP I/O.
            val clientDriver = GenericContainer(driverImage)
                .withNetwork(network)
                .withNetworkAliases("client")
                .withCreateContainerCmdModifier { cmd -> cmd.hostConfig!!.withIpcMode("shareable").withShmSize(1L shl 30) }
                .waitingFor(Wait.forLogMessage(".*wirespec aeron driver starting.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2))
            try {
                server.start()
                clientDriver.start()
                val client = GenericContainer(clientImage)
                    .withEnv("REQUEST_CHANNEL", "aeron:udp?endpoint=server:40123")
                    .withEnv("REPLY_CHANNEL", "aeron:udp?endpoint=client:40124")
                    .withEnv("SERVE_CHANNEL", "aeron:udp?endpoint=client:40125")
                    .withCreateContainerCmdModifier { cmd ->
                        // Join the sidecar's IPC namespace (the driver's /dev/shm)
                        // and its network namespace (one pod, one address).
                        cmd.hostConfig!!
                            .withIpcMode("container:${clientDriver.containerId}")
                            .withNetworkMode("container:${clientDriver.containerId}")
                    }
                    .withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)))
                try {
                    runCatching { client.start() }
                        .onFailure { throw AssertionError("Client run failed; server logs:\n${server.logs}\ndriver logs:\n${clientDriver.logs}", it) }
                    val logs = client.logs
                    listOf(
                        "Ping -> pong",
                        "GetQuote AAPL -> AAPL 178.25 USD",
                        "GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'",
                        "GetWatchlistQuotes -> AAPL 178.25 USD",
                        "GetWatchlistQuotes -> FLCK 42 EUR",
                        "Serving GetWatchlist for the backend",
                    ).forEach { expected ->
                        assertTrue(expected in logs) { "Expected client log to contain '$expected'; client logs:\n$logs\nserver logs:\n${server.logs}" }
                    }
                } finally {
                    client.stop()
                }
            } finally {
                clientDriver.stop()
                server.stop()
            }
        }
    }
}
