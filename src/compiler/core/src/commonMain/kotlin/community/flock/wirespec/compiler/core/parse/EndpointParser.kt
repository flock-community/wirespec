package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.DELETE
import community.flock.wirespec.compiler.core.tokenize.types.GET
import community.flock.wirespec.compiler.core.tokenize.types.HEAD
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.OPTIONS
import community.flock.wirespec.compiler.core.tokenize.types.PATCH
import community.flock.wirespec.compiler.core.tokenize.types.POST
import community.flock.wirespec.compiler.core.tokenize.types.PUT
import community.flock.wirespec.compiler.core.tokenize.types.TRACE
import community.flock.wirespec.compiler.utils.Logger

class EndpointParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEndpoint(): Either<WirespecException, Endpoint> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(token.value).bind()
            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEndpointDefinition(name: String) = either {
        eatToken().bind()
        token.log()
        Endpoint(
            name = name,
            method = when (token.type) {
                is Method -> parseEndpointMethod(token).bind()
                else -> raise(WrongTokenException(Method::class, token).also { eatToken().bind() })
            },
            path = emptyList(),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = emptyList(),
            responses = emptyList(),
        )
    }

    private fun TokenProvider.parseEndpointMethod(token: Token) = either {
        when (token.type as Method) {
            DELETE -> Endpoint.Method.DELETE
            GET -> Endpoint.Method.GET
            HEAD -> Endpoint.Method.HEAD
            OPTIONS -> Endpoint.Method.OPTIONS
            PATCH -> Endpoint.Method.PATCH
            POST -> Endpoint.Method.POST
            PUT -> Endpoint.Method.PUT
            TRACE -> Endpoint.Method.TRACE
        }.also { eatToken().bind() }
    }
}
