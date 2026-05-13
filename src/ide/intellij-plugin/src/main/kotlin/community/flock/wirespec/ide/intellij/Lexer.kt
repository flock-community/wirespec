package community.flock.wirespec.ide.intellij

import com.intellij.psi.tree.IElementType
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
import kotlin.reflect.KClass
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

    override fun getTokenType(): IElementType? {
        val type = tokens.getOrNull(index)?.type ?: return null
        // Sealed-interface checks first; concrete `data object` token types fall through to the map.
        return when (type) {
            is WirespecIdentifier -> Types.WIRESPEC_IDENTIFIER
            is TypeIdentifier -> Types.TYPE_IDENTIFIER
            is WhiteSpace -> Types.WHITE_SPACE
            else -> tokenTypeByClass[type::class]
        }
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

    private companion object {
        private val tokenTypeByClass: Map<KClass<out TokenType>, IElementType> = mapOf(
            LeftCurly::class to Types.LEFT_CURLY,
            RightCurly::class to Types.RIGHT_CURLY,
            Colon::class to Types.COLON,
            Comma::class to Types.COMMA,
            QuestionMark::class to Types.QUESTION_MARK,
            Hash::class to Types.HASH,
            Annotation::class to Types.ANNOTATION,
            ForwardSlash::class to Types.FORWARD_SLASH,
            Brackets::class to Types.BRACKETS,
            LeftBracket::class to Types.LEFT_BRACKET,
            RightBracket::class to Types.RIGHT_BRACKET,
            Comment::class to Types.COMMENT,
            Character::class to Types.CHARACTER,
            EndOfProgram::class to Types.END_OF_PROGRAM,
            TypeDefinition::class to Types.TYPE_DEF,
            EnumTypeDefinition::class to Types.ENUM_DEF,
            EndpointDefinition::class to Types.ENDPOINT_DEF,
            ChannelDefinition::class to Types.CHANNEL_DEF,
            WsString::class to Types.WS_STRING,
            WsInteger::class to Types.WS_INTEGER,
            WsNumber::class to Types.WS_NUMBER,
            WsBoolean::class to Types.WS_BOOLEAN,
            WsBytes::class to Types.WS_BYTES,
            WsUnit::class to Types.UNIT,
            Method::class to Types.METHOD,
            Path::class to Types.PATH,
            Arrow::class to Types.ARROW,
            Equals::class to Types.EQUALS,
            Pipe::class to Types.PIPE,
            Integer::class to Types.INTEGER,
            Number::class to Types.NUMBER,
            RightParenthesis::class to Types.RIGHT_PARENTHESES,
            LeftParenthesis::class to Types.LEFT_PARENTHESES,
            RegExp::class to Types.REG_EXP,
            Underscore::class to Types.UNDERSCORE,
            LiteralString::class to Types.LITERAL_STRING,
        )
    }
}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
