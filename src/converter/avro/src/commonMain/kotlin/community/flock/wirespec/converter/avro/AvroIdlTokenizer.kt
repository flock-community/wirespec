package community.flock.wirespec.converter.avro

internal sealed interface AvroIdlToken {
    data class Identifier(val value: String) : AvroIdlToken
    data class StringLiteral(val value: String) : AvroIdlToken
    data class NumberLiteral(val value: String) : AvroIdlToken
    data class DocComment(val value: String) : AvroIdlToken
    data class Symbol(val value: Char) : AvroIdlToken
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
                    tokens.add(AvroIdlToken.Symbol(c))
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
        return AvroIdlToken.DocComment(cleaned)
    }

    private fun readStringLiteral(): AvroIdlToken.StringLiteral {
        advance()
        val parts = mutableListOf<String>()
        while (pos < source.length && source[pos] != '"') {
            if (source[pos] == '\\' && pos + 1 < source.length) {
                advance()
                parts += when (val esc = source[pos]) {
                    'n' -> "\n"
                    't' -> "\t"
                    'r' -> "\r"
                    '"' -> "\""
                    '\\' -> "\\"
                    else -> "$esc"
                }
                advance()
            } else {
                parts += "${source[pos]}"
                advance()
            }
        }
        if (pos < source.length) advance()
        return AvroIdlToken.StringLiteral(parts.joinToString(""))
    }

    private fun readIdentifier(): AvroIdlToken.Identifier {
        val start = pos
        while (pos < source.length) {
            val c = source[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '.') {
                advance()
            } else {
                break
            }
        }
        return AvroIdlToken.Identifier(source.substring(start, pos))
    }

    private fun readNumber(): AvroIdlToken.NumberLiteral {
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
        return AvroIdlToken.NumberLiteral(source.substring(start, pos))
    }

    companion object {
        private val SYMBOL_CHARS = setOf('{', '}', '(', ')', '[', ']', '<', '>', ',', ';', '=', '@', '?', ':')
    }
}
