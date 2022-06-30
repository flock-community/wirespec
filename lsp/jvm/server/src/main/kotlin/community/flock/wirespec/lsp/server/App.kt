package community.flock.wirespec.lsp.server

import community.flock.wirespec.lsp.core.LiveTextDocumentService
import community.flock.wirespec.lsp.core.LiveWorkspaceService
import community.flock.wirespec.lsp.core.WireSpecServer
import org.eclipse.lsp4j.launch.LSPLauncher
import java.net.ServerSocket
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

fun main() {

    LogManager.getLogManager().reset()
    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
        .apply { level = Level.ALL }

    startServer()

}

private fun startServer() {
    val wireSpecServer = WireSpecServer(
        textDocumentService = LiveTextDocumentService(),
        workspaceService = LiveWorkspaceService(),
    )

    val socket = ServerSocket(8080).accept()

    LSPLauncher.createServerLauncher(wireSpecServer, socket.getInputStream(), socket.getOutputStream())
        .also { wireSpecServer.connect(it.remoteProxy) }
        .startListening().get()
}
