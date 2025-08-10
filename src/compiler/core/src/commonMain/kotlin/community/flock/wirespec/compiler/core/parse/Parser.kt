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
import community.flock.wirespec.compiler.core.tokenize.At
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.DromedaryCaseIdentifier
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.FieldIdentifier
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
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
    val annotations = parseAnnotations().bind()
        val comment = when (token.type) {
        is Comment -> Comment(token.value).also { eatToken().bind() }
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

    private fun TokenProvider.parseAnnotations() = either {
        val annotations = mutableListOf<Annotation>()
        while (token.type is At) {
            annotations.add(parseAnnotation().bind())
        }
        annotations
    }

    private fun TokenProvider.parseAnnotation() = either {
        eatToken().bind() // consume @
        val name = when (token.type) {
            is FieldIdentifier -> token.value.also { eatToken().bind() }
            is DromedaryCaseIdentifier -> token.value.also { eatToken().bind() }
            else -> raiseWrongToken<FieldIdentifier>().bind()
        }
        val parameters = if (token.type is LeftParenthesis) {
            eatToken().bind() // consume (
            parseAnnotationParameters().bind().also {
                if (token.type is RightParenthesis) {
                    eatToken().bind() // consume )
                } else {
                    raiseWrongToken<RightParenthesis>().bind()
                }
            }
        } else {
            emptyList()
        }
        Annotation(name, parameters)
    }

    private fun TokenProvider.parseAnnotationParameters() = either {
        val parameters = mutableListOf<AnnotationParameter>()
        while (token.type !is RightParenthesis && hasNext()) {
            val param = parseAnnotationParameter().bind()
            parameters.add(param)
            if (token.type is Comma) {
                eatToken().bind() // consume comma
            } else if (token.type !is RightParenthesis) {
                break // no comma, but also not closing paren - exit loop
            }
        }
        parameters
    }

    private fun TokenProvider.parseAnnotationParameter() = either {
        val nameAndValue = if (hasNext()) {
            // Simple approach: collect the current token value
            val value = token.value.also { eatToken().bind() }

            // Check if next token is colon (for named parameters)
            if (token.type is Colon) {
                eatToken().bind() // consume :
                val actualValue = token.value.also { eatToken().bind() }
                value to actualValue // name to value
            } else {
                null to value // positional parameter
            }
        } else {
            null to ""
        }
        AnnotationParameter(nameAndValue.first, nameAndValue.second)
    }
}
