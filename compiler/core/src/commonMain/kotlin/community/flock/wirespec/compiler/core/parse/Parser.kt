package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Value.Ws
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.Identifier
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.RightCurly
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.WsInteger
import community.flock.wirespec.compiler.core.tokenize.WsString
import community.flock.wirespec.compiler.core.tokenize.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

class Parser(private val logger: Logger) {

    fun parse(tokens: List<Token>): Either<WireSpecException, AST> = either {
        tokens.filterNot { it.type is WhiteSpace }
            .toProvider(logger)
            .parse()
    }

    private fun TokenProvider.parse(): AST = mutableListOf<Definition>()
        .apply { while (hasNext()) add(parseDefinition()) }

    private fun TokenProvider.parseDefinition(): Definition = run {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration()
            else -> throw WrongTokenException(WsTypeDef, token)
        }
    }

    private fun TokenProvider.parseTypeDeclaration(): Type = run {
        eatToken()
        token.log()
        when (token.type) {
            is Identifier -> parseTypeDefinition(token.value)
            else -> throw WrongTokenException(Identifier, token)
        }
    }

    private fun TokenProvider.parseTypeDefinition(typeName: String): Type = run {
        eatToken()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(Type.Name(typeName), parseTypeShape())
            else -> throw WrongTokenException(LeftCurly, token)
        }.also {
            when (token.type) {
                is RightCurly -> eatToken()
                else -> throw WrongTokenException(RightCurly, token)
            }
        }
    }

    private fun TokenProvider.parseTypeShape(): Shape = run {
        eatToken()
        token.log()
        when (token.type) {
            is Identifier -> mutableListOf<Pair<Shape.Key, Shape.Value>>().apply {
                add(parseKeyValueAsPair(Shape.Key(token.value)))
                while (token.type == Comma) {
                    eatToken()
                    add(parseKeyValueAsPair(Shape.Key(token.value)))
                }
            }
            else -> throw WrongTokenException(Identifier, token)
        }.toMap().let(::Shape)
    }

    private fun TokenProvider.parseKeyValueAsPair(key: Shape.Key): Pair<Shape.Key, Shape.Value> = run {
        eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> throw WrongTokenException(Colon, token)
        }
        val value = when (token.type) {
            is WsString -> Ws(Ws.Type.String)
            is WsInteger -> Ws(Ws.Type.Integer)
            is WsBoolean -> Ws(Ws.Type.Boolean)
            is Identifier -> Custom(token.value)
            else -> throw WrongTokenException(Identifier, token)
        }
        eatToken()
        key to value
    }

    private fun Token.log() = logger.log("Parsing $type at line ${index.line} position ${index.position}")

}
