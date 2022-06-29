package community.flock.wirespec.lsp.core

import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.CodeLensParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

typealias CF<T> = CompletableFuture<T>

class LiveTextDocumentService : TextDocumentService {

    override fun completion(completionParams: CompletionParams): CF<Either<List<CompletionItem>, CompletionList>> =
        future {
            Either.forLeft(listOf(CompletionItem().apply {
                // Define the text to be inserted in to the file if the completion item is selected.
                insertText = "sayHello() {\n    print(\"hello\")\n}"
                // Set the label that shows when the completion dropdown appears in the Editor.
                label = "sayHello()"
                // Set the completion kind. This is a snippet.
                // That means it replace character which trigger the completion and
                // replace it with what defined in inserted text.
                kind = CompletionItemKind.Snippet
                // This will set the details for the snippet code which will help user to
                // understand what this completion item is.
                detail = "sayHello()\n this will say hello to the people"
            }))
        }

    override fun resolveCompletionItem(completionItem: CompletionItem): CF<CompletionItem> = future {
        CompletionItem()
    }

    override fun references(referenceParams: ReferenceParams): CF<List<Location>> = future {
        listOf()
    }

    override fun documentSymbol(documentSymbolParams: DocumentSymbolParams): CF<List<Either<SymbolInformation, DocumentSymbol>>> =
        future { listOf() }

    override fun codeAction(codeActionParams: CodeActionParams): CF<List<Either<Command, CodeAction>>> = future {
        listOf()
    }

    override fun codeLens(codeLensParams: CodeLensParams): CF<List<CodeLens>> = future {
        listOf()
    }

    override fun resolveCodeLens(codeLens: CodeLens): CF<CodeLens> = future {
        CodeLens()
    }

    override fun formatting(documentFormattingParams: DocumentFormattingParams): CF<List<TextEdit>> = future {
        listOf()
    }

    override fun rangeFormatting(documentRangeFormattingParams: DocumentRangeFormattingParams): CF<List<TextEdit>> =
        future {
            listOf()
        }

    override fun onTypeFormatting(documentOnTypeFormattingParams: DocumentOnTypeFormattingParams): CF<List<TextEdit>> =
        future {
            listOf()
        }

    override fun rename(renameParams: RenameParams): CF<WorkspaceEdit> = future {
        WorkspaceEdit()
    }

    override fun didOpen(didOpenTextDocumentParams: DidOpenTextDocumentParams) {}

    override fun didChange(didChangeTextDocumentParams: DidChangeTextDocumentParams) {}

    override fun didClose(didCloseTextDocumentParams: DidCloseTextDocumentParams) {}

    override fun didSave(didSaveTextDocumentParams: DidSaveTextDocumentParams) {}

}

private fun <T> future(async: () -> T) = CF<T>().completeAsync { async() }
