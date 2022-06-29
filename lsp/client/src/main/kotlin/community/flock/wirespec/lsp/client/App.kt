package community.flock.wirespec.lsp.client

import community.flock.wirespec.lsp.core.WireSpecClient
import org.eclipse.lsp4j.launch.LSPLauncher
import java.net.Socket
import java.util.concurrent.Future
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

fun main() {

    LogManager.getLogManager().reset()
    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
        .apply { level = Level.ALL }

    startClient()

}

private fun startClient() {

    val wireSpecClient = WireSpecClient()

    val socket = Socket("localhost", 8080)
    // Create JSON RPC launcher for HelloLanguageServer instance.
    val launcher = LSPLauncher.createClientLauncher(wireSpecClient, socket.getInputStream(), socket.getOutputStream())

    // Start the listener for JsonRPC
    val startListening: Future<*> = launcher.startListening()

    // Get the computed result from LS.
    startListening.get()

}
