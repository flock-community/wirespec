package community.flock.wirespec.compiler.parse

import community.flock.wirespec.WireSpecException.CompilerException.ParserException.NullTokenException.NextException
import community.flock.wirespec.WireSpecException.CompilerException.ParserException.NullTokenException.StartingException
import community.flock.wirespec.compiler.tokenize.Token
import community.flock.wirespec.compiler.utils.log

class TokenProvider(private val tokenIterator: Iterator<Token>) {

    var token = nextToken() ?: throw StartingException()
    var nextToken = nextToken()

    init {
        printTokens()
    }

    fun hasNext() = nextToken != null

    fun eatToken() {
        val previousToken = token
        token = nextToken ?: throw NextException()
        nextToken = nextToken()

        printTokens(previousToken)
    }

    private fun printTokens(previousToken: Token? = null) = run {
        val prev = previousToken?.run { "Eating: '$value', " } ?: ""
        val curr = token.run { "Current: '$value'" }
        val next = nextToken?.run { ", Next: '$value'" } ?: ""
        log("$prev$curr$next")
    }

    private fun nextToken() = runCatching { tokenIterator.next() }.getOrNull()
}

fun List<Token>.toProvider() = TokenProvider(iterator())
