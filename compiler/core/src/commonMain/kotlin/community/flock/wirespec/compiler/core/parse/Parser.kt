package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Ws
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
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

    fun parse(tokens: List<Token>): Either<CompilerException, AST> = either {
        tokens
            .filterWhiteSpace()
            .toProvider(logger)
            .parse()
    }

    private fun List<Token>.filterWhiteSpace() = filterNot { it.type is WhiteSpace }

    private fun TokenProvider.parse(): AST = mutableListOf<Definition>()
        .apply { while (hasNext()) add(parseDefinition()) }

    private fun TokenProvider.parseDefinition(): Definition = run {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration()
            else -> throw WrongTokenException(WsTypeDef::class, token)
        }
    }

    private fun TokenProvider.parseTypeDeclaration(): Type = run {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(token.value)
            else -> throw WrongTokenException(CustomType::class, token)
        }
    }

    private fun TokenProvider.parseTypeDefinition(typeName: String): Type = run {
        eatToken()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(Type.Name(typeName), parseTypeShape())
            else -> throw WrongTokenException(LeftCurly::class, token)
        }.also {
            when (token.type) {
                is RightCurly -> eatToken()
                else -> throw WrongTokenException(RightCurly::class, token)
            }
        }
    }

    private fun TokenProvider.parseTypeShape(): Shape = run {
        eatToken()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Pair<Shape.Key, Shape.Value>>().apply {
                add(parseKeyValueAsPair(Shape.Key(token.value)))
                while (token.type == Comma) {
                    eatToken()
                    when (token.type) {
                        is CustomValue -> add(parseKeyValueAsPair(Shape.Key(token.value)))
                        else -> throw WrongTokenException(CustomValue::class, token)
                    }
                }
            }

            else -> throw WrongTokenException(CustomValue::class, token)
        }.toMap().let(::Shape)
    }

    private fun TokenProvider.parseKeyValueAsPair(key: Shape.Key): Pair<Shape.Key, Shape.Value> = run {
        eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> throw WrongTokenException(Colon::class, token)
        }
        when (val type = token.type) {
            is WsType -> when (type) {
                is WsString -> Ws(Ws.Type.String, type)
                is WsInteger -> Ws(Ws.Type.Integer, type)
                is WsBoolean -> Ws(Ws.Type.Boolean, type)
                is CustomType -> Custom(token.value, type)
            }.let { key.copy(iterable = it.iterable, nullable = it.nullable) to it }

            else -> throw WrongTokenException(CustomType::class, token)
        }.also { eatToken() }
    }

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
