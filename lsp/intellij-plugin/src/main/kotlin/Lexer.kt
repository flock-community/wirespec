package community.flock.wire_spec.lsp.intellij_plugin

import com.intellij.lexer.Lexer as IntellijLexer
import com.intellij.lexer.LexerPosition
import com.intellij.psi.tree.IElementType
import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.tokenize.types.*

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

    override fun getBufferSequence(): CharSequence {
        return buffer
    }

    override fun getState(): Int {
        return 0
    }

    override fun getTokenType(): IElementType? {
        return if (index == tokens.size) {
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
    }

    override fun getTokenStart(): Int {
        val (_, _, coordinates) = tokens[index]
        return coordinates.idxAndLength.idx - coordinates.idxAndLength.length
    }

    override fun getTokenEnd(): Int {
        val (_, _, coordinates) = tokens[index]
        return coordinates.idxAndLength.idx
    }

    override fun advance() {
        index++
    }

    override fun getCurrentPosition(): LexerPosition {
        val (_, _, coordinates) = tokens[index]
        val pos = coordinates.idxAndLength.idx - coordinates.idxAndLength.length
        return LexerPositionImpl(pos, state)
    }

    override fun restore(position: LexerPosition) {}
    override fun getBufferEnd(): Int {
        return buffer.toString().length
    }

    internal class LexerPositionImpl(private val myOffset: Int, private val myState: Int) : LexerPosition {
        override fun getOffset(): Int {
            return myOffset
        }

        override fun getState(): Int {
            return 1
        }
    }
}