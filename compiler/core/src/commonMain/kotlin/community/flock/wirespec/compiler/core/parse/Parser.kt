package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.Shape.Field
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value
import community.flock.wirespec.compiler.core.parse.Type.Name
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.Slash
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace
import community.flock.wirespec.compiler.core.tokenize.types.WsBoolean
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsType
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

class Parser(private val logger: Logger) {

    fun parse(tokens: Iterable<Token>): Either<CompilerException, AST> = either {
        tokens
            .filterWhiteSpace()
            .toProvider(logger)
            .parse()
    }

    private fun Iterable<Token>.filterWhiteSpace() = filterNot { it.type is WhiteSpace }

    private fun TokenProvider.parse(): AST = mutableListOf<Definition>()
        .apply { while (hasNext()) add(parseDefinition()) }

    private fun TokenProvider.parseDefinition(): Definition = run {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration()
            is WsEndpointDef -> parseEndpointDeclaration()
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
            is LeftCurly -> Type(Name(typeName), parseShape())
            else -> throw WrongTokenException(LeftCurly::class, token)
        }.also {
            when (token.type) {
                is RightCurly -> eatToken()
                else -> throw WrongTokenException(RightCurly::class, token)
            }
        }
    }

    private fun TokenProvider.parseShape(): Shape = run {
        eatToken()
        token.log()
        when (token.type) {
            is CustomValue -> mutableListOf<Field>().apply {
                add(parseField())
                while (token.type == Comma) {
                    eatToken()
                    when (token.type) {
                        is CustomValue -> add(parseField())
                        else -> throw WrongTokenException(CustomValue::class, token)
                    }
                }
            }

            else -> throw WrongTokenException(CustomValue::class, token)
        }.let(::Shape)
    }

    private fun TokenProvider.parseField(): Field = run {
        val field = eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> throw WrongTokenException(Colon::class, token)
        }
        when (token.type) {
            is WsType -> Field(
                key = Field.Key(field.value),
                value = parseFieldValue(),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken() }
            )

            else -> throw WrongTokenException(CustomType::class, token)
        }
    }

    private fun TokenProvider.parseFieldValue(): Value = run {
        val ws = eatToken()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken() }
        when (ws.type) {
            is WsString -> Value.Ws(Value.Ws.Type.String, isIterable)
            is WsInteger -> Value.Ws(Value.Ws.Type.Integer, isIterable)
            is WsBoolean -> Value.Ws(Value.Ws.Type.Boolean, isIterable)
            is CustomType -> Value.Custom(ws.value, isIterable)
            else -> error("token not found")
        }
    }


    private fun TokenProvider.parseEndpointDeclaration(): Endpoint = run {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition()
            else -> throw WrongTokenException(CustomType::class, token)
        }
    }

    private fun TokenProvider.parseEndpointDefinition(): Endpoint = run {
        val nameToken = eatToken(CustomType::class)
        val verbToken = eatToken(CustomType::class)
        nameToken.log()
        val name = Endpoint.Name(nameToken.value)
        val verb = Endpoint.Verb(verbToken.value)
        val endpoint = Endpoint(name, verb, parseEndpointPath(), parseEndpointQuery(), parseEndpointLambda())
        when (verbToken.value) {
            "GET" -> endpoint
            "POST" -> endpoint
            "PUT" -> endpoint
            "DELETE" -> endpoint
            else -> throw WrongTokenException(token.type::class, token)
        }
    }

    private fun TokenProvider.parseEndpointPath(): List<Segment> = run {
        token.log()
        when (token.type) {
            is Slash -> mutableListOf<Segment>().apply {
                while (token.type is Slash) {
                    eatToken()
                    when (token.type) {
                        is LeftCurly -> {
                            add(parseShape())
                            when (token.type) {
                                is RightCurly -> eatToken()
                                else -> throw WrongTokenException(token.type::class, token)
                            }
                        }

                        else -> {
                            add(Endpoint.PathSegment(token.value))
                            eatToken()
                        }
                    }
                }
            }.toList()

            else -> throw WrongTokenException(Slash::class, token)
        }
    }

    private fun TokenProvider.parseEndpointQuery(): Shape? = run {
        if (token.type is QuestionMark) {
            eatToken(LeftCurly::class)
            val shape = parseShape()
            eatToken(RightCurly::class)
            shape
        } else {
            null
        }
    }


    private fun TokenProvider.parseEndpointLambda(): Endpoint.Lambda = run {
        fun parseType(): Endpoint.Lambda.Type {
            return Endpoint.Lambda.Type(
                name = eatToken(WsType::class).value,
                isNullable = (token.type is Brackets).also { if (it) eatToken(Brackets::class) },
                isIterable = (token.type is QuestionMark).also { if (it) eatToken(QuestionMark::class) }
            )
        }
        if (nextToken?.type is Arrow) {
            val input = parseType()
            eatToken()
            val output = parseType()
            Endpoint.Lambda(input, output)
        } else {
            val output = parseType()
            Endpoint.Lambda(output, null)
        }
    }

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
