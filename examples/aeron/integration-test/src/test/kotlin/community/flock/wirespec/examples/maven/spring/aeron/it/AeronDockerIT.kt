package community.flock.wirespec.examples.maven.spring.aeron.it

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Paths
import java.time.Duration

/**
 * Runs the Kotlin Spring Boot backend and the Rust client together in Docker:
 * the two containers share the server's IPC namespace, so both map the same
 * /dev/shm and exchange Wirespec rpc frames through the backend's embedded
 * Aeron media driver over shared memory — no network between them at all.
 *
 * The client calls the backend (Ping, GetQuote) and then GetWatchlistQuotes,
 * which makes the backend call the client's own GetWatchlist rpc: the reverse,
 * server-to-client direction.
 */
class AeronDockerIT {

    private val exampleRoot = Paths.get("..").toAbsolutePath().normalize()

    // Overridable for environments where Docker Hub is unreachable.
    private val serverBase = System.getenv("AERON_EXAMPLE_SERVER_BASE_IMAGE") ?: "eclipse-temurin:21-jre"
    private val clientBase = System.getenv("AERON_EXAMPLE_CLIENT_BASE_IMAGE") ?: "ubuntu:24.04"

    @Test
    fun `the Rust client and the Kotlin backend exchange rpcs over shared memory`() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable, "Docker is not available; skipping")

        val serverImage = ImageFromDockerfile("wirespec-aeron-server", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.server"))
            .withFileFromPath("server.jar", exampleRoot.resolve("server/target/server.jar"))
            .withBuildArg("BASE_IMAGE", serverBase)
        val clientImage = ImageFromDockerfile("wirespec-aeron-client", true)
            .withFileFromPath("Dockerfile", exampleRoot.resolve("docker/Dockerfile.client"))
            .withFileFromPath("client", exampleRoot.resolve("client/target/release/client"))
            .withBuildArg("BASE_IMAGE", clientBase)

        val server = GenericContainer(serverImage)
            .withCreateContainerCmdModifier { cmd ->
                // The media driver lives in /dev/shm; sharing the IPC namespace
                // gives the client the exact same shared memory.
                cmd.hostConfig!!.withIpcMode("shareable").withShmSize(1L shl 30)
            }
            .waitingFor(Wait.forLogMessage(".*Wirespec Aeron rpc server listening.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2))
        try {
            server.start()
            val client = GenericContainer(clientImage)
                .withCreateContainerCmdModifier { cmd -> cmd.hostConfig!!.withIpcMode("container:${server.containerId}") }
                .withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(Duration.ofMinutes(2)))
            try {
                client.start()
                val logs = client.logs
                listOf(
                    "Ping -> pong",
                    "GetQuote AAPL -> AAPL 178.25 USD",
                    "GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'",
                    "GetWatchlistQuotes -> AAPL 178.25 USD",
                    "GetWatchlistQuotes -> FLCK 42 EUR",
                    "Serving GetWatchlist for the backend",
                ).forEach { expected ->
                    assertTrue(expected in logs) { "Expected client log to contain '$expected'; logs:\n$logs" }
                }
            } finally {
                client.stop()
            }
        } finally {
            server.stop()
        }
    }
}
