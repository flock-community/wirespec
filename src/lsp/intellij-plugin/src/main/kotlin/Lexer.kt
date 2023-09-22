package community.flock.wirespec.lsp.intellij_plugin

import community.flock.wirespec.compiler.core.Wirespec
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
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsRefinedTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import com.intellij.lexer.Lexer as IntellijLexer
import com.intellij.lexer.LexerPosition as IntellijLexerPosition

class Lexer : IntellijLexer() {

    private var buffer: CharSequence = ""
    private var index = 0
    private var tokens: List<Token> = emptyList()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.index = 0
        this.tokens = emptyList()
        if (buffer.isNotEmpty()) {
            tokens = Wirespec.tokenize(buffer.toString()).filterNot { it.type is EndOfProgram }
        }
    }

    override fun getBufferSequence() = buffer

    override fun getState() = 0

    override fun getTokenType() =
        if (index == tokens.size) null
        else {
            val token = tokens[index]
            when (token.type) {
                is WsTypeDef -> Types.TYPE_DEF
                is WhiteSpace -> Types.WHITE_SPACE
                is Brackets -> Types.BRACKETS
                is Colon -> Types.COLON
                is Comma -> Types.COMMA
                is CustomValue -> Types.CUSTOM_VALUE
                is CustomType -> Types.CUSTOM_TYPE
                is WsBoolean -> Types.BOOLEAN
                is WsInteger -> Types.INTEGER
                is WsString -> Types.STRING
                is LeftCurly -> Types.LEFT_CURLY
                is QuestionMark -> Types.QUESTION_MARK
                is RightCurly -> Types.RIGHT_CURLY
                is EndOfProgram -> Types.END_OF_PROGRAM
                is Invalid -> Types.INVALID
                is WsEnumTypeDef -> Types.ENUM_TYPE_DEF
                is WsRefinedTypeDef -> Types.REFINED_TYPE_DEF
                is WsEndpointDef -> Types.ENDPOINT_TYPE_DEF
                is CustomRegex -> Types.CUSTOM_REGEX
                is Arrow -> Types.ARROW
                is Method -> Types.METHOD
                is Path -> Types.PATH
                is StatusCode -> Types.STATUS_CODE
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
    }

    override fun getCurrentPosition(): IntellijLexerPosition = tokens[index]
        .coordinates
        .run { LexerPosition(getStartPos(), state) }

    override fun restore(position: IntellijLexerPosition) {}

    override fun getBufferEnd() = buffer.toString().length

    internal class LexerPosition(private val myOffset: Int, private val myState: Int) : IntellijLexerPosition {
        override fun getOffset() = myOffset

        override fun getState() = myState
    }
}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length
