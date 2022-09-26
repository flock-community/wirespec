package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.Either
import community.flock.wirespec.compiler.core.either
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.Shape.Field
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value
import community.flock.wirespec.compiler.core.parse.Type.Name
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.*
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
                add(parseField(Field.Key(token.value)))
                while (token.type == Comma) {
                    eatToken()
                    when (token.type) {
                        is CustomValue -> add(parseField(Field.Key(token.value)))
                        else -> throw WrongTokenException(CustomValue::class, token)
                    }
                }
            }

            else -> throw WrongTokenException(CustomValue::class, token)
        }.let(::Shape)
    }

    private fun TokenProvider.parseField(key: Field.Key): Field = run {
        eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> throw WrongTokenException(Colon::class, token)
        }
        when (val type = token.type) {
            is WsType -> Field(
                key = key,
                value = parseFieldValue(type, token.value),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken() }
            )

            else -> throw WrongTokenException(CustomType::class, token)
        }
    }

    private fun TokenProvider.parseFieldValue(wsType: WsType, value: String) = run {
        eatToken()
        token.log()
        val isIterable = (token.type is Brackets).also { if (it) eatToken() }
        when (wsType) {
            is WsString -> Value.Ws(Value.Ws.Type.String, isIterable)
            is WsInteger -> Value.Ws(Value.Ws.Type.Integer, isIterable)
            is WsBoolean -> Value.Ws(Value.Ws.Type.Boolean, isIterable)
            is CustomType -> Value.Custom(value, isIterable)
        }
    }


    private fun TokenProvider.parseEndpointDeclaration(): Endpoint = run {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseEndpointDefinition(token.value)
            else -> throw WrongTokenException(CustomType::class, token)
        }
    }

    private fun TokenProvider.parseEndpointDefinition(typeName: String): Endpoint = run {
        eatToken()
        token.log()
        when (typeName) {
            "GET" -> Endpoint(Endpoint.Verb(typeName), parseEndpointPath(), parseEndpointQuery(), parseEndpointLambda())
            "POST" -> Endpoint(
                Endpoint.Verb(typeName),
                parseEndpointPath(),
                parseEndpointQuery(),
                parseEndpointLambda()
            )

            "PUT" -> Endpoint(Endpoint.Verb(typeName), parseEndpointPath(), parseEndpointQuery(), parseEndpointLambda())
            "DELETE" -> Endpoint(
                Endpoint.Verb(typeName),
                parseEndpointPath(),
                parseEndpointQuery(),
                parseEndpointLambda()
            )

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

            else -> throw WrongTokenException(token.type::class, token)
        }
    }

    private fun TokenProvider.parseEndpointQuery(): Shape? = run {
        when (token.type) {
            is QuestionMark -> {
                eatToken()
                when (token.type) {
                    is LeftCurly -> parseShape()
                    else -> throw WrongTokenException(token.type::class, token)
                }.also {
                    when (token.type) {
                        is RightCurly -> eatToken()
                        else -> throw WrongTokenException(token.type::class, token)
                    }
                }
            }
            else -> null
        }
    }

    private fun TokenProvider.parseEndpointLambda(): Endpoint.Lambda = run {
        if(nextToken?.type is Arrow){
            val input = eatToken()
            eatToken()
            val output = eatToken()
            Endpoint.Lambda(input.value, output.value)
        } else {
            val output = eatToken()
            Endpoint.Lambda(output.value, null)
        }
    }

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
