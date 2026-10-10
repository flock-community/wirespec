package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.raise.ensure
import community.flock.wirespec.compiler.core.exceptions.AnnotatedPartException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.TypeParser.parseTypeShape
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Comment
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.tokenize.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.WirespecType

internal object PartParser {

    fun TokenProvider.parsePart(comment: Comment?, annotations: List<Annotation>): Either<WirespecException, Part> = parseToken { part ->
        ensure(annotations.isEmpty()) { AnnotatedPartException(fileUri, part.coordinates) }
        when (token.type) {
            is WirespecType -> parsePartDefinition(comment, DefinitionIdentifier(token.value)).bind()
            else -> raiseWrongToken<WirespecType>().bind()
        }
    }

    private fun TokenProvider.parsePartDefinition(comment: Comment?, identifier: DefinitionIdentifier) = parseToken {
        when (token.type) {
            is LeftCurly -> Part(
                comment = comment,
                identifier = identifier,
                shape = parseTypeShape(allowFieldDefaults = false).bind(),
            )

            else -> raiseWrongToken<LeftCurly>().bind()
        }
    }
}
