package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object EnumParser {

    fun TokenProvider.parseEnum(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Enum> = parseToken {
        when (token.type) {
            is WirespecType -> parseEnumTypeDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(comment: Comment?, annotations: List<Annotation>, typeName: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is LeftCurly -> Enum(
                comment = comment,
                annotations = annotations,
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

    private fun TokenProvider.isEnumEntry() = token.type is WirespecType || token.type is Integer

    private fun TokenProvider.parseEnumTypeEntries() = parseToken {
        when {
            isEnumEntry() -> mutableListOf<String>().apply {
                add(token.value)
                eatToken().bind()
                while (token.type == Comma) {
                    eatToken().bind()
                    when {
                        isEnumEntry() -> add(token.value).also { eatToken().bind() }
                        else -> raiseWrongToken<WirespecType>().bind()
                    }
                }
            }

            else -> raiseWrongToken<WirespecType>().bind()
        }.toSet()
    }
}
