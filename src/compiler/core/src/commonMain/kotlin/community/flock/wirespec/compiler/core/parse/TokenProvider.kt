package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.WsCustomType
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.NullTokenException.NextException
import community.flock.wirespec.compiler.core.tokenize.ImportDefinition
import community.flock.wirespec.compiler.core.tokenize.Keyword
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.Tokens
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.utils.Logger

class TokenProvider(private val logger: Logger, tokens: Tokens) {

    var token = tokens.head

    private val tokenIterator = tokens.tail.iterator()
    private var nextToken = nextToken()

    private val definitionNames = tokens
        .fold(emptyList<MutableList<Token>>()) { acc, t ->
            when {
                t.type is Keyword -> acc + listOf(mutableListOf(t))
                else -> acc.apply { lastOrNull()?.add(t) }
            }
        }
        .flatMap {
            when (it[0].type) {
                is ImportDefinition -> it.filter { t -> t.type is WsCustomType }
                is TypeDefinition -> listOf(it[1])
                else -> emptyList()
            }
        }
        .map { it.value }

    fun Token.shouldBeDefined(): Either<WirespecException, Unit> = either {
        if (value !in definitionNames) {
            raise(ParserException.DefinitionNotExistsException(value, coordinates))
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

    private fun printTokens(previousToken: Token? = null) {
        val prev = previousToken?.run { "Eating: '$value', " } ?: ""
        val curr = token.run { "Current: '$value'" }
        val next = nextToken?.run { ", Next: '$value'" } ?: ""
        logger.debug("$prev$curr$next")
    }

    private fun nextToken() = runCatching { tokenIterator.next() }.getOrNull()
}

fun Tokens.toProvider(logger: Logger) = TokenProvider(logger, this)
