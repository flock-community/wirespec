package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.utils.Logger

class EnumParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEnum(comment: Comment?): Either<WirespecException, Enum> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is WirespecType -> parseEnumTypeDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(comment: Comment?, typeName: DefinitionIdentifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Enum(
                comment = comment,
                identifier = typeName,
                entries = parseEnumTypeEntries().bind()
            )

            else -> raise(WrongTokenException<LeftCurly>(token).also { eatToken().bind() })
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
            }
        }
    }

    private fun TokenProvider.parseEnumTypeEntries() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is WirespecType -> mutableListOf<String>().apply {
                add(token.value)
                eatToken().bind()
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is WirespecType -> add(token.value).also { eatToken().bind() }
                        else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }.toSet()
    }
}
