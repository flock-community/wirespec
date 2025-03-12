package community.flock.wirespec.compiler.core.parse

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.exceptions.EmptyAST
import community.flock.wirespec.compiler.core.exceptions.UnionError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongTokenException
import community.flock.wirespec.compiler.core.tokenize.ChannelDefinition
import community.flock.wirespec.compiler.core.tokenize.Comment
import community.flock.wirespec.compiler.core.tokenize.EndpointDefinition
import community.flock.wirespec.compiler.core.tokenize.EnumTypeDefinition
import community.flock.wirespec.compiler.core.tokenize.Tokens
import community.flock.wirespec.compiler.core.tokenize.TypeDefinition
import community.flock.wirespec.compiler.core.tokenize.WirespecDefinition
import community.flock.wirespec.compiler.utils.HasLogger

typealias AST = NonEmptyList<Node>

data class ParseOptions(
    val strict: Boolean = false,
    val allowUnions: Boolean = true,
)

object Parser {

    private val typeParser = TypeParser
    private val enumParser = EnumParser
    private val endpointParser = EndpointParser
    private val channelParser = ChannelParser

    fun HasLogger.parse(tokens: Tokens, options: ParseOptions = ParseOptions()): EitherNel<WirespecException, AST> = tokens
        .toProvider(logger)
        .parse()
        .flatMap(validate(options))

    private fun TokenProvider.parse(): EitherNel<WirespecException, AST> = either {
        mutableListOf<EitherNel<WirespecException, Definition>>()
            .apply { while (hasNext()) add(parseDefinition().mapLeft { it.nel() }) }
            .map { it.bind() }
            .toNonEmptyListOrNull()
            .let { ensureNotNull(it) { EmptyAST().nel() } }
    }

    private fun TokenProvider.parseDefinition() = either {
        token.log()
        val comment = when (token.type) {
            is Comment -> Comment(token.value).also { eatToken().bind() }
            else -> null
        }
        when (token.type) {
            is WirespecDefinition -> when (token.type as WirespecDefinition) {
                is TypeDefinition -> with(typeParser) { parseType(comment) }.bind()
                is EnumTypeDefinition -> with(enumParser) { parseEnum(comment) }.bind()
                is EndpointDefinition -> with(endpointParser) { parseEndpoint(comment) }.bind()
                is ChannelDefinition -> with(channelParser) { parseChannel(comment) }.bind()
            }

            else -> raise(WrongTokenException<WirespecDefinition>(token).also { eatToken().bind() })
        }
    }

    private fun validate(options: ParseOptions): (AST) -> EitherNel<WirespecException, AST> = { ast: AST ->
        ast
            .runOption(options.allowUnions) { fillExtendsClause() }
    }

    private fun AST.runOption(bool: Boolean, block: AST.() -> EitherNel<WirespecException, AST>) = if (bool) block() else right()

    private fun AST.fillExtendsClause(): EitherNel<WirespecException, AST> = either {
        map { node ->
            when (node) {
                is Channel -> node
                is Endpoint -> node
                is Enum -> node
                is Refined -> node
                is Type -> node.copy(
                    extends = filterIsInstance<Union>()
                        .filter { union ->
                            union.entries
                                .map {
                                    when (it) {
                                        is Reference.Custom -> it.value
                                        else -> raise(UnionError().nel())
                                    }
                                }
                                .contains(node.identifier.value)
                        }
                        .map { Reference.Custom(value = it.identifier.value, isNullable = false) },
                )

                is Union -> node
            }
        }
    }
}
