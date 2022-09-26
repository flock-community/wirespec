package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.NullTokenException.NextException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.NullTokenException.StartingException
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.utils.Logger

class TokenProvider(private val logger: Logger, private val tokenIterator: Iterator<Token>) {

    var token = nextToken() ?: throw StartingException()
    var nextToken = nextToken()

    init {
        printTokens()
    }

    fun hasNext() = nextToken != null

    fun eatToken(): Token {
        val previousToken = token
        token = nextToken ?: throw NextException(previousToken.coordinates)
        nextToken = nextToken()
        printTokens(previousToken)
        return previousToken
    }

    private fun printTokens(previousToken: Token? = null) = run {
        val prev = previousToken?.run { "Eating: '$value', " } ?: ""
        val curr = token.run { "Current: '$value'" }
        val next = nextToken?.run { ", Next: '$value'" } ?: ""
        logger.log("$prev$curr$next")
    }

    private fun nextToken() = runCatching { tokenIterator.next() }.getOrNull()
}

fun List<Token>.toProvider(logger: Logger) = TokenProvider(logger, iterator())
