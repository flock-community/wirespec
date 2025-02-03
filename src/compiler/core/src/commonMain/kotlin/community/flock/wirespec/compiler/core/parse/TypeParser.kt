package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.Brackets
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.CustomType
import community.flock.wirespec.compiler.core.tokenize.CustomValue
import community.flock.wirespec.compiler.core.tokenize.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.Equals
import community.flock.wirespec.compiler.core.tokenize.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.Pipe
import community.flock.wirespec.compiler.core.tokenize.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.TypeDefinitionStart
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecType
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsBytes
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsNumber
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsUnit
import community.flock.wirespec.compiler.utils.Logger

class TypeParser(logger: Logger) : AbstractParser(logger) {

    fun TokenProvider.parseType(comment: Comment?): Either<WirespecException, Definition> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    fun TokenProvider.parseTypeShape(): Either<WirespecException, Type.Shape> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Field>().apply {
                add(parseField(FieldIdentifier(token.value)).bind())
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomValue -> add(parseField(FieldIdentifier(token.value)).bind())
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

    private fun TokenProvider.parseRefinedValidator(accumulatedString: String): Either<WirespecException, String> =
        either {
            eatToken().bind()
            token.log()
            when (token.type) {
                is WirespecDefinition, EndOfProgram -> accumulatedString
                else -> parseRefinedValidator(accumulatedString + token.value).bind()
            }
        }

    fun TokenProvider.parseDict() = either {
        eatToken().bind()
        when (val type = token.type) {
            is WirespecType -> Reference.Dict(
                reference = parseWirespecType(type).bind(),
                isNullable = when (token.type) {
                    is RightCurly -> {
                        eatToken().bind()
                        isNullable().bind()
                    }

                    else -> raise(WrongTokenException<RightCurly>(token).also { eatToken().bind() })
                }
            )

            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }

    fun TokenProvider.parseWirespecType(type: WirespecType) = either {
        val current = eatToken().bind()
        val wirespecType = when (type) {
            is WsString -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.String,
                    isNullable = it
                )
            }

            is WsBytes -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Bytes,
                    isNullable = it
                )
            }

            is WsInteger -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Integer(type.precision.toPrimitivePrecision()),
                    isNullable = it
                )
            }

            is WsNumber -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Number(type.precision.toPrimitivePrecision()),
                    isNullable = it
                )
            }

            is WsBoolean -> { it ->
                Reference.Primitive(
                    type = Reference.Primitive.Type.Boolean,
                    isNullable = it
                )
            }

            is WsUnit -> Reference::Unit

            is CustomType -> { it ->
                current.shouldBeDefined().bind()
                Reference.Custom(
                    value = current.value,
                    isNullable = it
                )
            }
        }(isNullable().bind())
        when(token.type){
            is Brackets -> {
                eatToken().bind()
                Reference.Iterable(
                    reference = wirespecType,
                    isNullable = isNullable().bind()
                )
            }
            else -> wirespecType
        }
    }

    private fun TokenProvider.parseTypeDefinition(comment: Comment?, typeName: DefinitionIdentifier) = either {
        eatToken().bind()
        token.log()
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

            else -> raise(WrongTokenException<TypeDefinitionStart>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseField(identifier: FieldIdentifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token).also { eatToken().bind() })
        }

        when (val type = token.type) {
            is LeftCurly -> Field(
                identifier = identifier,
                reference = parseDict().bind(),
                isNullable = null,
            )

            is WirespecType -> Field(
                identifier = identifier,
                reference = parseWirespecType(type).bind(),
                isNullable = null
            )

            else -> raise(WrongTokenException<WirespecType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseUnionTypeEntries() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> mutableListOf<Reference>().apply {
                token.shouldBeDefined().bind()
                add(Reference.Custom(token.value, false))
                eatToken().bind()
                while (token.type == Pipe) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomType -> {
                            token.shouldBeDefined().bind()
                            add(Reference.Custom(token.value, false)).also { eatToken().bind() }
                        }

                        else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
                    }
                }
            }

            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }.toSet()
    }

    private fun TokenProvider.isNullable() = either {
        when (token.type) {
            is QuestionMark -> true.also { eatToken().bind() }
            else -> false
        }
    }
}
