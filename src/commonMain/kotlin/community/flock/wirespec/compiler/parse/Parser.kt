package community.flock.wirespec.compiler.parse

import community.flock.wirespec.WireSpecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.parse.Type.Shape
import community.flock.wirespec.compiler.parse.Type.Shape.Value.Custom
import community.flock.wirespec.compiler.parse.Type.Shape.Value.Ws
import community.flock.wirespec.compiler.tokenize.Colon
import community.flock.wirespec.compiler.tokenize.Comma
import community.flock.wirespec.compiler.tokenize.Identifier
import community.flock.wirespec.compiler.tokenize.LeftCurly
import community.flock.wirespec.compiler.tokenize.RightCurly
import community.flock.wirespec.compiler.tokenize.Token
import community.flock.wirespec.compiler.tokenize.Whitespace
import community.flock.wirespec.compiler.tokenize.WsBoolean
import community.flock.wirespec.compiler.tokenize.WsInteger
import community.flock.wirespec.compiler.tokenize.WsString
import community.flock.wirespec.compiler.tokenize.WsTypeDef
import community.flock.wirespec.compiler.utils.log

typealias AST = List<Node>

fun List<Token>.parse(): AST = filterNot { it.type is Whitespace }
    .toProvider()
    .parse()

private fun TokenProvider.parse(): AST = mutableListOf<Definition>()
    .apply { while (hasNext()) add(parseDefinition()) }

private fun TokenProvider.parseDefinition(): Definition = run {
    log(token)
    when (token.type) {
        is WsTypeDef -> parseTypeDeclaration()
        else -> throw WrongTokenException(WsTypeDef, token)
    }
}

private fun TokenProvider.parseTypeDeclaration(): Type = run {
    eatToken()
    log(token)
    when (token.type) {
        is Identifier -> parseTypeDefinition(token.value)
        else -> throw WrongTokenException(Identifier, token)
    }
}

private fun TokenProvider.parseTypeDefinition(typeName: String): Type = run {
    eatToken()
    log(token)
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
    log(token)
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
    log(token)
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

private fun log(token: Token) = log("Parsing ${token.type} at position ${token.index}")
