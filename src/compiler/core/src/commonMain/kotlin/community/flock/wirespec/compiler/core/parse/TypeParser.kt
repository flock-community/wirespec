package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.Equals
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Pipe
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.WirespecType
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsNumber
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsUnit
import community.flock.wirespec.compiler.utils.Logger

class TypeParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseType(comment: Comment?): Either<WirespecException, Definition> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(comment, Identifier(token.value)).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    fun TokenProvider.parseTypeShape(): Either<WirespecException, Type.Shape> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Field>().apply {
                add(parseField(Identifier(token.value)).bind())
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomValue -> add(parseField(Identifier(token.value)).bind())
                        else -> raise(WrongTokenException<CustomValue>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<CustomValue>(token).also { eatToken().bind() })
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
            }
        }.let(Type::Shape)
    }

    private fun TokenProvider.parseTypeDefinition(comment: Comment?, typeName: Identifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(
                comment = comment,
                identifier = typeName,
                shape = parseTypeShape().bind(),
            )

            is CustomRegex -> Refined(
                comment = comment,
                identifier = typeName,
                validator = Refined.Validator(token.value),
            ).also { eatToken().bind() }

            is Equals -> Union(
                comment = comment,
                identifier = typeName,
                entries = parseUnionTypeEntries().bind(),
            )

            else -> raise(WrongTokenException<LeftCurly>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseField(identifier: Identifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token).also { eatToken().bind() })
        }
        val isDict = when (token.type) {
            is LeftCurly -> true.also { eatToken().bind() }
            else -> false
        }
        when (val type = token.type) {
            is WirespecType -> Field(
                identifier = identifier,
                reference = parseFieldValue(type, token.value, isDict).bind(),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken().bind() }
            ).also {
                if (isDict) {
                    when (token.type) {
                        is RightCurly -> eatToken().bind()
                        else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseFieldValue(wsType: WirespecType, value: String, isDict: Boolean) = either {
        val previousToken = token
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        when (wsType) {
            is WsString -> Field.Reference.Primitive(
                type = Field.Reference.Primitive.Type.String,
                isIterable = isIterable,
                isDictionary = isDict
            )

            is WsInteger -> Field.Reference.Primitive(
                type = Field.Reference.Primitive.Type.Integer,
                isIterable = isIterable,
                isDictionary = isDict
            )

            is WsNumber -> Field.Reference.Primitive(
                type = Field.Reference.Primitive.Type.Number,
                isIterable = isIterable,
                isDictionary = isDict
            )

            is WsBoolean -> Field.Reference.Primitive(
                type = Field.Reference.Primitive.Type.Boolean,
                isIterable = isIterable,
                isDictionary = isDict
            )

            is WsUnit -> Field.Reference.Unit(
                isIterable = isIterable,
                isDictionary = isDict
            )

            is CustomType -> {
                previousToken.shouldBeDefined().bind()
                Field.Reference.Custom(
                    value = value,
                    isIterable = isIterable,
                    isDictionary = isDict
                )
            }
        }
    }

    private fun TokenProvider.parseUnionTypeEntries() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> mutableListOf<Field.Reference>().apply {
                token.shouldBeDefined().bind()
                add(Field.Reference.Custom(token.value, false))
                eatToken().bind()
                while (token.type == Pipe) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomType -> {
                            token.shouldBeDefined().bind()
                            add(Field.Reference.Custom(token.value, false)).also { eatToken().bind() }
                        }

                        else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }.toSet()
    }
}
