package community.flock.wirespec.lsp.core

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class LiveWorkspaceService : WorkspaceService {

    override fun symbol(workspaceSymbolParams: WorkspaceSymbolParams): CompletableFuture<Either<List<SymbolInformation>, List<WorkspaceSymbol>>> =
        CompletableFuture<Either<List<SymbolInformation>, List<WorkspaceSymbol>>>().completeAsync {
            Either.forRight(listOf())
        }

    override fun didChangeConfiguration(didChangeConfigurationParams: DidChangeConfigurationParams) {}

    override fun didChangeWatchedFiles(didChangeWatchedFilesParams: DidChangeWatchedFilesParams) {}

}
