package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.NullTokenException.NextException
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.Tokens
import community.flock.wirespec.compiler.core.tokenize.removeWhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WirespecDefinition
import community.flock.wirespec.compiler.utils.Logger

class TokenProvider(private val logger: Logger, list: NonEmptyList<Token>) {

    private val tokenIterator = list.tail.iterator()

    var token = list.head
    private var nextToken = nextToken()

    private val definitionNames: List<String> = list
        .removeWhiteSpace()
        .zipWithNext()
        .mapNotNull { (first, second) ->
            when (first.type) {
                is WirespecDefinition -> second.value
                else -> null
            }
        }

    fun Token.shouldBeDefined(): Either<WirespecException, Unit> = either {
        if (value !in definitionNames) {
            raise(ParserException.DefinitionNotExistsException(value, coordinates))
        }
    }

    init {
        printTokens()
    }

    fun hasNext() = nextToken != null

    fun eatToken(): Either<WirespecException, Unit> = either {
        val previousToken = token
        token = nextToken ?: raise(NextException(previousToken.coordinates))
        nextToken = nextToken()
        printTokens(previousToken)
    }

    private fun printTokens(previousToken: Token? = null) {
        val prev = previousToken?.run { "Eating: '$value', " } ?: ""
        val curr = token.run { "Current: '$value'" }
        val next = nextToken?.run { ", Next: '$value'" } ?: ""
        logger.log("$prev$curr$next")
    }

    private fun nextToken() = runCatching { tokenIterator.next() }.getOrNull()
}

fun Tokens.toProvider(logger: Logger) = TokenProvider(logger, this)
