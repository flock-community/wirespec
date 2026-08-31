package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WrongGraphqlKindException
import community.flock.wirespec.compiler.core.parse.TypeParser.parseCurlyFields
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.tokenize.Arrow
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.TypeIdentifier
import community.flock.wirespec.compiler.core.tokenize.WirespecType

object GraphqlParser {

    fun TokenProvider.parseGraphql(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Graphql> = parseToken {
        when (token.type) {
            is WirespecType -> parseGraphqlDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parseGraphqlDefinition(
        comment: Comment?,
        annotations: List<Annotation>,
        identifier: DefinitionIdentifier,
    ) = parseToken {
        val kind = when (token.type) {
            is TypeIdentifier ->
                Graphql.Kind.entries.find { it.name == token.value }
                    ?: raise(WrongGraphqlKindException(fileUri, token))

            else -> raiseWrongToken<TypeIdentifier>().bind()
        }.also { eatToken().bind() }

        val inputs = when (token.type) {
            is LeftCurly -> parseCurlyFields().bind()
            else -> raiseWrongToken<LeftCurly>().bind()
        }

        when (token.type) {
            is Arrow -> eatToken().bind()
            else -> raiseWrongToken<Arrow>().bind()
        }

        val output = with(TypeParser) {
            when (token.type) {
                is LeftCurly -> parseDict().bind()
                is WirespecType -> parseType().bind()
                else -> raiseWrongToken<WirespecType>().bind()
            }
        }

        Graphql(
            comment = comment,
            annotations = annotations,
            identifier = identifier,
            kind = kind,
            inputs = inputs,
            output = output,
        )
    }
}
