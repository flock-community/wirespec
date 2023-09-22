package community.flock.wirespec.compiler.core.parse

import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.utils.Logger

class EndpointParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEndpoint() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(token.value).bind()
                .also { eatToken().bind() }

            else -> raise(
                WirespecException.CompilerException.ParserException.WrongTokenException(CustomType::class, token)
                    .also { eatToken().bind() }.nel()
            )
        }
    }

    private fun TokenProvider.parseEndpointDefinition(name: String) = either {
        eatToken().bind()
        token.log()
        Endpoint(
            name = name,
            method = Endpoint.Method.GET,
            path = emptyList(),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = emptyList(),
            responses = emptyList(),
        )
    }
}
