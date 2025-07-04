package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.Hash
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Method
import community.flock.wirespec.compiler.core.tokenize.Path
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.StatusCode
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object EndpointParser {

    fun TokenProvider.parseEndpoint(comment: Comment?): Either<WirespecException, Endpoint> = parseToken {
        when (token.type) {
            is WirespecType -> parseEndpointDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseEndpointDefinition(comment: Comment?, name: DefinitionIdentifier) = parseToken {
        val method = when (token.type) {
            is Method -> Endpoint.Method.valueOf(token.value)
            else -> raiseWrongToken<Method>().bind()
        }.also { eatToken().bind() }

        val requests = listOf(
            with(TypeParser) {
                when (val type = token.type) {
                    is LeftCurly -> parseDict().bind()
                    is WirespecType -> parseWirespecType(type).bind()
                    else -> null
                }
            },
        ).map {
            Endpoint.Request(
                content = it?.let {
                    Endpoint.Content(
                        type = "application/json",
                        reference = it,
                    )
                },
            )
        }

        val segments = mutableListOf<Endpoint.Segment>().apply {
            while (token.type !is QuestionMark && token.type !is Hash && token.type !is Arrow) {
                add(parseEndpointSegments().bind())
            }
        }

        val queryParams = when (token.type) {
            is QuestionMark -> {
                eatToken().bind()
                when (token.type) {
                    is LeftCurly -> with(TypeParser) { parseTypeShape().bind() }.value
                    else -> raiseWrongToken<LeftCurly>().bind()
                }
            }

            else -> emptyList()
        }

        val headers = parseHeaders().bind()

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Arrow>().bind()
        }

        when (token.type) {
            is LeftCurly -> Unit
            else -> raiseWrongToken<LeftCurly>().bind()
        }.also { eatToken().bind() }

        val responses = parseEndpointResponses().bind()

        Endpoint(
            comment = comment,
            identifier = name,
            method = method,
            path = segments,
            queries = queryParams,
            headers = headers,
            requests = requests,
            responses = responses,
        )
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
            when (val type = token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseWirespecType(type).bind()
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
            when (token.type) {
                is StatusCode -> responses.add(parseEndpointResponse(token.value).bind())
                else -> raiseWrongToken<StatusCode>().bind()
            }
        }
        when (token.type) {
            is RightCurly -> Unit
            else -> raiseWrongToken<RightCurly>().bind()
        }.also { eatToken().bind() }
        responses.toList()
    }

    private fun TokenProvider.parseEndpointResponse(statusCode: String) = parseToken {
        when (token.type) {
            is Arrow -> Unit
            else -> raiseWrongToken<Arrow>().bind()
        }
        eatToken().bind()

        val reference = with(TypeParser) {
            when (val type = token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseWirespecType(type).bind()
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

        Endpoint.Response(status = statusCode, headers = headers, content = content)
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
