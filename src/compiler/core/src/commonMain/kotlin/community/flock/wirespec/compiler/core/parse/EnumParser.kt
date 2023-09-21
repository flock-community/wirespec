package community.flock.wirespec.compiler.core.parse

import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.utils.Logger

class EnumParser(logger: Logger) : AbstractParser(logger) {
    fun TokenProvider.parseEnumTypeDeclaration() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEnumTypeDefinition(token.value).bind()
            else -> raise(
                ParserException.WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel()
            )
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Enum(typeName, parseEnumTypeEntries().bind())
            else -> raise(
                ParserException.WrongTokenException(
                    LeftCurly::class,
                    token
                ).also { eatToken().bind() }.nel()
            )
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raise(
                    ParserException.WrongTokenException(
                        RightCurly::class,
                        token
                    ).also { eatToken().bind() }.nel()
                )
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
                        else -> raise(
                            ParserException.WrongTokenException(CustomType::class, token)
                                .also { eatToken().bind() }.nel()
                        )
                    }
                }
            }

            else -> raise(
                ParserException.WrongTokenException(
                    CustomType::class,
                    token
                ).also { eatToken().bind() }.nel()
            )
        }.toSet()
    }

}
