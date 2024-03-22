package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
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

    fun TokenProvider.parseType(): Either<WirespecException, Type> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(token.value).bind()
            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    fun TokenProvider.parseTypeShape(): Either<WirespecException, Type.Shape> = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Type.Shape.Field>().apply {
                add(parseField(Type.Shape.Field.Identifier(token.value)).bind())
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomValue -> add(parseField(Type.Shape.Field.Identifier(token.value)).bind())
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

    private fun TokenProvider.parseTypeDefinition(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(typeName, parseTypeShape().bind())
            else -> raise(WrongTokenException<LeftCurly>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseField(identifier: Type.Shape.Field.Identifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException<Colon>(token).also { eatToken().bind() })
        }
        when (val type = token.type) {
            is WirespecType -> Type.Shape.Field(
                identifier = identifier,
                reference = parseFieldValue(type, token.value).bind(),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken().bind() }
            )

            else -> raise(WrongTokenException<CustomType>(token).also { eatToken().bind() })
        }
    }

    private fun TokenProvider.parseFieldValue(wsType: WirespecType, value: String) = either {
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        when (wsType) {
            is WsString -> Type.Shape.Field.Reference.Primitive(
                Type.Shape.Field.Reference.Primitive.Type.String,
                isIterable
            )

            is WsInteger -> Type.Shape.Field.Reference.Primitive(
                Type.Shape.Field.Reference.Primitive.Type.Integer,
                isIterable
            )

            is WsNumber -> Type.Shape.Field.Reference.Primitive(
                Type.Shape.Field.Reference.Primitive.Type.Number,
                isIterable
            )

            is WsBoolean -> Type.Shape.Field.Reference.Primitive(
                Type.Shape.Field.Reference.Primitive.Type.Boolean,
                isIterable
            )

            is WsUnit -> Type.Shape.Field.Reference.Unit(isIterable)

            is CustomType -> Type.Shape.Field.Reference.Custom(value, isIterable)
        }
    }
}
