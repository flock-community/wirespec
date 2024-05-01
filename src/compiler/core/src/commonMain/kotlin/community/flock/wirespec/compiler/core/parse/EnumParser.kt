package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.utils.Logger

class EnumParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseEnum(): Either<WirespecException, Enum> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEnumTypeDefinition(Identifier(token.value)).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(typeName: Identifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Enum(typeName, parseEnumTypeEntries().bind())
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
            is CustomType -> mutableListOf<String>().apply {
                add(token.value)
                eatToken().bind()
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomType -> add(token.value).also { eatToken().bind() }
                        else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }.toSet()
    }
}
