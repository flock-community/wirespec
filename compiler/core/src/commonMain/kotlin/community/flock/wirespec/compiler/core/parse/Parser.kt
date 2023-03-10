package community.flock.wirespec.compiler.core.parse

import arrow.core.continuations.eagerEffect
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.TypeDefinition.Name
import community.flock.wirespec.compiler.core.parse.Shape.Field
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsType
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

class Parser(private val logger: Logger) {

    fun parse(tokens: List<Token>) = eagerEffect {
        tokens
            .filterNot { it.type is WhiteSpace }
            .toProvider(logger)
            .parse().bind()
    }.toValidated()

    private fun TokenProvider.parse() = eagerEffect<CompilerException, AST> {
        mutableListOf<Definition>()
            .apply { while (hasNext()) add(parseDefinition().bind()) }
    }

    private fun TokenProvider.parseDefinition() = eagerEffect<CompilerException, Definition> {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration().bind()
            else -> shift(WrongTokenException(WsTypeDef::class, token))
        }
    }

    private fun TokenProvider.parseTypeDeclaration() = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(token.value).bind()
            else -> shift(WrongTokenException(CustomType::class, token))
        }
    }

    private fun TokenProvider.parseTypeDefinition(typeName: String) = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is LeftCurly -> TypeDefinition(Name(typeName), parseTypeShape().bind())
            else -> shift(WrongTokenException(LeftCurly::class, token))
        }.also {
            when (token.type) {
                is RightCurly -> eatToken()
                else -> shift(WrongTokenException(RightCurly::class, token))
            }
        }
    }

    private fun TokenProvider.parseTypeShape() = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Field>().apply {
                add(parseField(Field.Key(token.value)).bind())
                while (token.type == Comma) {
                    eatToken()
                    when (token.type) {
                        is CustomValue -> add(parseField(Field.Key(token.value)).bind())
                        else -> shift(WrongTokenException(CustomValue::class, token))
                    }
                }
            }

            else -> shift(WrongTokenException(CustomValue::class, token))
        }.let(::Shape)
    }

    private fun TokenProvider.parseField(key: Field.Key) = eagerEffect<CompilerException, Field> {
        eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> shift(WrongTokenException(Colon::class, token))
        }
        when (val type = token.type) {
            is WsType -> Field(
                key = key,
                value = parseFieldValue(type, token.value),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken() }
            )

            else -> shift(WrongTokenException(CustomType::class, token))
        }
    }

    private fun TokenProvider.parseFieldValue(wsType: WsType, value: String) = run {
        eatToken()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken() }
        when (wsType) {
            is WsString -> Value.Primitive(Value.Primitive.PrimitiveType.String, isIterable)
            is WsInteger -> Value.Primitive(Value.Primitive.PrimitiveType.Integer, isIterable)
            is WsBoolean -> Value.Primitive(Value.Primitive.PrimitiveType.Boolean, isIterable)
            is CustomType -> Value.Custom(value, isIterable)
        }
    }

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
