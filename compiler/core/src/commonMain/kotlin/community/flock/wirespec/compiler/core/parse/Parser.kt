package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.Nel
import arrow.core.ValidatedNel
import arrow.core.continuations.eagerEffect
import arrow.core.nel
import arrow.core.traverse
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.WrongTokenException
import community.flock.wirespec.compiler.core.parse.Type.Name
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Value
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
import community.flock.wirespec.compiler.core.tokenize.types.WsInteger
import community.flock.wirespec.compiler.core.tokenize.types.WsRefinedTypeDef
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsType
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import community.flock.wirespec.compiler.utils.Logger

typealias AST = List<Node>

class Parser(private val logger: Logger) {

    fun parse(tokens: List<Token>): Either<Nel<CompilerException>, List<Definition>> = tokens
        .filterNot { it.type is WhiteSpace }
        .toProvider(logger)
        .parse()
        .toEither()

    private fun TokenProvider.parse(): ValidatedNel<CompilerException, List<Definition>> =
        mutableListOf<ValidatedNel<CompilerException, Definition>>()
            .apply { while (hasNext()) add(parseDefinition()) }
            .traverse { it }

    private fun TokenProvider.parseDefinition() = eagerEffect {
        token.log()
        when (token.type) {
            is WsTypeDef -> parseTypeDeclaration().bind()
            is WsRefinedTypeDef -> parseRefinedTypeDeclaration().bind()
            else -> shift(WrongTokenException(WsTypeDef::class, token).also { eatToken() }.nel())
        }
    }.toValidated()

    private fun TokenProvider.parseTypeDeclaration() = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseTypeDefinition(token.value).bind()
            else -> shift(WrongTokenException(CustomType::class, token).also { eatToken() }.nel())
        }
    }.toValidated()

    private fun TokenProvider.parseTypeDefinition(typeName: String) = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is LeftCurly -> Type(Name(typeName), parseTypeShape().bind())
            else -> shift(WrongTokenException(LeftCurly::class, token).also { eatToken() }.nel())
        }.also {
            when (token.type) {
                is RightCurly -> eatToken()
                else -> shift(WrongTokenException(RightCurly::class, token).also { eatToken() }.nel())
            }
        }
    }.toValidated()

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
                        else -> shift(WrongTokenException(CustomValue::class, token).also { eatToken() }.nel())
                    }
                }
            }

            else -> shift(WrongTokenException(CustomValue::class, token).also { eatToken() }.nel())
        }.let(::Shape)
    }.toValidated()

    private fun TokenProvider.parseField(key: Field.Key) = eagerEffect<Nel<CompilerException>, Field> {
        eatToken()
        token.log()
        when (token.type) {
            is Colon -> eatToken()
            else -> shift(WrongTokenException(Colon::class, token).also { eatToken() }.nel())
        }
        when (val type = token.type) {
            is WsType -> Field(
                key = key,
                value = parseFieldValue(type, token.value),
                isNullable = (token.type is QuestionMark).also { if (it) eatToken() }
            )

            else -> shift(WrongTokenException(CustomType::class, token).also { eatToken() }.nel())
        }
    }.toValidated()

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

    private fun TokenProvider.parseRefinedTypeDeclaration() = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is CustomType -> parseCustomRegex(token.value).bind()
            else -> shift(WrongTokenException(CustomType::class, token).also { eatToken() }.nel())
        }
    }.toValidated()

    private fun TokenProvider.parseCustomRegex(typeName: String) = eagerEffect {
        eatToken()
        token.log()
        when (token.type) {
            is CustomRegex -> Refined(Refined.Name(typeName), Refined.Validator(token.value))
            else -> shift(WrongTokenException(LeftCurly::class, token).also { eatToken() }.nel())
        }.also { eatToken() }
    }.toValidated()

    private fun Token.log() = logger.log("Parsing $type at line ${coordinates.line} position ${coordinates.position}")

}
