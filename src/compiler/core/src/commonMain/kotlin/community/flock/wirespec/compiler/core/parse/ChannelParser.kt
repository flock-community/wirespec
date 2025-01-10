package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.utils.Logger

class ChannelParser(logger: Logger) : AbstractParser(logger) {

    private val typeParser = TypeParser(logger)

    fun TokenProvider.parseChannel(comment: Comment?): Either<WirespecException, Channel> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is WirespecType -> parseChannelDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseChannelDefinition(comment: Comment?, identifier: DefinitionIdentifier) = either {
        eatToken().bind()

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token).also { eatToken().bind() })
        }
        val isDict = when (token.type) {
            is LeftCurly -> true.also { eatToken().bind() }
            else -> false
        }
        when (val type = token.type) {
            is WirespecType -> Channel(
                comment = comment,
                identifier = identifier,
                reference = with(typeParser) { parseFieldValue(type, token.value, isDict).bind() },
                isNullable = (token.type is QuestionMark).also { if (it) eatToken().bind() }
            ).also {
                if (isDict) {
                    when (token.type) {
                        is RightCurly -> eatToken().bind()
                        else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }

    }
}
