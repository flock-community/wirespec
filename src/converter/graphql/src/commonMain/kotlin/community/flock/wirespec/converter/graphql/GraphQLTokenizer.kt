package community.flock.wirespec.converter.graphql

internal sealed interface GraphQLToken {
    data class Name(val value: String) : GraphQLToken
    data class StringValue(val value: String, val block: Boolean) : GraphQLToken
    data class IntValue(val value: String) : GraphQLToken
    data class FloatValue(val value: String) : GraphQLToken
    data class Punctuator(val value: String) : GraphQLToken
}

internal class GraphQLTokenizer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var column = 1

    fun tokenize(): List<GraphQLToken> {
        val tokens = mutableListOf<GraphQLToken>()
        while (pos < source.length) {
            val c = source[pos]
            when {
                // Commas are insignificant, like whitespace, per the GraphQL spec.
                c.isWhitespace() || c == ',' -> advance()
                c == '#' -> skipLineComment()
                c == '"' && peek(1) == '"' && peek(2) == '"' -> tokens.add(readBlockString())
                c == '"' -> tokens.add(readString())
                c.isLetter() || c == '_' -> tokens.add(readName())
                c.isDigit() || (c == '-' && peek(1)?.isDigit() == true) -> tokens.add(readNumber())
                c == '.' && peek(1) == '.' && peek(2) == '.' -> {
                    tokens.add(GraphQLToken.Punctuator("..."))
                    repeat(3) { advance() }
                }

                c in PUNCTUATOR_CHARS -> {
                    tokens.add(GraphQLToken.Punctuator("$c"))
                    advance()
                }

                else -> error("Unexpected character '$c' at line $line column $column")
            }
        }
        return tokens
    }

    private fun peek(offset: Int): Char? = source.getOrNull(pos + offset)

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

    private fun readName(): GraphQLToken.Name {
        val start = pos
        while (pos < source.length && (source[pos].isLetterOrDigit() || source[pos] == '_')) advance()
        return GraphQLToken.Name(source.substring(start, pos))
    }

    private fun readNumber(): GraphQLToken {
        val start = pos
        if (source[pos] == '-') advance()
        while (pos < source.length && source[pos].isDigit()) advance()
        var isFloat = false
        if (pos < source.length && source[pos] == '.') {
            isFloat = true
            advance()
            while (pos < source.length && source[pos].isDigit()) advance()
        }
        if (pos < source.length && (source[pos] == 'e' || source[pos] == 'E')) {
            isFloat = true
            advance()
            if (pos < source.length && (source[pos] == '+' || source[pos] == '-')) advance()
            while (pos < source.length && source[pos].isDigit()) advance()
        }
        val text = source.substring(start, pos)
        return if (isFloat) GraphQLToken.FloatValue(text) else GraphQLToken.IntValue(text)
    }

    private fun readString(): GraphQLToken.StringValue {
        advance()
        val builder = StringBuilder()
        while (pos < source.length && source[pos] != '"') {
            if (source[pos] == '\\' && pos + 1 < source.length) {
                advance()
                builder.append(
                    when (val esc = source[pos]) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'b' -> '\b'
                        '"' -> '"'
                        '\\' -> '\\'
                        '/' -> '/'
                        else -> esc
                    },
                )
                advance()
            } else {
                builder.append(source[pos])
                advance()
            }
        }
        if (pos < source.length) advance()
        return GraphQLToken.StringValue(builder.toString(), block = false)
    }

    private fun readBlockString(): GraphQLToken.StringValue {
        repeat(3) { advance() }
        val start = pos
        var end = pos
        while (pos < source.length) {
            if (source[pos] == '"' && peek(1) == '"' && peek(2) == '"') {
                end = pos
                repeat(3) { advance() }
                break
            }
            end = pos + 1
            advance()
        }
        val raw = source.substring(start, end)
        return GraphQLToken.StringValue(raw.trimBlockString(), block = true)
    }

    // Common-indent stripping per the GraphQL spec's block string semantics.
    private fun String.trimBlockString(): String {
        val lines = lines()
        val commonIndent = lines.drop(1)
            .filter { it.isNotBlank() }
            .minOfOrNull { it.takeWhile(Char::isWhitespace).length }
            ?: 0
        return (listOf(lines.first()) + lines.drop(1).map { it.drop(commonIndent) })
            .joinToString("\n")
            .trim('\n', ' ')
    }

    companion object {
        private val PUNCTUATOR_CHARS = setOf('!', '$', '&', '(', ')', ':', '=', '@', '[', ']', '{', '}', '|')
    }
}
