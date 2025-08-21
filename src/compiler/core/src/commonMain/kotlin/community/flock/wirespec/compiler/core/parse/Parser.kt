package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.computations.ResultEffect.bind
import arrow.core.flatMap
import arrow.core.flattenOrAccumulate
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.raise.mapOrAccumulate
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.exceptions.EmptyModule
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.compiler.utils.HasLogger

data class ParseOptions(
    val strict: Boolean = false,
    val allowUnions: Boolean = true,
)

object Parser {
    fun HasLogger.parse(
        modules: NonEmptyList<TokenizedModule>,
        options: ParseOptions = ParseOptions(),
    ): EitherNel<WirespecException, AST> = either {
        modules
            .map { it.toProvider(modules.allDefinitions(), logger).parseModule() }
            .flattenOrAccumulate().bind()
            .toNonEmptyListOrNull()
            .let { ensureNotNull(it) { EmptyModule().nel() } }
            .let { AST(it) }
            .let { Validator.validate(options, it).bind() }
    }
}

fun <A> TokenProvider.parseToken(block: Raise<WirespecException>.(Token) -> A) = either {
    block(eatToken().bind())
}

inline fun <reified T : TokenType> TokenProvider.raiseWrongToken(token: Token? = null): Either<WirespecException, Nothing> = either {
    raise(
        WrongTokenException<T>(
            fileUri,
            token ?: this@raiseWrongToken.token,
        ).also { eatToken().bind() },
    )
}

private fun NonEmptyList<TokenizedModule>.allDefinitions() = flatMap { it.tokens }
    .zipWithNext()
    .mapNotNull { (first, second) ->
        when (first.type) {
            is WirespecDefinition -> second.value
            else -> null
        }
    }
    .toSet()

private fun TokenProvider.parseModule(): EitherNel<WirespecException, Module> = either {
    mutableListOf<Either<WirespecException, Definition>>()
        .apply {
            while (hasNext()) {
                when (token.type) {
                    is Comment -> add(parseDefinition())
                    is WirespecDefinition -> add(parseDefinition())
                    else -> eatToken()
                }
            }
        }
        .mapOrAccumulate { it.bind() }.bind()
        .toNonEmptyListOrNull()
        .let { ensureNotNull(it) { EmptyModule().nel() } }
        .let { Module(fileUri, it) }
}

private fun TokenProvider.parseDefinition() = either {
    val comment = when (token.type) {
        is Comment -> Comment(token.value).also { eatToken().bind() }
        else -> null
    }
    when (token.type) {
        is WirespecDefinition -> when (token.type as WirespecDefinition) {
            is TypeDefinition -> with(TypeParser) { parseType(comment) }.bind()
            is EnumTypeDefinition -> with(EnumParser) { parseEnum(comment) }.bind()
            is EndpointDefinition -> with(EndpointParser) { parseEndpoint(comment) }.bind()
            is ChannelDefinition -> with(ChannelParser) { parseChannel(comment) }.bind()
        }

        else -> raiseWrongToken<WirespecDefinition>().bind()
    }
}
