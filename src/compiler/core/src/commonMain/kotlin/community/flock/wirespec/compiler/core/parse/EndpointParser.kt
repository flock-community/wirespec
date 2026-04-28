package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AnnotationParser.parseAnnotations
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object EndpointParser {

    fun TokenProvider.parseEndpoint(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Endpoint> = parseToken {
        when (token.type) {
            is WirespecType -> parseEndpointDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseEndpointDefinition(
        comment: Comment?,
        annotations: List<Annotation>,
        name: DefinitionIdentifier,
    ) = parseToken {
        val method = parseEndpointMethod().bind()
        val requests = parseEndpointRequests().bind()
        val segments = parseEndpointSegmentList().bind()
        val queryParams = parseEndpointQueryParams().bind()
        val headers = parseHeaders().bind()
        expectArrow().bind()
        expectLeftCurlyAndEat().bind()
        val responses = parseEndpointResponses().bind()

        Endpoint(
            comment = comment,
            annotations = annotations,
            identifier = name,
            method = method,
            path = segments,
            queries = queryParams,
            headers = headers,
            requests = requests,
            responses = responses,
        )
    }

    private fun TokenProvider.parseEndpointMethod() = either {
        val method = when (token.type) {
            is Method -> Endpoint.Method.valueOf(token.value)
            else -> raiseWrongToken<Method>().bind()
        }
        eatToken().bind()
        method
    }

    private fun TokenProvider.parseEndpointRequests() = either {
        val body = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> null
            }
        }
        listOf(
            Endpoint.Request(
                content = body?.let { Endpoint.Content(type = "application/json", reference = it) },
            ),
        )
    }

    private fun TokenProvider.parseEndpointSegmentList() = either {
        buildList {
            while (token.type !is QuestionMark && token.type !is Hash && token.type !is Arrow) {
                add(parseEndpointSegments().bind())
            }
        }
    }

    private fun TokenProvider.parseEndpointQueryParams() = either {
        when (token.type) {
            is QuestionMark -> {
                eatToken().bind()
                when (token.type) {
                    is LeftCurly -> with(TypeParser) { parseTypeShape().bind() }.value
                    else -> raiseWrongToken<LeftCurly>().bind()
                }
            }

            else -> emptyList()
        }
    }

    private fun TokenProvider.expectArrow() = either {
        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Arrow>().bind()
        }
    }

    private fun TokenProvider.expectLeftCurlyAndEat() = either {
        when (token.type) {
            is LeftCurly -> Unit
            else -> raiseWrongToken<LeftCurly>().bind()
        }
        eatToken().bind()
    }

    private fun TokenProvider.parseEndpointSegments() = either {
        when (token.type) {
            is Path -> Endpoint.Segment.Literal(token.value.drop(1)).also { eatToken().bind() }
            is ForwardSlash -> parseEndpointSegmentParam().bind()
            else -> raiseWrongToken<Path>().bind()
        }
    }

    private fun TokenProvider.parseEndpointSegmentParam() = parseToken {
        when (token.type) {
            is LeftCurly -> eatToken().bind()
            else -> raiseWrongToken<LeftCurly>().bind()
        }
        val identifier = when (token.type) {
            is WirespecIdentifier -> FieldIdentifier(token.value).also { eatToken().bind() }
            else -> raiseWrongToken<WirespecIdentifier>().bind()
        }
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }
        val reference = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }
        when (token.type) {
            is RightCurly -> eatToken().bind()
            else -> raiseWrongToken<RightCurly>().bind()
        }
        Endpoint.Segment.Param(
            identifier = identifier,
            reference = reference,
        )
    }

    private fun TokenProvider.parseEndpointResponses() = either {
        val responses = mutableListOf<Endpoint.Response>()
        while (token.type !is RightCurly) {
            val annotations = parseAnnotations().bind()
            when (token.type) {
                is Integer -> responses.add(parseEndpointResponse(token.value, annotations).bind())
                else -> raiseWrongToken<Integer>().bind()
            }
        }
        when (token.type) {
            is RightCurly -> Unit
            else -> raiseWrongToken<RightCurly>().bind()
        }.also { eatToken().bind() }
        responses.toList()
    }

    private fun TokenProvider.parseEndpointResponse(statusCode: String, annotations: List<Annotation>) = parseToken {
        when (token.type) {
            is Arrow -> Unit
            else -> raiseWrongToken<Arrow>().bind()
        }
        eatToken().bind()

        val reference = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }

        val content =
            if (reference is Reference.Unit) {
                null
            } else {
                Endpoint.Content(
                    type = "application/json",
                    reference = reference,
                )
            }

        val headers = parseHeaders().bind()

        Endpoint.Response(
            status = statusCode,
            headers = headers,
            content = content,
            annotations = annotations,
        )
    }

    private fun TokenProvider.parseHeaders() = either {
        when (token.type) {
            is Hash -> {
                eatToken().bind()
                when (token.type) {
                    is LeftCurly -> with(TypeParser) { parseTypeShape().bind() }.value
                    else -> raiseWrongToken<LeftCurly>().bind()
                }
            }

            else -> emptyList()
        }
    }
}
