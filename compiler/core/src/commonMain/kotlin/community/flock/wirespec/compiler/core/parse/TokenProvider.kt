package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.NullTokenException.NextException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.NullTokenException.StartingException
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.utils.Logger

class TokenProvider(private val logger: Logger, private val tokenIterator: Iterator<Token>) {

    var token = nextToken() ?: throw StartingException()
    private var nextToken = nextToken()

    init {
        printTokens()
    }

    fun hasNext() = nextToken != null

    fun eatToken(): Either<NonEmptyList<WirespecException>, Unit> = either{
        val previousToken = token
        token = nextToken ?: raise(NextException(previousToken.coordinates).nel())
        nextToken = nextToken()
        printTokens(previousToken)
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
