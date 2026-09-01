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
 * Runs the Kotlin Spring Boot backend, the Rust client and the TypeScript
 * client together in Docker, connected over the network: every channel is an
 * `aeron:udp` endpoint on the container network, and the payloads are CBOR.
 * The backend embeds its media driver; each client is a pod of two containers
 * sharing one IPC namespace — a media-driver sidecar (the pod's network
 * identity) and the client attached to it over /dev/shm.
 *
 * The Rust client calls the backend (Ping, GetQuote) and then
 * GetWatchlistQuotes, which makes the backend call the client's own
 * GetWatchlist rpc: the reverse, server-to-client direction — over UDP both
 * ways. It then keeps serving while the TypeScript client drives the same
 * loop, closing a three-language triangle: TypeScript -> Kotlin -> Rust ->
 * Kotlin -> TypeScript.
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
        val tsClientImage = ImageFromDockerfile("wirespec-aeron-client-ts", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.client-ts"))
            .withFileFromPath("node", exampleRoot.resolve("client-ts/target/docker/node"))
            .withFileFromPath("libaeron.so", exampleRoot.resolve("client-ts/target/docker/libaeron.so"))
            .withFileFromPath("app", exampleRoot.resolve("client-ts/target/docker/app"))
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
            // Each client's own media driver: the pod's network identity, doing the UDP I/O.
            fun driver(alias: String): GenericContainer<*> = GenericContainer(driverImage)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withCreateContainerCmdModifier { cmd -> cmd.hostConfig!!.withIpcMode("shareable").withShmSize(1L shl 30) }
                .waitingFor(Wait.forLogMessage(".*wirespec aeron driver starting.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2))

            // Join the sidecar's IPC namespace (the driver's /dev/shm)
            // and its network namespace (one pod, one address).
            fun GenericContainer<*>.inPodOf(sidecar: GenericContainer<*>): GenericContainer<*> =
                withCreateContainerCmdModifier { cmd ->
                    cmd.hostConfig!!
                        .withIpcMode("container:${sidecar.containerId}")
                        .withNetworkMode("container:${sidecar.containerId}")
                }

            val clientDriver = driver("client")
            val tsClientDriver = driver("tsclient")
            try {
                server.start()
                clientDriver.start()
                // The Rust client runs its calls, then keeps serving GetWatchlist
                // so the backend can answer the TypeScript client's loop too.
                val client = GenericContainer(clientImage)
                    .withEnv("REQUEST_CHANNEL", "aeron:udp?endpoint=server:40123")
                    .withEnv("REPLY_CHANNEL", "aeron:udp?endpoint=client:40124")
                    .withEnv("SERVE_CHANNEL", "aeron:udp?endpoint=client:40125")
                    .withEnv("LINGER_SECONDS", "300")
                    .inPodOf(clientDriver)
                    .waitingFor(Wait.forLogMessage(".*Client calls done; serving GetWatchlist.*", 1))
                    .withStartupTimeout(Duration.ofMinutes(3))
                try {
                    runCatching { client.start() }
                        .onFailure { throw AssertionError("Client run failed; client logs:\n${runCatching { client.logs }.getOrNull()}\nserver logs:\n${server.logs}\ndriver logs:\n${clientDriver.logs}", it) }
                    listOf(
                        "Ping -> pong",
                        "GetQuote AAPL -> AAPL 178.25 USD on XNAS (New York), prev 176.1 USD, history 2",
                        "GetQuote FLCK -> FLCK 42 EUR on XAMS (Amsterdam), prev n/a, history 1",
                        "GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'",
                        "GetWatchlistQuotes -> AAPL 178.25 USD on XNAS",
                        "GetWatchlistQuotes -> FLCK 42 EUR on XAMS",
                        "Serving GetWatchlist for the backend",
                    ).forEach { expected ->
                        assertTrue(expected in client.logs) { "Expected client log to contain '$expected'; client logs:\n${client.logs}\nserver logs:\n${server.logs}" }
                    }

                    tsClientDriver.start()
                    val tsClient = GenericContainer(tsClientImage)
                        .withEnv("REQUEST_CHANNEL", "aeron:udp?endpoint=server:40123")
                        .withEnv("REPLY_CHANNEL", "aeron:udp?endpoint=tsclient:40124")
                        .inPodOf(tsClientDriver)
                        .withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)))
                    try {
                        runCatching { tsClient.start() }
                            .onFailure { throw AssertionError("TypeScript client run failed; ts client logs:\n${runCatching { tsClient.logs }.getOrNull()}\nserver logs:\n${server.logs}\nts driver logs:\n${tsClientDriver.logs}", it) }
                        listOf(
                            "Ping -> pong",
                            "GetQuote AAPL -> AAPL 178.25 USD on XNAS (New York), prev 176.1 USD, history 2",
                            "GetQuote FLCK -> FLCK 42 EUR on XAMS (Amsterdam), prev n/a, history 1",
                            "GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'",
                            // Kotlin backend -> Rust client -> back here: three languages, one loop.
                            "GetWatchlistQuotes -> AAPL 178.25 USD on XNAS",
                            "GetWatchlistQuotes -> FLCK 42 EUR on XAMS",
                        ).forEach { expected ->
                            assertTrue(expected in tsClient.logs) { "Expected ts client log to contain '$expected'; ts client logs:\n${tsClient.logs}\nserver logs:\n${server.logs}\nrust client logs:\n${client.logs}" }
                        }
                    } finally {
                        tsClient.stop()
                    }
                } finally {
                    client.stop()
                }
            } finally {
                tsClientDriver.stop()
                clientDriver.stop()
                server.stop()
            }
        }
    }
}
