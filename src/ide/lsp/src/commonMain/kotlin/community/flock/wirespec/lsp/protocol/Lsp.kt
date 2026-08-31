package community.flock.wirespec.lsp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class Position(val line: Int, val character: Int)

@Serializable
internal data class Range(val start: Position, val end: Position)

@Serializable
internal data class Location(val uri: String, val range: Range)

@Serializable
internal data class TextDocumentIdentifier(val uri: String)

@Serializable
internal data class VersionedTextDocumentIdentifier(
    val uri: String,
    val version: Int? = null,
)

@Serializable
internal data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String,
)

@Serializable
internal data class TextDocumentContentChangeEvent(
    val range: Range? = null,
    val text: String,
)

@Serializable
internal data class DidOpenTextDocumentParams(val textDocument: TextDocumentItem)

@Serializable
internal data class DidChangeTextDocumentParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: List<TextDocumentContentChangeEvent>,
)

@Serializable
internal data class DidCloseTextDocumentParams(val textDocument: TextDocumentIdentifier)

@Serializable
internal data class TextDocumentPositionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

@Serializable
internal data class Diagnostic(
    val range: Range,
    val severity: Int? = null,
    val code: String? = null,
    val source: String? = null,
    val message: String,
)

internal object DiagnosticSeverity {
    const val ERROR = 1
    const val WARNING = 2
    const val INFORMATION = 3
    const val HINT = 4
}

@Serializable
internal data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>,
)

@Serializable
internal data class SemanticTokensLegend(
    val tokenTypes: List<String>,
    val tokenModifiers: List<String>,
)

@Serializable
internal data class SemanticTokensOptions(
    val legend: SemanticTokensLegend,
    val range: Boolean = false,
    val full: Boolean = true,
)

@Serializable
internal data class SemanticTokensParams(val textDocument: TextDocumentIdentifier)

@Serializable
internal data class SemanticTokens(val data: List<Int>)

@Serializable
private data class InitializeParams(
    val processId: Int? = null,
    val rootUri: String? = null,
    val capabilities: JsonElement? = null,
)

@Serializable
internal data class TextDocumentSyncOptions(
    val openClose: Boolean = true,
    val change: Int = TextDocumentSyncKind.FULL,
)

private object TextDocumentSyncKind {
    const val NONE = 0
    const val FULL = 1
    const val INCREMENTAL = 2
}

@Serializable
internal data class RenameOptions(val prepareProvider: Boolean = false)

@Serializable
internal data class RenameParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val newName: String,
)

@Serializable
internal data class TextEdit(val range: Range, val newText: String)

@Serializable
internal data class WorkspaceEdit(val changes: Map<String, List<TextEdit>>)

@Serializable
internal data class ServerCapabilities(
    val textDocumentSync: TextDocumentSyncOptions = TextDocumentSyncOptions(),
    val semanticTokensProvider: SemanticTokensOptions? = null,
    val definitionProvider: Boolean = false,
    val renameProvider: RenameOptions? = null,
)

@Serializable
internal data class InitializeResult(
    val capabilities: ServerCapabilities,
    val serverInfo: ServerInfo? = null,
)

@Serializable
internal data class ServerInfo(
    val name: String,
    val version: String? = null,
)
