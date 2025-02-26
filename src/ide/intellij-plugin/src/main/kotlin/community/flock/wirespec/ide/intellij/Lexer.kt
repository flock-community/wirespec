package community.flock.wirespec.ide.intellij

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Character
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.CustomType
import community.flock.wirespec.compiler.core.tokenize.CustomValue
import community.flock.wirespec.compiler.core.tokenize.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.ImportDefinition
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Literal
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.OptimizeOptions
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.StatusCode
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WhiteSpace
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
        tokens = WirespecSpec.tokenize(buffer.toString(), OptimizeOptions(removeWhitespace = false)).filterNot { it.type is EndOfProgram }
    }

    override fun getBufferSequence() = bufferSequence

    override fun getState() = state

    override fun getTokenType() = when (tokens.getOrNull(index)?.type) {
        null -> null
        is LeftCurly -> Types.LEFT_CURLY
        is RightCurly -> Types.RIGHT_CURLY
        is Colon -> Types.COLON
        is Comma -> Types.COMMA
        is QuestionMark -> Types.QUESTION_MARK
        is Hash -> Types.HASH
        is ForwardSlash -> Types.FORWARD_SLASH
        is Brackets -> Types.BRACKETS
        is CustomValue -> Types.CUSTOM_VALUE
        is Comment -> Types.COMMENT
        is Character -> Types.CHARACTER
        is EndOfProgram -> Types.END_OF_PROGRAM
        is WhiteSpace -> Types.WHITE_SPACE
        is ImportDefinition -> Types.IMPORT_DEF
        is TypeDefinition -> Types.TYPE_DEF
        is EnumTypeDefinition -> Types.ENUM_DEF
        is EndpointDefinition -> Types.ENDPOINT_DEF
        is ChannelDefinition -> Types.CHANNEL_DEF
        is WsString -> Types.STRING
        is WsInteger -> Types.INTEGER
        is WsNumber -> Types.NUMBER
        is WsBoolean -> Types.BOOLEAN
        is WsBytes -> Types.BYTES
        is CustomType -> Types.CUSTOM_TYPE
        is WsUnit -> Types.UNIT
        is Method -> Types.METHOD
        is Path -> Types.PATH
        is StatusCode -> Types.STATUS_CODE
        is Arrow -> Types.ARROW
        is Equals -> Types.EQUALS
        is Pipe -> Types.PIPE
        is Literal -> Types.LITERAL
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
}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
