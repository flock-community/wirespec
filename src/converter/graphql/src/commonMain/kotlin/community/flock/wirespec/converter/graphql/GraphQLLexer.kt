package community.flock.wirespec.converter.graphql

internal sealed interface GraphQLToken {
    val offset: Int

    data class Punctuator(val value: String, override val offset: Int) : GraphQLToken
    data class Name(val value: String, override val offset: Int) : GraphQLToken
    data class IntValue(val raw: String, override val offset: Int) : GraphQLToken
    data class FloatValue(val raw: String, override val offset: Int) : GraphQLToken
    data class StringValue(val value: String, override val offset: Int) : GraphQLToken
    data class EndOfFile(override val offset: Int) : GraphQLToken
}

/**
 * Lexes a GraphQL document following the "Source Text" section of the specification.
 * Ignored tokens (unicode BOM, whitespace, line terminators, commas and comments) are dropped.
 */
internal class GraphQLLexer(private val source: String) {

    private var index = 0

    fun tokenize(): List<GraphQLToken> = buildList {
        do {
            val token = nextToken()
            add(token)
        } while (token !is GraphQLToken.EndOfFile)
    }

    private fun nextToken(): GraphQLToken {
        skipIgnored()
        val start = index
        val char = source.getOrNull(index) ?: return GraphQLToken.EndOfFile(start)
        return when (char) {
            in PUNCTUATORS -> GraphQLToken.Punctuator(char.toString(), start).also { index++ }
            '.' -> when {
                source.startsWith("...", index) -> GraphQLToken.Punctuator("...", start).also { index += 3 }
                else -> fail("Unexpected character '.'", start)
            }
            '"' -> when {
                source.startsWith("\"\"\"", index) -> GraphQLToken.StringValue(readBlockString(), start)
                else -> GraphQLToken.StringValue(readString(), start)
            }
            '-' -> readNumber()
            else -> when {
                char.isAsciiDigit() -> readNumber()
                char.isNameStart() -> GraphQLToken.Name(readName(), start)
                else -> fail("Unexpected character '$char'", start)
            }
        }
    }

    private fun skipIgnored() {
        while (index < source.length) {
            when (source[index]) {
                '﻿', ' ', '\t', '\n', '\r', ',' -> index++
                '#' -> while (index < source.length && source[index] != '\n' && source[index] != '\r') index++
                else -> return
            }
        }
    }

    private fun readName(): String {
        val start = index
        while (index < source.length && source[index].isNameContinue()) index++
        return source.substring(start, index)
    }

    private fun readNumber(): GraphQLToken {
        val start = index
        if (source[index] == '-') index++
        when {
            source.getOrNull(index) == '0' -> {
                index++
                if (source.getOrNull(index)?.isAsciiDigit() == true) fail("Invalid number, unexpected digit after 0", index)
            }
            else -> readDigits()
        }
        val fraction = source.getOrNull(index) == '.'
        if (fraction) {
            index++
            readDigits()
        }
        val exponent = source.getOrNull(index) == 'e' || source.getOrNull(index) == 'E'
        if (exponent) {
            index++
            if (source.getOrNull(index) == '+' || source.getOrNull(index) == '-') index++
            readDigits()
        }
        source.getOrNull(index)
            ?.takeIf { it == '.' || it.isNameStart() }
            ?.let { fail("Invalid number, unexpected character '$it'", index) }
        val raw = source.substring(start, index)
        return if (fraction || exponent) GraphQLToken.FloatValue(raw, start) else GraphQLToken.IntValue(raw, start)
    }

    private fun readDigits() {
        if (source.getOrNull(index)?.isAsciiDigit() != true) fail("Invalid number, expected digit", index)
        while (source.getOrNull(index)?.isAsciiDigit() == true) index++
    }

    private fun readString(): String {
        val start = index
        index++
        return buildString {
            while (true) {
                when (val char = source.getOrNull(index)) {
                    null, '\n', '\r' -> fail("Unterminated string", start)
                    '"' -> {
                        index++
                        return@buildString
                    }
                    '\\' -> readEscape()
                    else -> append(char).also { index++ }
                }
            }
        }
    }

    private fun StringBuilder.readEscape() {
        val start = index
        index += 2
        when (source.getOrNull(start + 1)) {
            '"' -> append('"')
            '\\' -> append('\\')
            '/' -> append('/')
            'b' -> append('\b')
            'f' -> append('\u000C')
            'n' -> append('\n')
            'r' -> append('\r')
            't' -> append('\t')
            'u' -> when (source.getOrNull(index)) {
                '{' -> source.indexOf('}', index)
                    .takeIf { it > index + 1 }
                    ?.let { end -> source.substring(index + 1, end).also { index = end + 1 } }
                    ?.takeIf { hex -> hex.all { it.isHexDigit() } }
                    ?.toIntOrNull(16)
                    ?.takeIf { it <= 0x10FFFF }
                    ?.let { appendCodePoint(it) }
                    ?: fail("Invalid unicode escape sequence", start)
                else -> source.substring(index, minOf(index + 4, source.length))
                    .takeIf { hex -> hex.length == 4 && hex.all { it.isHexDigit() } }
                    ?.toIntOrNull(16)
                    ?.also { index += 4 }
                    ?.let { append(it.toChar()) }
                    ?: fail("Invalid unicode escape sequence", start)
            }
            else -> fail("Invalid escape sequence", start)
        }
    }

    private fun StringBuilder.appendCodePoint(codePoint: Int) {
        when {
            codePoint < 0x10000 -> append(codePoint.toChar())
            else -> (codePoint - 0x10000).let {
                append(((it shr 10) + 0xD800).toChar())
                append(((it and 0x3FF) + 0xDC00).toChar())
            }
        }
    }

    private fun readBlockString(): String {
        val start = index
        index += 3
        val raw = buildString {
            while (true) {
                when {
                    index >= source.length -> fail("Unterminated block string", start)
                    source.startsWith("\"\"\"", index) -> {
                        index += 3
                        return@buildString
                    }
                    source.startsWith("\\\"\"\"", index) -> append("\"\"\"").also { index += 4 }
                    else -> append(source[index]).also { index++ }
                }
            }
        }
        return blockStringValue(raw)
    }

    private fun fail(message: String, offset: Int): Nothing = source.substring(0, minOf(offset, source.length))
        .let { before -> error("$message at line ${before.count { it == '\n' } + 1}, column ${offset - before.lastIndexOf('\n')}") }

    companion object {
        private val PUNCTUATORS = setOf('!', '$', '&', '(', ')', ':', '=', '@', '[', ']', '{', '|', '}')
        private val LINE_TERMINATOR = Regex("\r\n|\n|\r")

        private fun Char.isNameStart() = this == '_' || this in 'A'..'Z' || this in 'a'..'z'

        private fun Char.isNameContinue() = isNameStart() || this in '0'..'9'

        private fun Char.isAsciiDigit() = this in '0'..'9'

        private fun Char.isHexDigit() = isAsciiDigit() || this in 'a'..'f' || this in 'A'..'F'

        private fun String.isWhitespaceOnly() = all { it == ' ' || it == '\t' }

        private fun String.indentation() = takeWhile { it == ' ' || it == '\t' }.length

        /** The `BlockStringValue` algorithm of the specification: strips the common indentation and blank edge lines. */
        fun blockStringValue(raw: String): String = raw.split(LINE_TERMINATOR).let { lines ->
            val commonIndent = lines.drop(1).filterNot { it.isWhitespaceOnly() }.minOfOrNull { it.indentation() }
            lines
                .mapIndexed { i, line -> if (i == 0 || commonIndent == null) line else line.drop(commonIndent) }
                .dropWhile { it.isWhitespaceOnly() }
                .dropLastWhile { it.isWhitespaceOnly() }
                .joinToString("\n")
        }
    }
}
