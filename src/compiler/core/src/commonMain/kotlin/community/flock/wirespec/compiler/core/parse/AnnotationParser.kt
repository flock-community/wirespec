package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.LeftBracket
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.LiteralString
import community.flock.wirespec.compiler.core.tokenize.RightBracket
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
import community.flock.wirespec.compiler.core.tokenize.Annotation as AnnotationToken

object AnnotationParser {
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

        is LeftCurly -> {
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
                    eatToken().bind()
                    val valueNode = parseAnnotationValue().bind()
                    listOf(Annotation.Parameter(firstTokenValue, valueNode))
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

        is LeftCurly -> {
            val dictParams = parseDict().bind()
            Annotation.Value.Dict(dictParams)
        }

        LiteralString -> Annotation.Value.Single(token.value.removeSurrounding("\""))
        else -> Annotation.Value.Single(token.value)
    }.also {
        if (it is Annotation.Value.Single) {
            eatToken().bind()
        }
    }
}

private fun TokenProvider.parseDict(): Either<WirespecException, List<Annotation.Parameter>> = either {
    when (token.type) {
        is LeftCurly -> eatToken().bind()
        else -> raiseWrongToken<LeftCurly>().bind()
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
    when (token.type) {
        is RightCurly -> eatToken().bind()
        else -> raiseWrongToken<RightCurly>().bind()
    }
    params
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
