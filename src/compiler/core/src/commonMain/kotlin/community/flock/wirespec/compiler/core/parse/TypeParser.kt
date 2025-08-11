package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser.parseAnnotations
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.Integer
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.Number
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.Precision
import community.flock.wirespec.compiler.core.tokenize.PrimitiveType
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RegExp
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
import community.flock.wirespec.compiler.core.tokenize.SpecificType
import community.flock.wirespec.compiler.core.tokenize.Token
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

    fun TokenProvider.parseType(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Definition> = parseToken {
        when (token.type) {
            is TypeIdentifier -> parseTypeDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<TypeIdentifier>().bind()
        }
    }

    fun TokenProvider.parseTypeShape(): Either<WirespecException, Type.Shape> = parseToken {
        mutableListOf<Field>().apply {
            // Parse first field with potential annotations
            val firstFieldAnnotations = parseAnnotations().bind()
            when (token.type) {
                is WirespecIdentifier -> add(parseField(FieldIdentifier(token.value), firstFieldAnnotations).bind())
                else -> raiseWrongToken<WirespecIdentifier>().bind()
            }

            // Parse remaining fields
            while (token.type == Comma) {
                eatToken().bind()
                val fieldAnnotations = parseAnnotations().bind()
                when (token.type) {
                    is WirespecIdentifier -> add(parseField(FieldIdentifier(token.value), fieldAnnotations).bind())
                    else -> raiseWrongToken<WirespecIdentifier>().bind()
                }
            }
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raiseWrongToken<RightCurly>().bind()
            }
        }.let(Type::Shape)
    }

    fun TokenProvider.parseDict(): Either<WirespecException, Reference.Dict> = parseToken {
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

    fun TokenProvider.parseType(): Either<WirespecException, Reference> = parseToken { previousToken ->
        val reference = when (previousToken.type) {
            is PrimitiveType -> parsePrimitiveType(previousToken).bind()

            is WsUnit -> {
                Reference.Unit(
                    isNullable = isNullable().bind(),
                )
            }

            is TypeIdentifier -> {
                Reference.Custom(
                    value = previousToken.shouldBeDefined().bind().value,
                    isNullable = isNullable().bind(),
                )
            }

            else -> raiseWrongToken<TypeDefinitionStart>(previousToken).bind()
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

    private fun TokenProvider.parseTypeDefinition(comment: Comment?, annotations: List<Annotation>, typeName: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is Equals -> parseRefinedOrUnion(comment, annotations, typeName).bind()
            is LeftCurly -> Type(
                comment = comment,
                annotations = annotations,
                identifier = typeName,
                shape = parseTypeShape().bind(),
                extends = emptyList(),
            )

            else -> raiseWrongToken<TypeDefinitionStart>().bind()
        }
    }

    private fun TokenProvider.parseRefinedOrUnion(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is SpecificType -> parseRefined(comment, annotations, identifier).bind()
            is TypeIdentifier -> parseUnion(comment, annotations, identifier).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseRefined(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = either {
        when (token.type) {
            is PrimitiveType -> Refined(
                comment = comment,
                annotations = annotations,
                identifier = identifier,
                reference = parsePrimitiveType(eatToken().bind()).bind(),
            )

            else -> raiseWrongToken<SpecificType>().bind()
        }
    }

    private fun TokenProvider.parseUnion(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = either {
        Union(
            comment = comment,
            annotations = annotations,
            identifier = identifier,
            entries = parseUnionTypeEntries().bind(),
        )
    }

    private fun TokenProvider.parseUnionTypeEntries() = either {
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

    private fun TokenProvider.parseTypeConstraint() = parseToken {
        when (token.type) {
            is RegExp -> Reference.Primitive.Type.Constraint.RegExp(token.value).also { eatToken().bind() }
            else -> raiseWrongToken<RegExp>().bind()
        }.also {
            when (token.type) {
                is RightParenthesis -> eatToken().bind()
                else -> raiseWrongToken<RightParenthesis>().bind()
            }
        }
    }

    private inline fun <reified T : TokenType> TokenProvider.parseTypeBound() = parseToken {
        val min = parseTypeBoundValue<T>().bind()
        when (token.type) {
            is Comma -> eatToken().bind()
            else -> raiseWrongToken<Comma>().bind()
        }
        val max = parseTypeBoundValue<T>().bind()
        when (token.type) {
            is RightParenthesis -> eatToken().bind()
            else -> raiseWrongToken<RightParenthesis>().bind()
        }
        Reference.Primitive.Type.Constraint.Bound(min, max)
    }

    private inline fun <reified T : TokenType> TokenProvider.parseTypeBoundValue() = parseToken { previousToken ->
        when (previousToken.type) {
            is T -> previousToken.value
            is Underscore -> null
            else -> raiseWrongToken<T>().bind()
        }
    }

    private fun TokenProvider.parsePrimitiveType(previousToken: Token) = either {
        when (val type = previousToken.type) {
            is WsString -> {
                Reference.Primitive(
                    isNullable = isNullable().bind(),
                    type = Reference.Primitive.Type.String(
                        constraint = when (token.type) {
                            is LeftParenthesis -> parseTypeConstraint().bind()
                            else -> null
                        },
                    ),
                )
            }

            is WsBytes -> {
                Reference.Primitive(
                    type = Reference.Primitive.Type.Bytes,
                    isNullable = isNullable().bind(),
                )
            }

            is WsInteger -> {
                Reference.Primitive(
                    type = Reference.Primitive.Type.Integer(
                        precision = type.precision.toPrimitivePrecision(),
                        constraint = when (token.type) {
                            is LeftParenthesis -> parseTypeBound<Integer>().bind()
                            else -> null
                        },

                    ),
                    isNullable = isNullable().bind(),
                )
            }

            is WsNumber -> {
                Reference.Primitive(
                    type = Reference.Primitive.Type.Number(
                        precision = type.precision.toPrimitivePrecision(),
                        constraint = when (token.type) {
                            is LeftParenthesis -> parseTypeBound<Number>().bind()
                            else -> null
                        },
                    ),
                    isNullable = isNullable().bind(),
                )
            }

            is WsBoolean -> {
                Reference.Primitive(
                    type = Reference.Primitive.Type.Boolean,
                    isNullable = isNullable().bind(),
                )
            }
            else -> raiseWrongToken<PrimitiveType>().bind()
        }
    }

    private fun TokenProvider.parseField(identifier: FieldIdentifier, annotations: List<Annotation> = emptyList()) = parseToken {
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }

        when (token.type) {
            is LeftCurly -> Field(
                identifier = identifier,
                reference = parseDict().bind(),
                annotations = annotations,
            )

            is WirespecType -> Field(
                identifier = identifier,
                reference = parseType().bind(),
                annotations = annotations,
            )

            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.isNullable() = either {
        when (token.type) {
            is QuestionMark -> true.also { eatToken().bind() }
            else -> false
        }
    }
}

fun Precision.toPrimitivePrecision(): Reference.Primitive.Type.Precision = when (this) {
    Precision.P32 -> Reference.Primitive.Type.Precision.P32
    Precision.P64 -> Reference.Primitive.Type.Precision.P64
}
