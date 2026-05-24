package community.flock.wirespec.lsp

import arrow.core.NonEmptyList
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

data class LspToken(
    val line: Int,
    val character: Int,
    val length: Int,
    val type: Int,
    val value: String,
)

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
            data += t.type
            data += 0 // no modifiers in this legend
            prevLine = t.line
            prevChar = t.character
        }
        return SemanticTokens(data)
    }

    fun definition(document: Document, position: Position): List<Location> {
        val tokens = tokenize(document)
        val hit = tokens.firstOrNull { t ->
            t.line == position.line && position.character >= t.character && position.character <= t.character + t.length
        } ?: return emptyList()
        if (hit.type != SemanticTokenLegend.TYPE_TYPE) return emptyList()
        return tokens
            .filter { it.value == hit.value }
            .map { t ->
                Location(
                    uri = document.uri,
                    range = Range(
                        start = Position(t.line, t.character),
                        end = Position(t.line, t.character + t.length),
                    ),
                )
            }
    }

    private fun Token.toLspToken(): LspToken? {
        val semanticType = type.toSemanticType() ?: return null
        val length = coordinates.idxAndLength.length
        if (length == 0) return null
        val line = coordinates.line - 1
        // coordinates.position is 1-based and points just past the token; subtract length to reach the start, then -1 for 0-indexing.
        val character = coordinates.position - 1 - length
        if (line < 0 || character < 0) return null
        return LspToken(line = line, character = character, length = length, type = semanticType, value = value)
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

    @Suppress("unused")
    private fun NonEmptyList<community.flock.wirespec.compiler.core.exceptions.WirespecException>.firstError() = first()
}
