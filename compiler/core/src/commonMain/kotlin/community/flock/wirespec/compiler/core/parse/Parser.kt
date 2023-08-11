package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsEnumTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsRefinedTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsType
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

class Parser(private val logger: Logger) {

    fun parse(tokens: NonEmptyList<Token>): Either<NonEmptyList<WirespecException>, List<Definition>> = tokens
        .filterNot { it.type is WhiteSpace }
        .toProvider(logger)
        .parse()

    private fun TokenProvider.parse(): EitherNel<WirespecException, List<Definition>> = either {
        mutableListOf<Either<NonEmptyList<WirespecException>, Definition>>()
            .apply { while (hasNext()) add(parseDefinition()) }
            .map { it.bind() }
    }

    private fun TokenProvider.parseDefinition() = either {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration().bind()
            is WsEnumTypeDef -> parseEnumTypeDeclaration().bind()
            is WsRefinedTypeDef -> parseRefinedTypeDeclaration().bind()
            else -> raise(WrongTokenException(WsTypeDef::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseTypeDeclaration() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(token.value).bind()
            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseTypeDefinition(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(typeName, parseTypeShape().bind())
            else -> raise(WrongTokenException(LeftCurly::class, token).also { eatToken().bind() }.nel())
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raise(WrongTokenException(RightCurly::class, token).also { eatToken().bind() }.nel())
            }
        }
    }

    private fun TokenProvider.parseTypeShape() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Field>().apply {
                add(parseField(Field.Identifier(token.value)).bind())
                while (token.type == Comma) {
                    eatToken().bind()
                    when (token.type) {
                        is CustomValue -> add(parseField(Field.Identifier(token.value)).bind())
                        else -> raise(WrongTokenException(CustomValue::class, token).also { eatToken().bind() }.nel())
                    }
                }
            }

            else -> raise(WrongTokenException(CustomValue::class, token).also { eatToken().bind() }.nel())
        }.let(::Shape)
    }

    private fun TokenProvider.parseField(identifier: Field.Identifier) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raise(WrongTokenException(Colon::class, token).also { eatToken().bind() }.nel())
        }
        when (val type = token.type) {
            is WsType -> Field(
                identifier = identifier,
                reference = parseFieldValue(type, token.value).bind(),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken().bind() }
            )

            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseFieldValue(wsType: WsType, value: String) = either {
        eatToken().bind()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken().bind() }
        when (wsType) {
            is WsString -> Reference.Primitive(Reference.Primitive.Type.String, isIterable)
            is WsInteger -> Reference.Primitive(Reference.Primitive.Type.Integer, isIterable)
            is WsBoolean -> Reference.Primitive(Reference.Primitive.Type.Boolean, isIterable)
            is CustomType -> Reference.Custom(value, isIterable)
        }
    }

    private fun TokenProvider.parseEnumTypeDeclaration() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseEnumTypeDefinition(token.value).bind()
            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseEnumTypeDefinition(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is LeftCurly -> Enum(typeName, parseEnumTypeEntries().bind())
            else -> raise(WrongTokenException(LeftCurly::class, token).also { eatToken().bind() }.nel())
        }.also {
            when (token.type) {
                is RightCurly -> eatToken().bind()
                else -> raise(WrongTokenException(RightCurly::class, token).also { eatToken().bind() }.nel())
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
                        else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
                    }
                }
            }

            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }.toSet()
    }

    private fun TokenProvider.parseRefinedTypeDeclaration() = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomType -> parseCustomRegex(token.value).bind()
            else -> raise(WrongTokenException(CustomType::class, token).also { eatToken().bind() }.nel())
        }
    }

    private fun TokenProvider.parseCustomRegex(typeName: String) = either {
        eatToken().bind()
        token.log()
        when (token.type) {
            is CustomRegex -> Refined(typeName, Refined.Validator(token.value))
            else -> raise(WrongTokenException(CustomRegex::class, token).also { eatToken().bind() }.nel())
        }.also { eatToken().bind() }
    }

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
