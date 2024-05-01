package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.Tokens
import community.flock.wirespec.compiler.core.tokenize.removeWhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WirespecDefinition
import community.flock.wirespec.compiler.core.tokenize.types.WsComment
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

abstract class AbstractParser(protected val logger: Logger) {
    protected fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")
}

class Parser(logger: Logger) : AbstractParser(logger) {

    private val typeParser = TypeParser(logger)
    private val enumParser = EnumParser(logger)
    private val endpointParser = EndpointParser(logger)

    fun parse(tokens: Tokens): Either<NonEmptyList<WirespecException>, AST> = tokens
        .removeWhiteSpace()
        .toProvider(logger)
        .parse()

    private fun TokenProvider.parse(): EitherNel<WirespecException, AST> = either {
        mutableListOf<EitherNel<WirespecException, Definition>>()
            .apply { while (hasNext()) add(parseDefinition().mapLeft { it.nel() }) }
            .map { it.bind() }
    }

    private fun TokenProvider.parseDefinition() = either {
        token.log()
        val comment = when (token.type) {
            is WsComment -> Comment(token.value).also { eatToken().bind() }
            else -> null
        }
        when (token.type) {
            is WirespecDefinition -> when (token.type as WirespecDefinition) {
                is WsTypeDef -> with(typeParser) { parseType(comment) }.bind()
                is WsEnumTypeDef -> with(enumParser) { parseEnum(comment) }.bind()
                is WsEndpointDef -> with(endpointParser) { parseEndpoint(comment) }.bind()
            }

            else -> raise(WrongTokenException<WirespecDefinition>(token).also { eatToken().bind() })
        }
    }
}
