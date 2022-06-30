package community.flock.wirespec.lsp.core

import org.eclipse.lsp4j.CompletionOptions
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess


class WireSpecServer(
    private val textDocumentService: TextDocumentService,
    private val workspaceService: WorkspaceService
) : LanguageServer, LanguageClientAware {

    private var client: LanguageClient? = null
    private var errorCode = 1

    override fun initialize(initializeParams: InitializeParams?): CompletableFuture<InitializeResult> =
        CompletableFuture.supplyAsync {
            InitializeResult(ServerCapabilities()).apply {
                // Set the capabilities of the LS to inform the client.
                capabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
                capabilities.completionProvider = CompletionOptions()
            }
        }

    override fun shutdown(): CompletableFuture<Any> {
        errorCode = 0
        return CompletableFuture.completedFuture(Unit)
    }

    override fun exit() {
        // Kill the LS on exit request from client.
        exitProcess(errorCode)
    }

    override fun getTextDocumentService() = textDocumentService

    override fun getWorkspaceService() = workspaceService

    override fun connect(languageClient: LanguageClient) {
        client = languageClient
    }

}
