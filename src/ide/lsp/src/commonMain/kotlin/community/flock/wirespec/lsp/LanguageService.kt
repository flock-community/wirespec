package community.flock.wirespec.lsp

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.lsp.protocol.Diagnostic
import community.flock.wirespec.lsp.protocol.DiagnosticSeverity
import community.flock.wirespec.lsp.protocol.Location
import community.flock.wirespec.lsp.protocol.Position
import community.flock.wirespec.lsp.protocol.Range
import community.flock.wirespec.lsp.protocol.SemanticTokens
import community.flock.wirespec.lsp.protocol.TextEdit
import community.flock.wirespec.lsp.protocol.WorkspaceEdit

data class LspToken(
    val line: Int,
    val character: Int,
    val length: Int,
    val kind: TokenKind,
    val value: String,
) {
    val range: Range get() = Range(Position(line, character), Position(line, character + length))
}

object LanguageService {

    private val parseContext = object : ParseContext, NoLogger {}

    fun diagnose(document: Document): List<Diagnostic> = parseContext
        .parse(nonEmptyListOf(ModuleContent(FileUri(document.uri), document.text)))
        .fold(
            ifLeft = { errors -> errors.map { it.toDiagnostic(document) } },
            ifRight = { emptyList() },
        )

    fun tokenize(document: Document): List<LspToken> = WirespecSpec
        .tokenize(document.text)
        .mapNotNull { token -> token.toLspToken() }

    fun semanticTokens(document: Document): SemanticTokens {
        val data = mutableListOf<Int>()
        var prevLine = 0
        var prevChar = 0
        for (t in tokenize(document)) {
            val deltaLine = t.line - prevLine
            val deltaChar = if (deltaLine == 0) t.character - prevChar else t.character
            data += deltaLine
            data += deltaChar
            data += t.length
            data += t.kind.toSemanticType()
            data += 0 // no modifiers in this legend
            prevLine = t.line
            prevChar = t.character
        }
        return SemanticTokens(data)
    }

    fun definition(document: Document, position: Position): List<Location> {
        val hit = tokenAt(document, position) ?: return emptyList()
        if (hit.kind != TokenKind.USER_TYPE) return emptyList()
        return tokenize(document)
            .filter { it.kind == TokenKind.USER_TYPE && it.value == hit.value }
            .map { Location(uri = document.uri, range = it.range) }
    }

    /**
     * Returns the range of the identifier at [position] if it can be renamed, or null otherwise.
     * Only user-defined type names (PascalCase identifiers introduced by `type` / `enum` / `endpoint`
     * / `channel`) are renameable.
     */
    fun prepareRename(document: Document, position: Position): Range? {
        val hit = tokenAt(document, position) ?: return null
        return if (hit.kind == TokenKind.USER_TYPE) hit.range else null
    }

    /**
     * Returns a [WorkspaceEdit] that renames every occurrence of the user-defined type identifier
     * at [position] to [newName] within [document]. Returns null when the position is not on a
     * renameable token or when [newName] is not a valid Wirespec PascalCase identifier.
     */
    fun rename(document: Document, position: Position, newName: String): WorkspaceEdit? {
        if (!isValidPascalCaseIdentifier(newName)) return null
        val hit = tokenAt(document, position) ?: return null
        if (hit.kind != TokenKind.USER_TYPE) return null
        if (newName == hit.value) return WorkspaceEdit(emptyMap())
        val edits = tokenize(document)
            .filter { it.kind == TokenKind.USER_TYPE && it.value == hit.value }
            .map { TextEdit(range = it.range, newText = newName) }
        if (edits.isEmpty()) return null
        return WorkspaceEdit(changes = mapOf(document.uri to edits))
    }

    private fun tokenAt(document: Document, position: Position): LspToken? = tokenize(document).firstOrNull { t ->
        t.line == position.line && position.character >= t.character && position.character <= t.character + t.length
    }

    private fun Token.toLspToken(): LspToken? {
        val kind = type.toTokenKind() ?: return null
        val length = coordinates.idxAndLength.length
        if (length == 0) return null
        val line = coordinates.line - 1
        // coordinates.position is 1-based and points just past the token; subtract length to reach the start, then -1 for 0-indexing.
        val character = coordinates.position - 1 - length
        if (line < 0 || character < 0) return null
        return LspToken(line = line, character = character, length = length, kind = kind, value = value)
    }

    private fun community.flock.wirespec.compiler.core.exceptions.WirespecException.toDiagnostic(document: Document): Diagnostic {
        val length = coordinates.idxAndLength.length.coerceAtLeast(1)
        val endIdx = coordinates.idxAndLength.idx.coerceAtLeast(0)
        val startIdx = (endIdx - length).coerceAtLeast(0)
        val startPos = document.positionAt(startIdx)
        val endPos = document.positionAt(endIdx)
        return Diagnostic(
            range = Range(startPos, endPos),
            severity = DiagnosticSeverity.ERROR,
            message = message,
            source = "wirespec",
        )
    }
}

private val PASCAL_CASE_REGEX = Regex("^[A-Z][A-Za-z0-9_]*$")

private fun isValidPascalCaseIdentifier(name: String): Boolean = PASCAL_CASE_REGEX.matches(name)
