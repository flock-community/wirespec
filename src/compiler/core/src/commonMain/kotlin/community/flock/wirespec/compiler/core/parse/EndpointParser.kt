package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.DELETE
import community.flock.wirespec.compiler.core.tokenize.types.GET
import community.flock.wirespec.compiler.core.tokenize.types.HEAD
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.OPTIONS
import community.flock.wirespec.compiler.core.tokenize.types.PATCH
import community.flock.wirespec.compiler.core.tokenize.types.POST
import community.flock.wirespec.compiler.core.tokenize.types.PUT
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.TRACE
import community.flock.wirespec.compiler.core.tokenize.types.WirespecType
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.utils.Logger

class EndpointParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEndpoint(): Either<WirespecException, Endpoint> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(token.value).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEndpointDefinition(name: String) = either {
        eatToken().bind()
        token.log()
        val method = parseEndpointMethod().bind()
        val path = parseEndpointPath().bind()
        when (token.type) {
            is LeftCurly -> Unit
            else -> raise(WrongTokenException<LeftCurly>(token))
        }.also { eatToken().bind() }

        val responses = parseEndpointResponses().bind()

        Endpoint(
            name = name,
            method = method,
            path = path,
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = emptyList(),
            responses = responses,
        )
    }

    private fun TokenProvider.parseEndpointMethod() = either {
        token.log()
        when (token.type) {
            is Method -> when (token.type as Method) {
                DELETE -> Endpoint.Method.DELETE
                GET -> Endpoint.Method.GET
                HEAD -> Endpoint.Method.HEAD
                OPTIONS -> Endpoint.Method.OPTIONS
                PATCH -> Endpoint.Method.PATCH
                POST -> Endpoint.Method.POST
                PUT -> Endpoint.Method.PUT
                TRACE -> Endpoint.Method.TRACE
            }

            else -> raise(WrongTokenException<Method>(token))
        }.also { eatToken().bind() }
    }

    private fun TokenProvider.parseEndpointPath() = either {
        token.log()
        when (token.type) {
            is Path -> Endpoint.Segment.Literal(token.value.drop(1)).nel()
            else -> raise(WrongTokenException<Path>(token))
        }.also { eatToken().bind() }
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
        val content = when (token.type) {
            is WirespecType -> parseContent(token.type as WirespecType, token.value).bind()
            else -> raise(WrongTokenException<WirespecType>(token))
        }
        Endpoint.Response(status = statusCode, content = content)
    }

    private fun TokenProvider.parseContent(wsType: WirespecType, value: String) = either {
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        val reference = when (wsType) {
            is WsString -> Primitive(Primitive.Type.String, isIterable)

            is WsInteger -> Primitive(Primitive.Type.Integer, isIterable)

            is WsBoolean -> Primitive(Primitive.Type.Boolean, isIterable)

            is CustomType -> Custom(value, isIterable)
        }
        Endpoint.Content(type = "application/json", reference = reference)
    }
}
