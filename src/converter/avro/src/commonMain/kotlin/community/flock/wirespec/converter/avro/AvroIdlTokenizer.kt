package community.flock.wirespec.converter.avro

internal sealed interface AvroIdlToken {
    val line: Int
    val column: Int

    data class Identifier(val value: String, override val line: Int, override val column: Int) : AvroIdlToken
    data class StringLiteral(val value: String, override val line: Int, override val column: Int) : AvroIdlToken
    data class NumberLiteral(val value: String, override val line: Int, override val column: Int) : AvroIdlToken
    data class DocComment(val value: String, override val line: Int, override val column: Int) : AvroIdlToken
    data class Symbol(val value: Char, override val line: Int, override val column: Int) : AvroIdlToken
}

internal class AvroIdlTokenizer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var column = 1

    fun tokenize(): List<AvroIdlToken> {
        val tokens = mutableListOf<AvroIdlToken>()
        while (pos < source.length) {
            val c = source[pos]
            when {
                c.isWhitespace() -> advance()
                c == '/' && peek(1) == '*' && peek(2) == '*' -> tokens.add(readDocComment())
                c == '/' && peek(1) == '*' -> skipBlockComment()
                c == '/' && peek(1) == '/' -> skipLineComment()
                c == '"' -> tokens.add(readStringLiteral())
                c.isLetter() || c == '_' -> tokens.add(readIdentifier())
                c.isDigit() || (c == '-' && peek(1)?.isDigit() == true) -> tokens.add(readNumber())
                c in SYMBOL_CHARS -> {
                    tokens.add(AvroIdlToken.Symbol(c, line, column))
                    advance()
                }
                else -> error("Unexpected character '$c' at line $line column $column")
            }
        }
        return tokens
    }

    private fun peek(offset: Int): Char? = if (pos + offset < source.length) source[pos + offset] else null

    private fun advance() {
        if (source[pos] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        pos++
    }

    private fun skipLineComment() {
        while (pos < source.length && source[pos] != '\n') advance()
    }

    private fun skipBlockComment() {
        advance()
        advance()
        while (pos < source.length) {
            if (source[pos] == '*' && peek(1) == '/') {
                advance()
                advance()
                return
            }
            advance()
        }
    }

    private fun readDocComment(): AvroIdlToken.DocComment {
        val startLine = line
        val startColumn = column
        advance()
        advance()
        advance()
        val contentStart = pos
        var contentEnd = pos
        while (pos < source.length) {
            if (source[pos] == '*' && peek(1) == '/') {
                contentEnd = pos
                advance()
                advance()
                break
            }
            contentEnd = pos + 1
            advance()
        }
        val cleaned = source.substring(contentStart, contentEnd)
            .lines()
            .joinToString("\n") { it.trim().removePrefix("*").trim() }
            .trim()
        return AvroIdlToken.DocComment(cleaned, startLine, startColumn)
    }

    private fun readStringLiteral(): AvroIdlToken.StringLiteral {
        val startLine = line
        val startColumn = column
        advance()
        var value = ""
        while (pos < source.length && source[pos] != '"') {
            if (source[pos] == '\\' && pos + 1 < source.length) {
                advance()
                value += when (val esc = source[pos]) {
                    'n' -> "\n"
                    't' -> "\t"
                    'r' -> "\r"
                    '"' -> "\""
                    '\\' -> "\\"
                    else -> "$esc"
                }
                advance()
            } else {
                value += source[pos]
                advance()
            }
        }
        if (pos < source.length) advance()
        return AvroIdlToken.StringLiteral(value, startLine, startColumn)
    }

    private fun readIdentifier(): AvroIdlToken.Identifier {
        val startLine = line
        val startColumn = column
        val start = pos
        while (pos < source.length) {
            val c = source[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '.') {
                advance()
            } else {
                break
            }
        }
        return AvroIdlToken.Identifier(source.substring(start, pos), startLine, startColumn)
    }

    private fun readNumber(): AvroIdlToken.NumberLiteral {
        val startLine = line
        val startColumn = column
        val start = pos
        if (source[pos] == '-') advance()
        while (pos < source.length) {
            val c = source[pos]
            if (c.isDigit() || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                advance()
            } else {
                break
            }
        }
        return AvroIdlToken.NumberLiteral(source.substring(start, pos), startLine, startColumn)
    }

    companion object {
        private val SYMBOL_CHARS = setOf('{', '}', '(', ')', '[', ']', '<', '>', ',', ';', '=', '@', '?', ':')
    }
}
