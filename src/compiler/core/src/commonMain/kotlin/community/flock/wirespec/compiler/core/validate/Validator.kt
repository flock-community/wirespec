package community.flock.wirespec.compiler.core.validate

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.flatten
import arrow.core.left
import arrow.core.mapOrAccumulate
import arrow.core.right
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.CompilerException.ParserException.ValidatorException.DefinitionNotExistsValidatorException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Union

fun AST.validate(): Either<NonEmptyList<WirespecException>, AST> = kotlin.run {
    validateReferences()
}

fun AST.validateReferences(): Either<NonEmptyList<DefinitionNotExistsValidatorException>, AST> {

    val typeList = mapNotNull {
        when (it) {
            is Type -> it.name
            is Refined -> it.name
            is Enum -> it.name
            is Union -> it.name
            else -> null
        }
    }

    fun Type.validate(): Either<NonEmptyList<DefinitionNotExistsValidatorException>, Node> {
        return shape.value
            .map { it.reference }
            .mapOrAccumulate {
                if (it is Reference.Custom && !typeList.contains(it.value)) {
                    DefinitionNotExistsValidatorException(it).left().bind<Node>()
                } else {
                    right().bind()
                }
            }
            .map { this }
    }

    fun Endpoint.validate(): Either<NonEmptyList<DefinitionNotExistsValidatorException>, Node> {
        val references = listOf(
            path.mapNotNull {
                when (it) {
                    is Endpoint.Segment.Param -> it.reference
                    is Endpoint.Segment.Literal -> null
                }
            },
            this.query.map { it.reference },
            this.headers.map { it.reference },
            this.cookies.map { it.reference },
            this.requests.mapNotNull { it.content?.reference },
            this.responses.mapNotNull { it.content?.reference },
            this.responses.flatMap { it.headers.map { it.reference } }
        ).flatten()
        return references
            .mapOrAccumulate {
                if (it is Reference.Custom && !typeList.contains(it.value)) {
                    DefinitionNotExistsValidatorException(it).left().bind<Node>()
                } else {
                    right().bind()
                }
            }
            .map { this }
    }

    return this.mapOrAccumulate {
        when (it) {
            is Type -> it.validate().bindNel()
            is Endpoint -> it.validate().bindNel()
            else -> it
        }
    }
}

