package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.Precision
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.TypeDefinitionStart
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit
import community.flock.wirespec.compiler.core.tokenize.Comment as CommentToken

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

    private fun TokenProvider.parseRefinedValidator(accumulatedString: String): Either<WirespecException, String> = parseToken {
        when (token.type) {
            is WirespecDefinition, EndOfProgram, CommentToken -> accumulatedString
            else -> parseRefinedValidator(accumulatedString + token.value).bind()
        }
    }

    fun TokenProvider.parseDict() = parseToken {
        when (val type = token.type) {
            is WirespecType -> Reference.Dict(
                reference = parseWirespecType(type).bind(),
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

    fun TokenProvider.parseWirespecType(type: WirespecType) = parseToken { current ->
        val wirespecType = when (type) {
            is WsString -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.String,
                    isNullable = it,
                )
            }

            is WsBytes -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Bytes,
                    isNullable = it,
                )
            }

            is WsInteger -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Integer(type.precision.toPrimitivePrecision()),
                    isNullable = it,
                )
            }

            is WsNumber -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Number(type.precision.toPrimitivePrecision()),
                    isNullable = it,
                )
            }

            is WsBoolean -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Boolean,
                    isNullable = it,
                )
            }

            is WsUnit -> Reference::Unit

            is TypeIdentifier -> { it ->
                current.shouldBeDefined().bind()
                Reference.Custom(
                    value = current.value,
                    isNullable = it,
                )
            }
        }(isNullable().bind())
        when (token.type) {
            is Brackets -> {
                eatToken().bind()
                Reference.Iterable(
                    reference = wirespecType,
                    isNullable = isNullable().bind(),
                )
            }
            else -> wirespecType
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

            is ForwardSlash -> Refined(
                comment = comment,
                identifier = typeName,
                validator = Refined.Validator(parseRefinedValidator("/").bind()),
            )

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

        when (val type = token.type) {
            is LeftCurly -> Field(
                identifier = identifier,
                reference = parseDict().bind(),
            )

            is WirespecType -> Field(
                identifier = identifier,
                reference = parseWirespecType(type).bind(),
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
