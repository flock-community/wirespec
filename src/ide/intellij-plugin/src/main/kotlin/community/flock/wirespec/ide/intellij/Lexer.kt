package community.flock.wirespec.ide.intellij

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.Annotation
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Character
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.LeftBracket
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.LiteralString
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.Number
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RegExp
import community.flock.wirespec.compiler.core.tokenize.RightBracket
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TokenizeOptions
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier
import community.flock.wirespec.compiler.core.tokenize.Underscore
import community.flock.wirespec.compiler.core.tokenize.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit
import community.flock.wirespec.compiler.core.tokenize.tokenize
import com.intellij.lexer.LexerBase as IntellijLexer

class Lexer : IntellijLexer() {

    private var bufferSequence: CharSequence = ""
    private var index = 0
    private var state = 0
    private var tokens: List<Token> = emptyList()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        bufferSequence = buffer
        index = 0
        state = initialState
        tokens = WirespecSpec.tokenize(buffer.toString(), TokenizeOptions(removeWhitespace = false)).filterNot { it.type is EndOfProgram }
    }

    override fun getBufferSequence() = bufferSequence

    override fun getState() = state

    override fun getTokenType() = when (val type = tokens.getOrNull(index)?.type) {
        null -> null
        is WhiteSpace -> Types.WHITE_SPACE
        is WirespecIdentifier -> Types.WIRESPEC_IDENTIFIER
        is TypeIdentifier -> Types.TYPE_IDENTIFIER
        is WsInteger -> Types.WS_INTEGER
        is WsNumber -> Types.WS_NUMBER
        else -> SIMPLE_TOKEN_TYPES[type]
    }

    override fun getTokenStart() = tokens[index]
        .coordinates
        .getStartPos()

    override fun getTokenEnd() = tokens[index]
        .coordinates
        .idxAndLength
        .idx

    override fun advance() {
        state = ++index
    }

    override fun getBufferEnd() = bufferSequence.toString().length

    companion object {
        private val SIMPLE_TOKEN_TYPES: Map<TokenType, Types.ElementType> = mapOf(
            LeftCurly to Types.LEFT_CURLY,
            RightCurly to Types.RIGHT_CURLY,
            Colon to Types.COLON,
            Comma to Types.COMMA,
            QuestionMark to Types.QUESTION_MARK,
            Hash to Types.HASH,
            Annotation to Types.ANNOTATION,
            ForwardSlash to Types.FORWARD_SLASH,
            Brackets to Types.BRACKETS,
            LeftBracket to Types.LEFT_BRACKET,
            RightBracket to Types.RIGHT_BRACKET,
            Comment to Types.COMMENT,
            Character to Types.CHARACTER,
            EndOfProgram to Types.END_OF_PROGRAM,
            TypeDefinition to Types.TYPE_DEF,
            EnumTypeDefinition to Types.ENUM_DEF,
            EndpointDefinition to Types.ENDPOINT_DEF,
            ChannelDefinition to Types.CHANNEL_DEF,
            WsString to Types.WS_STRING,
            WsBoolean to Types.WS_BOOLEAN,
            WsBytes to Types.WS_BYTES,
            WsUnit to Types.UNIT,
            Method to Types.METHOD,
            Path to Types.PATH,
            Arrow to Types.ARROW,
            Equals to Types.EQUALS,
            Pipe to Types.PIPE,
            Integer to Types.INTEGER,
            Number to Types.NUMBER,
            LeftParenthesis to Types.LEFT_PARENTHESES,
            RightParenthesis to Types.RIGHT_PARENTHESES,
            RegExp to Types.REG_EXP,
            Underscore to Types.UNDERSCORE,
            LiteralString to Types.LITERAL_STRING,
        )
    }
}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
