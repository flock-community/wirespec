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
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.LeftBracket
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.LiteralString
import community.flock.wirespec.compiler.core.tokenize.RightBracket
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
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

    fun TokenProvider.parseAnnotations(): Either<WirespecException, List<Annotation>> = either {
        when (token.type) {
            is AnnotationToken -> {
                val annotation = parseAnnotation().bind()
                val remaining = parseAnnotations().bind()
                listOf(annotation) + remaining
            }
            else -> emptyList()
        }
    }

    private fun TokenProvider.parseAnnotation() = either {
        val name = token.value.drop(1).also { eatToken().bind() }
        val parameters = when (token.type) {
            is LeftParenthesis -> {
                eatToken().bind() // consume (
                parseAnnotationParameters().bind().also {
                    when (token.type) {
                        RightParenthesis -> eatToken().bind() // consume )
                        else -> raiseWrongToken<RightParenthesis>().bind()
                    }
                }
            }
            else -> emptyList()
        }
        Annotation(name, parameters)
    }

    private fun TokenProvider.parseAnnotationParameters(): Either<WirespecException, List<Annotation.Parameter>> = either {
        when (token.type) {
            is RightParenthesis -> emptyList()
            else -> {
                val params = parseAnnotationParameter().bind()
                val remaining = when (token.type) {
                    is Comma -> {
                        eatToken().bind()
                        parseAnnotationParameters().bind()
                    }
                    else -> emptyList()
                }
                params + remaining
            }
        }
    }

    private fun TokenProvider.parseAnnotationParameter(): Either<WirespecException, List<Annotation.Parameter>> = either {
        when (token.type) {
            is LeftBracket -> {
                val arr = parseArray().bind()
                listOf(Annotation.Parameter("default", Annotation.Value.Array(arr)))
            }

            is community.flock.wirespec.compiler.core.tokenize.LeftCurly -> {
                val dictParams = parseDict().bind()
                listOf(Annotation.Parameter("default", Annotation.Value.Dict(dictParams)))
            }
            else -> {
                val firstTokenValue = when (token.type) {
                    LiteralString -> token.value.removeSurrounding("\"")
                    else -> token.value
                }
                eatToken().bind()
                when (token.type) {
                    is Colon -> {
                        val name = firstTokenValue
                        eatToken().bind()
                        val valueNode = parseAnnotationValue().bind()
                        listOf(Annotation.Parameter(name, valueNode))
                    }
                    else -> {
                        listOf(Annotation.Parameter("default", Annotation.Value.Single(firstTokenValue)))
                    }
                }
            }
        }
    }

    private fun TokenProvider.parseAnnotationValue(): Either<WirespecException, Annotation.Value> = either {
        when (token.type) {
            is LeftBracket -> {
                val arr = parseArray().bind()
                Annotation.Value.Array(arr)
            }

            is community.flock.wirespec.compiler.core.tokenize.LeftCurly -> {
                val dictParams = parseDict().bind()
                Annotation.Value.Dict(dictParams)
            }

            LiteralString -> Annotation.Value.Single(token.value.removeSurrounding("\""))
            else -> Annotation.Value.Single(token.value)
        }.also {
            // consume token(s) for Single types here
            if (it is Annotation.Value.Single) {
                eatToken().bind()
            }
        }
    }

    private fun TokenProvider.parseArray(): Either<WirespecException, List<Annotation.Value.Single>> = either {
        when (token.type) {
            is LeftBracket -> eatToken().bind()
            else -> raiseWrongToken<LeftBracket>().bind()
        }
        val items = mutableListOf<Annotation.Value.Single>()
        while (token.type != RightBracket) {
            val v = when (token.type) {
                LiteralString -> Annotation.Value.Single(token.value.removeSurrounding("\""))
                else -> Annotation.Value.Single(token.value)
            }
            items.add(v)
            eatToken().bind()
            if (token.type is Comma) eatToken().bind()
        }
        when (token.type) {
            RightBracket -> eatToken().bind()
            else -> raiseWrongToken<RightBracket>().bind()
        }
        items
    }

    private fun TokenProvider.parseDict(): Either<WirespecException, List<Annotation.Parameter>> = either {
        when (token.type) {
            is community.flock.wirespec.compiler.core.tokenize.LeftCurly -> eatToken().bind()
            else -> raiseWrongToken<community.flock.wirespec.compiler.core.tokenize.LeftCurly>().bind()
        }
        val params = mutableListOf<Annotation.Parameter>()
        while (token.type !is RightCurly) {
            val key = token.value
            eatToken().bind()
            when (token.type) {
                is Colon -> eatToken().bind()
                else -> raiseWrongToken<Colon>().bind()
            }
            val value = parseAnnotationValue().bind()
            params.add(Annotation.Parameter(key, value))
            if (token.type is Comma) eatToken().bind()
        }
        // consume closing }
        when (token.type) {
            is RightCurly -> eatToken().bind()
            else -> raiseWrongToken<RightCurly>().bind()
        }
        params
    }
}

private fun TokenProvider.captureEnclosedAsRawString(openChar: Char, closeChar: Char): Either<WirespecException, String> = either {
    val sb = StringBuilder()
    var depth = 0
    // Start expecting the opening token already at current position
    while (true) {
        val t = token
        val v = t.value
        when (t.type) {
            is community.flock.wirespec.compiler.core.tokenize.LeftCurly -> {
                depth += 1
                sb.append(v)
                eatToken().bind()
            }
            is RightCurly -> {
                depth -= 1
                sb.append(v)
                eatToken().bind()
                if (depth == 0) break
            }
            else -> {
                sb.append(v)
                eatToken().bind()
            }
        }
    }
    sb.toString()
}
