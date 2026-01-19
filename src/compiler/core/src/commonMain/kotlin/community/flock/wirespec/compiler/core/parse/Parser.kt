package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.flattenOrAccumulate
import arrow.core.mapOrAccumulate
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.exceptions.EmptyModule
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.parse.AnnotationParser.parseAnnotations
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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
import community.flock.wirespec.compiler.core.tokenize.Annotation as AnnotationToken

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
                    is AnnotationToken -> add(parseDefinition())
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
    val annotations = parseAnnotations().bind()
    val comment = when (token.type) {
        is Comment -> community.flock.wirespec.compiler.core.parse.ast.Comment(token.value).also { eatToken().bind() }
        else -> null
    }
    when (token.type) {
        is WirespecDefinition -> when (token.type as WirespecDefinition) {
            is TypeDefinition -> with(TypeParser) { parseType(comment, annotations) }.bind()
            is EnumTypeDefinition -> with(EnumParser) { parseEnum(comment, annotations) }.bind()
            is EndpointDefinition -> with(EndpointParser) { parseEndpoint(comment, annotations) }.bind()
            is ChannelDefinition -> with(ChannelParser) { parseChannel(comment, annotations) }.bind()
        }

        else -> raiseWrongToken<WirespecDefinition>().bind()
    }
}
