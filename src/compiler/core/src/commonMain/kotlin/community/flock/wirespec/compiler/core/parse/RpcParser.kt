package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.TypeParser.parseDict
import community.flock.wirespec.compiler.core.parse.TypeParser.parseType
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.Colon
import community.flock.wirespec.compiler.core.tokenize.Comma
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.LeftParenthesis
import community.flock.wirespec.compiler.core.tokenize.RightParenthesis
import community.flock.wirespec.compiler.core.tokenize.WirespecIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object RpcParser {

    fun TokenProvider.parseRpc(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Rpc> = parseToken {
        when (token.type) {
            is WirespecType -> parseRpcDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseRpcDefinition(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = parseToken {
        val requestParameters = parseRpcParameters().bind()

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Arrow>().bind()
        }

        val response = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }

        Rpc(
            comment = comment,
            annotations = annotations,
            identifier = identifier,
            requestParameters = requestParameters,
            response = response,
        )
    }

    private fun TokenProvider.parseRpcParameters(): Either<WirespecException, List<Field>> = either {
        when (token.type) {
            is LeftParenthesis -> eatToken().bind()
            else -> raiseWrongToken<LeftParenthesis>().bind()
        }
        mutableListOf<Field>().apply {
            if (token.type !is RightParenthesis) {
                add(parseRpcParameter().bind())
                while (token.type is Comma) {
                    eatToken().bind()
                    add(parseRpcParameter().bind())
                }
            }
            when (token.type) {
                is RightParenthesis -> eatToken().bind()
                else -> raiseWrongToken<RightParenthesis>().bind()
            }
        }
    }

    private fun TokenProvider.parseRpcParameter(): Either<WirespecException, Field> = either {
        val identifier = when (token.type) {
            is WirespecIdentifier -> FieldIdentifier(token.value).also { eatToken().bind() }
            else -> raiseWrongToken<WirespecIdentifier>().bind()
        }
        when (token.type) {
            is Colon -> eatToken().bind()
            else -> raiseWrongToken<Colon>().bind()
        }
        val reference = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }
        Field(
            annotations = emptyList(),
            identifier = identifier,
            reference = reference,
        )
    }
}
