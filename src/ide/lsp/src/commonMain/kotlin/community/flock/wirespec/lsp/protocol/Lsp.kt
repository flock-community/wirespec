package community.flock.wirespec.lsp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Position(val line: Int, val character: Int)

@Serializable
data class Range(val start: Position, val end: Position)

@Serializable
data class Location(val uri: String, val range: Range)

@Serializable
data class TextDocumentIdentifier(val uri: String)

@Serializable
data class VersionedTextDocumentIdentifier(
    val uri: String,
    val version: Int? = null,
)

@Serializable
data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String,
)

@Serializable
data class TextDocumentContentChangeEvent(
    val range: Range? = null,
    val text: String,
)

@Serializable
data class DidOpenTextDocumentParams(val textDocument: TextDocumentItem)

@Serializable
data class DidChangeTextDocumentParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: List<TextDocumentContentChangeEvent>,
)

@Serializable
data class DidCloseTextDocumentParams(val textDocument: TextDocumentIdentifier)

@Serializable
data class TextDocumentPositionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

@Serializable
data class Diagnostic(
    val range: Range,
    val severity: Int? = null,
    val code: String? = null,
    val source: String? = null,
    val message: String,
)

object DiagnosticSeverity {
    const val ERROR = 1
    const val WARNING = 2
    const val INFORMATION = 3
    const val HINT = 4
}

@Serializable
data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>,
)

@Serializable
data class SemanticTokensLegend(
    val tokenTypes: List<String>,
    val tokenModifiers: List<String>,
)

@Serializable
data class SemanticTokensOptions(
    val legend: SemanticTokensLegend,
    val range: Boolean = false,
    val full: Boolean = true,
)

@Serializable
data class SemanticTokensParams(val textDocument: TextDocumentIdentifier)

@Serializable
data class SemanticTokens(val data: List<Int>)

@Serializable
data class InitializeParams(
    val processId: Int? = null,
    val rootUri: String? = null,
    val capabilities: JsonElement? = null,
)

@Serializable
data class TextDocumentSyncOptions(
    val openClose: Boolean = true,
    val change: Int = TextDocumentSyncKind.FULL,
)

object TextDocumentSyncKind {
    const val NONE = 0
    const val FULL = 1
    const val INCREMENTAL = 2
}

@Serializable
data class ServerCapabilities(
    val textDocumentSync: TextDocumentSyncOptions = TextDocumentSyncOptions(),
    val semanticTokensProvider: SemanticTokensOptions? = null,
    val definitionProvider: Boolean = false,
)

@Serializable
data class InitializeResult(
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo? = null,
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String? = null,
)
