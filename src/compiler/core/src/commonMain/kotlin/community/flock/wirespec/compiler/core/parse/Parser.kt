package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.exceptions.EmptyModule
import community.flock.wirespec.compiler.core.exceptions.UnionError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.TokenType
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.compiler.utils.HasLogger

data class ParseOptions(
    val strict: Boolean = false,
    val allowUnions: Boolean = true,
)

object Parser {

    fun HasLogger.parse(modules: NonEmptyList<TokenizedModule>, options: ParseOptions = ParseOptions()): EitherNel<WirespecException, AST> = modules
        .map { it.tokens.toProvider(logger).parseModule(it.src, options) }
        .let { either { it.bindAll() } }
        .map { AST(it) }
        .let(Validator::validate)

    private fun TokenProvider.parseModule(src: String, options: ParseOptions): EitherNel<WirespecException, Module> = either {
        mutableListOf<EitherNel<WirespecException, Definition>>()
            .apply { while (hasNext()) add(parseDefinition().mapLeft { it.nel() }) }
            .map { it.bind() }
            .toNonEmptyListOrNull()
            .let { ensureNotNull(it) { EmptyModule().nel() } }
    }.flatMap(validate(options))
        .map { Module(src, it) }

    private fun TokenProvider.parseDefinition() = either {
        val comment = when (token.type) {
            is Comment -> Comment(token.value).also { eatToken().bind() }
            else -> null
        }
        when (token.type) {
            is WirespecDefinition -> when (token.type as WirespecDefinition) {
                is TypeDefinition -> with(TypeParser) { parseType(comment) }.bind()
                is EnumTypeDefinition -> with(EnumParser) { parseEnum(comment) }.bind()
                is EndpointDefinition -> with(EndpointParser) { parseEndpoint(comment) }.bind()
                is ChannelDefinition -> with(ChannelParser) { parseChannel(comment) }.bind()
            }

            else -> raiseWrongToken<WirespecDefinition>().bind()
        }
    }

    fun validate(options: ParseOptions): (Statements) -> EitherNel<WirespecException, Statements> = { defs: Statements ->
        defs.runOption(options.allowUnions) { fillExtendsClause() }
    }

    private fun Statements.runOption(bool: Boolean, block: Statements.() -> EitherNel<WirespecException, Statements>) = if (bool) block() else right()

    private fun Statements.fillExtendsClause(): EitherNel<WirespecException, Statements> = either {
        map { definition ->
            when (definition) {
                is Channel -> definition
                is Endpoint -> definition
                is Enum -> definition
                is Refined -> definition
                is Type -> definition.copy(
                    extends = filterIsInstance<Union>()
                        .filter { union ->
                            union.entries
                                .map {
                                    when (it) {
                                        is Reference.Custom -> it.value
                                        else -> raise(UnionError().nel())
                                    }
                                }
                                .contains(definition.identifier.value)
                        }
                        .map { Reference.Custom(value = it.identifier.value, isNullable = false) },
                )

                is Union -> definition
            }
        }
    }
}

fun <A> TokenProvider.parseToken(block: Raise<WirespecException>.(Token) -> A) = either {
    block(eatToken().bind())
}

inline fun <reified T : TokenType> TokenProvider.raiseWrongToken(token: Token? = null): Either<WirespecException, Nothing> = either {
    raise(WrongTokenException<T>(token ?: this@raiseWrongToken.token).also { eatToken().bind() })
}
