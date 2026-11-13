package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import community.flock.wirespec.compiler.core.exceptions.MisplacedHeaderAnnotationException
import community.flock.wirespec.compiler.core.exceptions.MissingEmptyLineAfterHeaderException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.FileAnnotation
import community.flock.wirespec.compiler.core.tokenize.HeaderAnnotation
import community.flock.wirespec.compiler.core.tokenize.LeftBracket
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.LiteralString
import community.flock.wirespec.compiler.core.tokenize.RightBracket
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
import community.flock.wirespec.compiler.core.tokenize.SpecAnnotation
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.Annotation as AnnotationToken

/** The `@spec:` and `@file:` annotations at the top of a file. */
internal data class Header(val spec: List<Annotation>, val file: List<Annotation>)

internal object AnnotationParser {
    fun TokenProvider.parseAnnotations(): Either<WirespecException, List<Annotation>> = either {
        buildList {
            while (token.type is AnnotationToken) {
                add(parseAnnotation().bind())
            }
        }
    }

    /** Reads the file header, which an empty line separates from whatever follows it. */
    fun TokenProvider.parseHeader(): Either<WirespecException, Header> = either {
        buildList { while (token.type is HeaderAnnotation) add(token.type to parseAnnotation().bind()) }
            .also { if (it.isNotEmpty()) ensureEmptyLineAfterHeader().bind() }
            .let { Header(spec = it.annotationsOf<SpecAnnotation>(), file = it.annotationsOf<FileAnnotation>()) }
    }

    /** Reads a header annotation outside the header and raises, so parsing goes on after it. */
    fun TokenProvider.misplacedHeaderAnnotation(): Either<WirespecException, Nothing> = either {
        val annotation = token
        parseAnnotation().bind()
        raise(MisplacedHeaderAnnotationException(fileUri, annotation.value, annotation.coordinates))
    }
}

private inline fun <reified T : HeaderAnnotation> List<Pair<TokenType, Annotation>>.annotationsOf(): List<Annotation> = filter { (type) -> type is T }.map { (_, annotation) -> annotation }

private fun TokenProvider.ensureEmptyLineAfterHeader(): Either<WirespecException, Unit> = either {
    ensure(token.type is EndOfProgram || token.startLine() - (previous?.coordinates?.line ?: 0) > 1) {
        MissingEmptyLineAfterHeaderException(fileUri, token.coordinates)
    }
}

/** A token's coordinates are where it ends, and a comment can span several lines. */
private fun Token.startLine(): Int = coordinates.line - value.count { it == '\n' }

private fun TokenProvider.parseAnnotation() = either {
    val name = token.value.drop(1).substringAfter(':').also { eatToken().bind() }
    val parameters = when (token.type) {
        is LeftParenthesis -> {
            eatToken().bind() // consume (
            parseAnnotationParameters().bind().also {
                expect<RightParenthesis>().bind()
            }
        }

        else -> emptyList()
    }
    Annotation(name, parameters)
}

private fun TokenProvider.parseAnnotationParameters(): Either<WirespecException, List<Annotation.Parameter>> = either {
    buildList {
        while (token.type !is RightParenthesis) {
            addAll(parseAnnotationParameter().bind())
            when (token.type) {
                is Comma -> eatToken().bind()
                else -> break
            }
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
    expect<LeftCurly>().bind()
    val params = mutableListOf<Annotation.Parameter>()
    while (token.type !is RightCurly) {
        val key = when (token.type) {
            LiteralString -> token.value.removeSurrounding("\"")
            else -> token.value
        }
        eatToken().bind()
        expect<Colon>().bind()
        val value = parseAnnotationValue().bind()
        params.add(Annotation.Parameter(key, value))
        if (token.type is Comma) eatToken().bind()
    }
    expect<RightCurly>().bind()
    params
}

private fun TokenProvider.parseArray(): Either<WirespecException, List<Annotation.Value.Single>> = either {
    expect<LeftBracket>().bind()
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
    expect<RightBracket>().bind()
    items
}
