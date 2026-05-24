package community.flock.wirespec.lsp

import community.flock.wirespec.lsp.protocol.DidChangeTextDocumentParams
import community.flock.wirespec.lsp.protocol.DidCloseTextDocumentParams
import community.flock.wirespec.lsp.protocol.DidOpenTextDocumentParams
import community.flock.wirespec.lsp.protocol.InitializeResult
import community.flock.wirespec.lsp.protocol.JsonRpcError
import community.flock.wirespec.lsp.protocol.JsonRpcMessage
import community.flock.wirespec.lsp.protocol.PublishDiagnosticsParams
import community.flock.wirespec.lsp.protocol.RenameOptions
import community.flock.wirespec.lsp.protocol.RenameParams
import community.flock.wirespec.lsp.protocol.SemanticTokensLegend
import community.flock.wirespec.lsp.protocol.SemanticTokensOptions
import community.flock.wirespec.lsp.protocol.SemanticTokensParams
import community.flock.wirespec.lsp.protocol.ServerCapabilities
import community.flock.wirespec.lsp.protocol.ServerInfo
import community.flock.wirespec.lsp.protocol.TextDocumentPositionParams
import community.flock.wirespec.lsp.protocol.WorkspaceEdit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class LspServer(private val transport: Transport) {

    private val documents = DocumentStore()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun start() {
        transport.start(::onMessage)
    }

    private fun onMessage(payload: String) {
        val message = runCatching { json.decodeFromString<JsonRpcMessage>(payload) }
            .getOrElse {
                respondError(null, JsonRpcError.PARSE_ERROR, "Parse error: ${it.message}")
                return
            }
        try {
            when {
                message.isRequest -> handleRequest(message)
                message.isNotification -> handleNotification(message)
                else -> { /* responses from client are not expected; ignore */
                }
            }
        } catch (t: Throwable) {
            if (message.id != null) {
                respondError(message.id, JsonRpcError.INTERNAL_ERROR, t.message ?: t::class.simpleName ?: "internal error")
            }
        }
    }

    private fun handleRequest(message: JsonRpcMessage) {
        val id = message.id ?: return
        when (message.method) {
            "initialize" -> respond(id, buildInitializeResult())
            "shutdown" -> respond(id, JsonNull)
            "textDocument/semanticTokens/full",
            "textDocument/semanticTokens/full/delta",
                -> {
                val params = decodeOrNull<SemanticTokensParams>(message.params) ?: run {
                    respondError(id, JsonRpcError.INVALID_PARAMS, "Invalid params for ${message.method}")
                    return
                }
                val doc = documents[params.textDocument.uri]
                if (doc == null) {
                    respond(id, JsonNull)
                } else {
                    respond(id, LanguageService.semanticTokens(doc))
                }
            }

            "textDocument/definition" -> {
                val params = decodeOrNull<TextDocumentPositionParams>(message.params) ?: run {
                    respondError(id, JsonRpcError.INVALID_PARAMS, "Invalid params for textDocument/definition")
                    return
                }
                val doc = documents[params.textDocument.uri]
                if (doc == null) {
                    respond(id, JsonNull)
                } else {
                    respond(id, LanguageService.definition(doc, params.position))
                }
            }

            "textDocument/prepareRename" -> {
                val params = decodeOrNull<TextDocumentPositionParams>(message.params) ?: run {
                    respondError(id, JsonRpcError.INVALID_PARAMS, "Invalid params for textDocument/prepareRename")
                    return
                }
                val doc = documents[params.textDocument.uri]
                val range = doc?.let { LanguageService.prepareRename(it, params.position) }
                if (range == null) respond(id, JsonNull) else respond(id, range)
            }

            "textDocument/rename" -> {
                val params = decodeOrNull<RenameParams>(message.params) ?: run {
                    respondError(id, JsonRpcError.INVALID_PARAMS, "Invalid params for textDocument/rename")
                    return
                }
                val doc = documents[params.textDocument.uri]
                val edit: WorkspaceEdit? = doc?.let { LanguageService.rename(it, params.position, params.newName) }
                if (edit == null) respond(id, JsonNull) else respond(id, edit)
            }

            else -> respondError(id, JsonRpcError.METHOD_NOT_FOUND, "Method not found: ${message.method}")
        }
    }

    private fun handleNotification(message: JsonRpcMessage) {
        when (message.method) {
            "initialized" -> { /* no-op */ }
            "exit" -> { /* let the process loop exit naturally when input closes */ }
            "textDocument/didOpen" -> {
                val params = decodeOrNull<DidOpenTextDocumentParams>(message.params) ?: return
                val doc = documents.open(
                    uri = params.textDocument.uri,
                    version = params.textDocument.version,
                    text = params.textDocument.text,
                )
                publishDiagnostics(doc)
            }
            "textDocument/didChange" -> {
                val params = decodeOrNull<DidChangeTextDocumentParams>(message.params) ?: return
                // Server announces FULL sync, so the last change carries the entire new text.
                val newText = params.contentChanges.lastOrNull()?.text ?: return
                val doc = documents.update(
                    uri = params.textDocument.uri,
                    version = params.textDocument.version ?: 0,
                    text = newText,
                ) ?: documents.open(params.textDocument.uri, params.textDocument.version ?: 0, newText)
                publishDiagnostics(doc)
            }
            "textDocument/didClose" -> {
                val params = decodeOrNull<DidCloseTextDocumentParams>(message.params) ?: return
                documents.close(params.textDocument.uri)
            }
            else -> { /* ignore unknown notifications */ }
        }
    }

    private fun publishDiagnostics(doc: Document) {
        val diagnostics = LanguageService.diagnose(doc)
        val params = PublishDiagnosticsParams(uri = doc.uri, diagnostics = diagnostics)
        sendNotification("textDocument/publishDiagnostics", json.encodeToJsonElement(params))
    }

    private fun buildInitializeResult(): InitializeResult = InitializeResult(
        capabilities = ServerCapabilities(
            semanticTokensProvider = SemanticTokensOptions(
                legend = SemanticTokensLegend(
                    tokenTypes = SemanticTokenLegend.tokenTypes,
                    tokenModifiers = SemanticTokenLegend.tokenModifiers,
                ),
                range = false,
                full = true,
            ),
            definitionProvider = true,
            renameProvider = RenameOptions(prepareProvider = true),
        ),
        serverInfo = ServerInfo(name = "wirespec-lsp"),
    )

    private inline fun <reified T> decodeOrNull(element: JsonElement?): T? = element?.let {
        runCatching { json.decodeFromJsonElement<T>(it) }.getOrNull()
    }

    private inline fun <reified T> respond(id: JsonPrimitive, result: T) {
        val element: JsonElement = if (result is JsonElement) result else json.encodeToJsonElement(result)
        val response = JsonRpcMessage(id = id, result = element)
        transport.send(json.encodeToString(response))
    }

    private fun respondError(id: JsonPrimitive?, code: Int, message: String) {
        val response = JsonRpcMessage(id = id, error = JsonRpcError(code, message))
        transport.send(json.encodeToString(response))
    }

    private fun sendNotification(method: String, params: JsonElement) {
        val message = JsonRpcMessage(method = method, params = params)
        transport.send(json.encodeToString(message))
    }
}

