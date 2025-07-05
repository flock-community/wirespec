package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object EnumParser {

    fun TokenProvider.parseEnum(comment: Comment?): Either<WirespecException, Enum> = parseToken {
        when (token.type) {
            is WirespecType -> parseEnumTypeDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(comment: Comment?, typeName: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is LeftCurly -> Enum(
                comment = comment,
                identifier = typeName,
                entries = parseEnumTypeEntries().bind(),
            )

            else -> raiseWrongToken<LeftCurly>().bind()
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raiseWrongToken<RightCurly>().bind()
            }
        }
    }

    private fun TokenProvider.parseEnumTypeEntries() = parseToken {
        when (token.type) {
            is WirespecType -> mutableListOf<String>().apply {
                add(token.value)
                eatToken().bind()
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is WirespecType -> add(token.value).also { eatToken().bind() }
                        else -> raiseWrongToken<WirespecType>().bind()
                    }
                }
            }

            else -> raiseWrongToken<WirespecType>().bind()
        }.toSet()
    }
}
