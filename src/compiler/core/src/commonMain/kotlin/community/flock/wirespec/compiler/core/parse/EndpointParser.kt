package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.Hash
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WirespecType
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsNumber
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsUnit
import community.flock.wirespec.compiler.utils.Logger

class EndpointParser(logger: Logger) : AbstractParser(logger) {

    private val typeParser = TypeParser(logger)

    fun TokenProvider.parseEndpoint(comment: Comment?): Either<WirespecException, Endpoint> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEndpointDefinition(comment: Comment?, name: DefinitionIdentifier) = either {
        eatToken().bind()
        token.log()
        val method = when (token.type) {
            is Method -> Endpoint.Method.valueOf(token.value)
            else -> raise(WrongTokenException<Method>(token))
        }.also { eatToken().bind() }

        val isDict = when (token.type) {
            is LeftCurly -> true.also { eatToken().bind() }
            else -> false
        }

        val requests = listOf(
            when (token.type) {
                is WirespecType -> parseReference(token.type as WirespecType, token.value, isDict).bind()
                else -> null
            }
        )
            .also {
                if (isDict) {
                    when (token.type) {
                        is RightCurly -> eatToken().bind()
                        else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                    }
                }
            }
            .map {
                Endpoint.Request(
                    content = it?.let {
                        Endpoint.Content(
                            type = "application/json",
                            reference = it,
                            isNullable = false
                        )
                    }
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
                    is LeftCurly -> with(typeParser) { parseTypeShape().bind() }.value
                    else -> raise(WrongTokenException<LeftCurly>(token))
                }
            }

            else -> emptyList()
        }

        val headers = when (token.type) {
            is Hash -> {
                eatToken().bind()
                when (token.type) {
                    is LeftCurly -> with(typeParser) { parseTypeShape().bind() }.value
                    else -> raise(WrongTokenException<LeftCurly>(token))
                }
            }

            else -> emptyList()
        }

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raise(WrongTokenException<Arrow>(token))
        }

        when (token.type) {
            is LeftCurly -> Unit
            else -> raise(WrongTokenException<LeftCurly>(token))
        }.also { eatToken().bind() }

        val responses = parseEndpointResponses().bind()

        Endpoint(
            comment = comment,
            identifier = name,
            method = method,
            path = segments,
            queries = queryParams,
            headers = headers,
            cookies = emptyList(),
            requests = requests,
            responses = responses,
        )
    }

    private fun TokenProvider.parseEndpointSegments() = either {
        token.log()
        when (token.type) {
            is Path -> Endpoint.Segment.Literal(token.value.drop(1)).also { eatToken().bind() }
            is ForwardSlash -> parseEndpointSegmentParam().bind()
            else -> raise(WrongTokenException<Path>(token))
        }
    }

    private fun TokenProvider.parseEndpointSegmentParam() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> eatToken().bind()
            else -> raise(WrongTokenException<LeftCurly>(token))
        }
        val identifier = when (token.type) {
            is CustomValue -> FieldIdentifier(token.value).also { eatToken().bind() }
            else -> raise(WrongTokenException<CustomValue>(token))
        }
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token))
        }
        val isDict = when (token.type) {
            is LeftCurly -> true.also { eatToken().bind() }
            else -> false
        }
        val reference = when (token.type) {
            is WirespecType -> parseReference(token.type as WirespecType, token.value, isDict).bind()
            else -> raise(WrongTokenException<WirespecType>(token))
        }.also {
            if (isDict) {
                when (token.type) {
                    is RightCurly -> eatToken().bind()
                    else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                }
            }
        }
        when (token.type) {
            is RightCurly -> eatToken().bind()
            else -> raise(WrongTokenException<RightCurly>(token))
        }
        Endpoint.Segment.Param(
            identifier = identifier,
            reference = reference
        )
    }

    private fun TokenProvider.parseEndpointResponses() = either {
        token.log()
        val responses = mutableListOf<Endpoint.Response>()
        while (token.type !is RightCurly) {
            when (token.type) {
                is StatusCode -> responses.add(parseEndpointResponse(token.value).bind())
                else -> raise(WrongTokenException<StatusCode>(token))
            }
        }
        when (token.type) {
            is RightCurly -> Unit
            else -> raise(WrongTokenException<RightCurly>(token))
        }.also { eatToken().bind() }
        responses.toList()
    }

    private fun TokenProvider.parseEndpointResponse(statusCode: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Arrow -> Unit
            else -> raise(WrongTokenException<Arrow>(token))
        }
        eatToken().bind()
        token.log()
        val isDict = when (token.type) {
            is LeftCurly -> true.also { eatToken().bind() }
            else -> false
        }
        val content = when (token.type) {
            is WirespecType -> parseContent(token.type as WirespecType, token.value, isDict).bind()
            else -> raise(WrongTokenException<WirespecType>(token))
        }.also {
            if (isDict) {
                when (token.type) {
                    is RightCurly -> eatToken().bind()
                    else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                }
            }
        }
        Endpoint.Response(status = statusCode, headers = emptyList(), content = content)
    }

    private fun TokenProvider.parseContent(wsType: WirespecType, value: String, isDict: Boolean) = either {
        token.log()
        val reference = parseReference(wsType, value, isDict).bind()
        if (reference is Reference.Unit) null
        else Endpoint.Content(
            type = "application/json",
            reference = reference,
        )
    }

    private fun TokenProvider.parseReference(wsType: WirespecType, value: String, isDict: Boolean) = either {
        val previousToken = token
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        when (wsType) {
            is WsString -> Reference.Primitive(
                type = Reference.Primitive.Type.String,
                isIterable = isIterable,
                isDictionary = isDict,
            )

            is WsInteger -> Reference.Primitive(
                type = Reference.Primitive.Type.Integer,
                isIterable = isIterable,
                isDictionary = isDict,
            )

            is WsNumber -> Reference.Primitive(
                type = Reference.Primitive.Type.Number,
                isIterable = isIterable,
                isDictionary = isDict,
            )

            is WsBoolean -> Reference.Primitive(
                type = Reference.Primitive.Type.Boolean,
                isIterable = isIterable,
                isDictionary = isDict,
            )

            is WsUnit -> Reference.Unit(isIterable)

            is CustomType -> {
                previousToken.shouldBeDefined().bind()
                Reference.Custom(value, isIterable)
            }
        }
    }
}
