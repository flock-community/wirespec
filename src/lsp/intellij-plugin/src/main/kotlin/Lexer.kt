package community.flock.wirespec.lsp.intellij_plugin

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.Equals
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.Hash
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.Pipe
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsComment
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsNumber
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsUnit
import com.intellij.lexer.LexerBase as IntellijLexer

class Lexer : IntellijLexer() {

    private var buffer: CharSequence = ""
    private var index = 0
    private var state = 0
    private var tokens: List<Token> = emptyList()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.index = 0
        this.state = initialState
        this.tokens = WirespecSpec.tokenize(buffer.toString()).filterNot { it.type is EndOfProgram }

    }

    override fun getBufferSequence() = buffer

    override fun getState() = state

    override fun getTokenType() =
        if (index == tokens.size) null
        else {
            val token = tokens[index]
            when (token.type) {
                is LeftCurly -> Types.LEFT_CURLY
                is RightCurly -> Types.RIGHT_CURLY
                is Colon -> Types.COLON
                is Comma -> Types.COMMA
                is QuestionMark -> Types.QUESTION_MARK
                is Hash -> Types.HASH
                is ForwardSlash -> Types.FORWARD_SLASH
                is Brackets -> Types.BRACKETS
                is CustomValue -> Types.CUSTOM_VALUE
                is WsComment -> Types.COMMENT
                is Invalid -> Types.INVALID
                is EndOfProgram -> Types.END_OF_PROGRAM
                is WhiteSpace -> Types.WHITE_SPACE
                is WsTypeDef -> Types.TYPE_DEF
                is WsEnumTypeDef -> Types.ENUM_DEF
                is WsEndpointDef -> Types.ENDPOINT_DEF
                is WsString -> Types.STRING
                is WsInteger -> Types.INTEGER
                is WsNumber -> Types.NUMBER
                is WsBoolean -> Types.BOOLEAN
                is CustomType -> Types.CUSTOM_TYPE
                is WsUnit -> Types.UNIT
                is Method -> Types.METHOD
                is Path -> Types.PATH
                is StatusCode -> Types.STATUS_CODE
                is Arrow -> Types.ARROW
                is Equals -> Types.EQUALS
                is Pipe -> Types.PIPE
                is CustomRegex -> Types.CUSTOM_REGEX
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
        index++
        state = index
    }

    override fun getBufferEnd() = buffer.toString().length

}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
