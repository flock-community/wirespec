package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.ImportDefinition
import community.flock.wirespec.compiler.core.tokenize.Precision
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.Tokens
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

abstract class AbstractParser(protected val logger: Logger) {
    protected fun Token.log() = logger.debug("Parsing $type at line ${coordinates.line} position ${coordinates.position}")
}

class Parser(logger: Logger) : AbstractParser(logger) {

    private val importParser = ImportParser(logger)
    private val typeParser = TypeParser(logger)
    private val enumParser = EnumParser(logger)
    private val endpointParser = EndpointParser(logger)
    private val channelParser = ChannelParser(logger)

    fun parse(tokens: Tokens): Either<NonEmptyList<WirespecException>, AST> = tokens
        .toProvider(logger)
        .parse()

    private fun TokenProvider.parse(): EitherNel<WirespecException, AST> = either {
        mutableListOf<EitherNel<WirespecException, Node>>()
            .apply { while (hasNext()) add(parseDefinition().mapLeft { it.nel() }) }
            .map { it.bind() }
    }

    private fun TokenProvider.parseDefinition() = either {
        token.log()
        val comment = when (token.type) {
            is Comment -> Comment(token.value).also { eatToken().bind() }
            else -> null
        }
        when (token.type) {
            is WirespecDefinition -> when (token.type as WirespecDefinition) {
                is ImportDefinition -> with(importParser) { parseImport() }.bind()
                is TypeDefinition -> with(typeParser) { parseType(comment) }.bind()
                is EnumTypeDefinition -> with(enumParser) { parseEnum(comment) }.bind()
                is EndpointDefinition -> with(endpointParser) { parseEndpoint(comment) }.bind()
                is ChannelDefinition -> with(channelParser) { parseChannel(comment) }.bind()
            }

            else -> raise(WrongTokenException<WirespecDefinition>(token).also { eatToken().bind() })
        }
    }
}

fun Precision.toPrimitivePrecision() = when (this) {
    Precision.P32 -> Reference.Primitive.Type.Precision.P32
    Precision.P64 -> Reference.Primitive.Type.Precision.P64
}
