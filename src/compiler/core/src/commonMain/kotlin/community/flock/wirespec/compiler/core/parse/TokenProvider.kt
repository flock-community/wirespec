package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.NonEmptyList
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.DefinitionNotExistsException
import community.flock.wirespec.compiler.core.exceptions.NextException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.utils.Logger

class TokenProvider(private val logger: Logger, tokens: NonEmptyList<Token>) {

    var token = tokens.head

    private val tokenIterator = tokens.tail.iterator()
    private var nextToken = nextToken()

    private val definitionNames: List<String> = tokens
        .zipWithNext()
        .mapNotNull { (first, second) ->
            when (first.type) {
                is WirespecDefinition -> second.value
                else -> null
            }
        }

    fun Token.shouldBeDefined(): Either<WirespecException, Unit> = either {
        if (value !in definitionNames) {
            raise(DefinitionNotExistsException(value, coordinates))
        }
    }

    init {
        printTokens()
    }

    fun hasNext() = nextToken != null

    fun eatToken(): Either<WirespecException, Token> = either {
        val previousToken = token
        token = nextToken ?: raise(NextException(previousToken.coordinates))
        nextToken = nextToken()
        printTokens(previousToken)
        previousToken
    }

    fun Token.log() = logger.debug("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

    private fun printTokens(previousToken: Token? = null) {
        val prev = previousToken?.run { "Eating: '$value', " } ?: ""
        val curr = token.run { "Current: '$value'" }
        val next = nextToken?.run { ", Next: '$value'" } ?: ""
        logger.debug("$prev$curr$next")
    }

    private fun nextToken() = catch { tokenIterator.next() }.getOrNull()
}

fun NonEmptyList<Token>.toProvider(logger: Logger) = TokenProvider(logger, this)
