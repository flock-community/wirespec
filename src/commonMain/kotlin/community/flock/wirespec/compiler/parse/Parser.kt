package community.flock.wirespec.compiler.parse

import community.flock.wirespec.WireSpecException.CompilerException.ParserException
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition.Shape
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition.Shape.Value.Custom
import community.flock.wirespec.compiler.parse.Definition.TypeDefinition.Shape.Value.Ws
import community.flock.wirespec.compiler.tokenize.Colon
import community.flock.wirespec.compiler.tokenize.Comma
import community.flock.wirespec.compiler.tokenize.Identifier
import community.flock.wirespec.compiler.tokenize.LeftCurly
import community.flock.wirespec.compiler.tokenize.RightCurly
import community.flock.wirespec.compiler.tokenize.Token
import community.flock.wirespec.compiler.tokenize.Whitespace
import community.flock.wirespec.compiler.tokenize.WsInteger
import community.flock.wirespec.compiler.tokenize.WsString
import community.flock.wirespec.compiler.tokenize.WsType
import community.flock.wirespec.compiler.utils.log

typealias AST = List<Node>

fun List<Token>.parse(): AST = filterNot { it.type is Whitespace }
    .toProvider()
    .parse()

private fun TokenProvider.parse(): AST = mutableListOf<Definition>()
    .apply { while (hasNext()) add(parseDefinition()) }

private fun TokenProvider.parseDefinition(): Definition = run {
    log("Parsing Definition starting with token: '${token.type}'")
    when (token.type) {
        is WsType -> parseTypeDeclaration()
        else -> throw ParserException("Statement does not start with type declaration: '${token.type}'")
    }
}

private fun TokenProvider.parseTypeDeclaration(): TypeDefinition = run {
    eatToken()
    log("Parsing Type Declaration statement with Token: ${token.type}")
    when (token.type) {
        is Identifier -> parseTypeDefinition(token.value)
        else -> throw ParserException("Type declaration didn't start with an Identifier Token: ${token.type}")
    }
}

private fun TokenProvider.parseTypeDefinition(typeName: String): TypeDefinition = run {
    eatToken()
    log("Parsing Type Definition with Token: ${token.type}")
    when (token.type) {
        is LeftCurly -> TypeDefinition(name = TypeDefinition.Name(typeName), shape = parseTypeShape())
        else -> throw ParserException("Type declaration should start with a curly brace '{' not a: ${token.type}")
    }.also {
        when (token.type) {
            is RightCurly -> eatToken()
            else -> throw ParserException("Type Definition shape should be closed with a '}' found: ${token.type}")
        }
    }
}

private fun TokenProvider.parseTypeShape(): Shape = run {
    eatToken()
    log("Parsing Type Shape with Token: ${token.type}")
    val shape = when (token.type) {
        is Identifier -> {
            val keyValuePairs = mutableListOf<Pair<Shape.Key, Shape.Value>>()
            keyValuePairs.add(parseKeyValueAsPair(Shape.Key(token.value)))
            while (token.type == Comma) {
                eatToken()
                keyValuePairs.add(parseKeyValueAsPair(Shape.Key(token.value)))
            }
            keyValuePairs
        }
        else -> throw ParserException("Type shape should start with an Identifier Token not: ${token.type}")
    }
    Shape(shape.toMap())
}

private fun TokenProvider.parseKeyValueAsPair(key: Shape.Key): Pair<Shape.Key, Shape.Value> = run {
    eatToken()
    log("Parsing Colon Token: ${token.type}")
    when (token.type) {
        is Colon -> eatToken()
        else -> throw ParserException("Colon expected, not: ${token.type}")
    }
    val value = when (token.type) {
        is WsString -> Ws(Ws.Type.String)
        is WsInteger -> Ws(Ws.Type.Integer)
        is Identifier -> Custom(token.value)
        else -> throw ParserException("Value expected: ${token.type}")
    }
    eatToken()
    key to value
}
