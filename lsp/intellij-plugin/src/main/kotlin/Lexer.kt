package community.flock.wirespec.lsp.intellij_plugin

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.tokenize.types.*
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
            tokens = Wirespec.tokenize(buffer.toString())
                .filterNot { token -> token.type is EndOfProgram }
        }
    }

    override fun getBufferSequence() = buffer

    override fun getState() = 0

    override fun getTokenType() =
        if (index == tokens.size) {
            null
        } else {
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
        .let {
            LexerPosition(it.getStartPos(), state)
        }

    override fun restore(position: IntellijLexerPosition) {}
    override fun getBufferEnd() = buffer.toString().length

    internal class LexerPosition(private val myOffset: Int, private val myState: Int) : IntellijLexerPosition {
        override fun getOffset(): Int {
            return myOffset
        }

        override fun getState(): Int {
            return myState
        }
    }
}

fun Token.Coordinates.getStartPos() = idxAndLength.idx - idxAndLength.length