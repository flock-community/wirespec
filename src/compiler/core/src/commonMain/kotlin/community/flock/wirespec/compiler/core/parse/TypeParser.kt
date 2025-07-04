package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.GenericParserException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.CaseVariant
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParentheses
import community.flock.wirespec.compiler.core.tokenize.Number
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.Precision
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RegExp
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParentheses
import community.flock.wirespec.compiler.core.tokenize.SpecificType
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeDefinitionStart
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier
import community.flock.wirespec.compiler.core.tokenize.Underscore
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit

object TypeParser {

    fun TokenProvider.parseType(comment: Comment?): Either<WirespecException, Definition> = parseToken {
        when (token.type) {
            is TypeIdentifier -> parseTypeDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<TypeIdentifier>().bind()
        }
    }

    fun TokenProvider.parseTypeShape(): Either<WirespecException, Type.Shape> = parseToken {
        when (token.type) {
            is WirespecIdentifier -> mutableListOf<Field>().apply {
                add(parseField(FieldIdentifier(token.value)).bind())
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is WirespecIdentifier -> add(parseField(FieldIdentifier(token.value)).bind())
                        else -> raiseWrongToken<WirespecIdentifier>().bind()
                    }
                }
            }

            else -> raiseWrongToken<WirespecIdentifier>().bind()
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raiseWrongToken<RightCurly>().bind()
            }
        }.let(Type::Shape)
    }

    private fun TokenProvider.parseRefined(
        identifier: DefinitionIdentifier,
        comment: Comment?,
    ): Either<WirespecException, Refined> = either {
        eatToken().bind()
        when (token.type) {
            is SpecificType -> Refined(
                identifier = identifier,
                comment = comment,
                reference = parseType().bind().let {
                    it as? Reference.Primitive
                        ?: raise(
                            GenericParserException(
                                token.coordinates,
                                "Refined types can only be primitive types",
                            ),
                        )
                },
            )

            else -> raise(WrongTokenException<SpecificType>(token))
        }
    }

    fun TokenProvider.parseDict() = parseToken {
        when (token.type) {
            is WirespecType -> Reference.Dict(
                reference = parseType().bind(),
                isNullable = when (token.type) {
                    is RightCurly -> {
                        eatToken().bind()
                        isNullable().bind()
                    }

                    else -> raiseWrongToken<RightCurly>().bind()
                },
            )

            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseTypePattern(): Either<WirespecException, Reference.Primitive.Type.Pattern?> = either {
        when (token.type) {
            is LeftParentheses -> {
                eatToken().bind()
                val pattern = when (token.type) {
                    is CaseVariant -> Reference.Primitive.Type.Pattern.Format(token.value)
                    is RegExp -> Reference.Primitive.Type.Pattern.RegExp(token.value)
                    else -> raise(GenericParserException(token.coordinates, "Expected a RegExp of Literal"))
                }
                pattern.also {
                    eatToken<RightParentheses>().bind()
                    eatToken().bind()
                }
            }
            else -> null
        }
    }

    private inline fun <reified T : TokenType> TokenProvider.parseTypeBound(): Either<WirespecException, Reference.Primitive.Type.Bound?> = either {
        when (token.type) {
            is LeftParentheses -> {
                val min = parseTypeBoundValue<T>().bind()
                eatToken<Comma>().bind()
                val max = parseTypeBoundValue<T>().bind()
                Reference.Primitive.Type.Bound(min, max).also {
                    eatToken<RightParentheses>().bind()
                    eatToken().bind()
                }
            }
            else -> null
        }
    }

    private inline fun <reified T : TokenType> TokenProvider.parseTypeBoundValue(): Either<WirespecException, String?> = either {
        eatToken().bind()
        when (token.type) {
            is T -> token.value
            is Underscore -> null
            else -> raise(
                GenericParserException(
                    token.coordinates,
                    "Bound value must be a ${T::class.simpleName} or an underscore (_). Got ${token.type::class.simpleName} instead.",
                ),
            )
        }
    }

    fun TokenProvider.parseType(): Either<WirespecException, Reference> = either {
        val reference = when (val type = token.type) {
            is WsString -> {
                eatToken().bind()
                Reference.Primitive(
                    isNullable = isNullable().bind(),
                    type = Reference.Primitive.Type.String(
                        pattern = parseTypePattern().bind(),
                    ),
                )
            }

            is WsBytes -> {
                eatToken().bind()
                Reference.Primitive(
                    type = Reference.Primitive.Type.Bytes,
                    isNullable = isNullable().bind(),
                )
            }

            is WsInteger -> {
                eatToken().bind()
                Reference.Primitive(
                    type = Reference.Primitive.Type.Integer(
                        precision = type.precision.toPrimitivePrecision(),
                        bound = parseTypeBound<Integer>().bind(),
                    ),
                    isNullable = isNullable().bind(),
                )
            }

            is WsNumber -> {
                eatToken().bind()
                Reference.Primitive(
                    type = Reference.Primitive.Type.Number(
                        precision = type.precision.toPrimitivePrecision(),
                        bound = parseTypeBound<Number>().bind(),
                    ),
                    isNullable = isNullable().bind(),
                )
            }

            is WsBoolean -> {
                eatToken().bind()
                Reference.Primitive(
                    type = Reference.Primitive.Type.Boolean,
                    isNullable = isNullable().bind(),
                )
            }

            is WsUnit -> {
                eatToken().bind()
                Reference.Unit(
                    isNullable = isNullable().bind(),
                )
            }

            is TypeIdentifier -> {
                token.shouldBeDefined().bind()
                val value = token.value
                eatToken().bind()
                Reference.Custom(
                    value = value,
                    isNullable = isNullable().bind(),
                )
            }

            else -> raise(WrongTokenException<TypeDefinitionStart>(token))
        }
        when (token.type) {
            is Brackets -> {
                eatToken().bind()
                Reference.Iterable(
                    reference = reference,
                    isNullable = isNullable().bind(),
                )
            }
            else -> reference
        }
    }

    private fun TokenProvider.parseTypeDefinition(comment: Comment?, typeName: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is LeftCurly -> Type(
                comment = comment,
                identifier = typeName,
                shape = parseTypeShape().bind(),
                extends = emptyList(),
            )

            is Arrow -> parseRefined(typeName, comment).bind()

            is Equals -> Union(
                comment = comment,
                identifier = typeName,
                entries = parseUnionTypeEntries().bind(),
            )

            else -> raiseWrongToken<TypeDefinitionStart>().bind()
        }
    }

    private fun TokenProvider.parseField(identifier: FieldIdentifier) = parseToken {
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }

        when (token.type) {
            is LeftCurly -> Field(
                identifier = identifier,
                reference = parseDict().bind(),
            )

            is WirespecType -> Field(
                identifier = identifier,
                reference = parseType().bind(),
            )

            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseUnionTypeEntries() = parseToken {
        when (token.type) {
            is TypeIdentifier -> mutableListOf<Reference>().apply {
                token.shouldBeDefined().bind()
                add(Reference.Custom(token.value, false))
                eatToken().bind()
                while (token.type == Pipe) {
                    eatToken().bind()
                    when (token.type) {
                        is TypeIdentifier -> {
                            token.shouldBeDefined().bind()
                            add(Reference.Custom(token.value, false)).also { eatToken().bind() }
                        }

                        else -> raiseWrongToken<TypeIdentifier>().bind()
                    }
                }
            }

            else -> raiseWrongToken<TypeIdentifier>().bind()
        }.toSet()
    }

    private fun TokenProvider.isNullable() = either {
        when (token.type) {
            is QuestionMark -> true.also { eatToken().bind() }
            else -> false
        }
    }
}

fun Precision.toPrimitivePrecision() = when (this) {
    Precision.P32 -> Reference.Primitive.Type.Precision.P32
    Precision.P64 -> Reference.Primitive.Type.Precision.P64
}
