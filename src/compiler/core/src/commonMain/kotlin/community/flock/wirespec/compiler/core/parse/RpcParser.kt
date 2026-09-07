package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.TypeParser.parseType
import community.flock.wirespec.compiler.core.parse.TypeParser.parseTypeShape
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.ExclamationMark
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

internal object RpcParser {

    fun TokenProvider.parseRpc(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Rpc> = parseToken {
        when (token.type) {
            is WirespecType -> parseRpcDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseRpcDefinition(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = parseToken {
        val shape = when (token.type) {
            is LeftCurly -> parseTypeShape().bind()
            else -> raiseWrongToken<LeftCurly>().bind()
        }

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Arrow>().bind()
        }

        val result = parseReference().bind()

        val error = when (token.type) {
            is ExclamationMark -> {
                eatToken().bind()
                parseReference().bind()
            }

            else -> null
        }

        Rpc(
            comment = comment,
            annotations = annotations,
            identifier = identifier,
            shape = shape,
            result = result,
            error = error,
        )
    }

    private fun TokenProvider.parseReference(): Either<WirespecException, Reference> = either {
        when (token.type) {
            is WirespecType -> parseType().bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }
}
