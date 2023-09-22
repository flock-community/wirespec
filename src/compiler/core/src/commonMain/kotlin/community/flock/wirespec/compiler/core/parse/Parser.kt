package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.core.parse.nodes.Node
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.removeWhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsRefinedTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

abstract class AbstractParser(protected val logger: Logger) {
    protected fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")
}

class Parser(logger: Logger) : AbstractParser(logger) {

    private val typeParser = TypeParser(logger)
    private val enumParser = EnumParser(logger)
    private val refinedTypeParser = RefinedTypeParser(logger)
    private val endpointParser = EndpointParser(logger)

    fun parse(tokens: NonEmptyList<Token>): Either<NonEmptyList<WirespecException>, List<Definition>> = tokens
        .removeWhiteSpace()
        .toProvider(logger)
        .parse()

    private fun TokenProvider.parse(): EitherNel<WirespecException, List<Definition>> = either {
        mutableListOf<Either<NonEmptyList<WirespecException>, Definition>>()
            .apply { while (hasNext()) add(parseDefinition()) }
            .map { it.bind() }
    }

    private fun TokenProvider.parseDefinition() = either {
        token.log()
        when (token.type) {
            is WsTypeDef -> with(typeParser) { parseType() }.bind()
            is WsEnumTypeDef -> with(enumParser) { parseEnum() }.bind()
            is WsRefinedTypeDef -> with(refinedTypeParser) { parseRefinedType() }.bind()
            is WsEndpointDef -> with(endpointParser) { parseEndpoint() }.bind()
            else -> raise(WrongTokenException(WsTypeDef::class, token).also { eatToken().bind() }.nel())
        }
    }
}
