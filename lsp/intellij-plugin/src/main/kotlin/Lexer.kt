package community.flock.wire_spec.lsp.intellij_plugin

import community.flock.wirespec.compiler.core.WireSpec
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
            tokens = WireSpec.tokenize(buffer.toString())
                .fold(
                    ifLeft = { emptyList() },
                    ifRight = { it.filterNot { token -> token.type is EndOfProgram } }
                )
        }
    }

    override fun getBufferSequence() = buffer

    override fun getState() = 0

    override fun getTokenType() =
        if (index == tokens.size) {
            null
        } else {
            when (tokens[index].type) {
                is WsType -> Types.TYPE
                is Keyword -> Types.KEYWORD
                is RightCurly -> Types.BRACKETS
                is LeftCurly -> Types.BRACKETS
                is Colon -> Types.COLON
                is Comma -> Types.COMMA
                else -> Types.VALUE
            }
        }

    override fun getTokenStart() = tokens[index]
        .coordinates
        .let { it.idxAndLength.idx - it.idxAndLength.length }


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
            val pos = it.idxAndLength.idx - it.idxAndLength.length
            LexerPosition(pos, state)
        }

    override fun restore(position: IntellijLexerPosition) {}
    override fun getBufferEnd() = buffer.toString().length

    internal class LexerPosition(private val myOffset: Int, private val myState: Int) : IntellijLexerPosition {
        override fun getOffset(): Int {
            return myOffset
        }

        override fun getState(): Int {
            return 1
        }
    }
}