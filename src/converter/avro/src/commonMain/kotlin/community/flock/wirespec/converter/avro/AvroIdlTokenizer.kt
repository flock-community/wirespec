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
        val sb = StringBuilder()
        while (pos < source.length) {
            if (source[pos] == '*' && peek(1) == '/') {
                advance()
                advance()
                break
            }
            sb.append(source[pos])
            advance()
        }
        val cleaned = sb.toString()
            .lines()
            .joinToString("\n") { it.trim().removePrefix("*").trim() }
            .trim()
        return AvroIdlToken.DocComment(cleaned, startLine, startColumn)
    }

    private fun readStringLiteral(): AvroIdlToken.StringLiteral {
        val startLine = line
        val startColumn = column
        advance()
        val sb = StringBuilder()
        while (pos < source.length && source[pos] != '"') {
            if (source[pos] == '\\' && pos + 1 < source.length) {
                advance()
                when (val esc = source[pos]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(esc)
                }
                advance()
            } else {
                sb.append(source[pos])
                advance()
            }
        }
        if (pos < source.length) advance()
        return AvroIdlToken.StringLiteral(sb.toString(), startLine, startColumn)
    }

    private fun readIdentifier(): AvroIdlToken.Identifier {
        val startLine = line
        val startColumn = column
        val sb = StringBuilder()
        while (pos < source.length) {
            val c = source[pos]
            if (c.isLetterOrDigit() || c == '_' || c == '.') {
                sb.append(c)
                advance()
            } else {
                break
            }
        }
        return AvroIdlToken.Identifier(sb.toString(), startLine, startColumn)
    }

    private fun readNumber(): AvroIdlToken.NumberLiteral {
        val startLine = line
        val startColumn = column
        val sb = StringBuilder()
        if (source[pos] == '-') {
            sb.append('-')
            advance()
        }
        while (pos < source.length) {
            val c = source[pos]
            if (c.isDigit() || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                sb.append(c)
                advance()
            } else {
                break
            }
        }
        return AvroIdlToken.NumberLiteral(sb.toString(), startLine, startColumn)
    }

    companion object {
        private val SYMBOL_CHARS = setOf('{', '}', '(', ')', '[', ']', '<', '>', ',', ';', '=', '@', '?', ':')
    }
}
