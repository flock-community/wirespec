package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.TypeParser.parseTypeShape
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

internal object PartParser {

    fun TokenProvider.parsePart(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Part> = parseToken {
        when (token.type) {
            is WirespecType -> parsePartDefinition(comment, annotations, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parsePartDefinition(comment: Comment?, annotations: List<Annotation>, identifier: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is LeftCurly -> Part(
                comment = comment,
                annotations = annotations,
                identifier = identifier,
                shape = parseTypeShape().bind(),
            )

            else -> raiseWrongToken<LeftCurly>().bind()
        }
    }
}
