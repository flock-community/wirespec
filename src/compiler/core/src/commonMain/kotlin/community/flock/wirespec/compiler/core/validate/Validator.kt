package community.flock.wirespec.compiler.core.validate

import arrow.core.Either
import arrow.core.Nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Import
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

fun AST.validate(): Either<Nel<WirespecException>, AST> = either {
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
                                    else -> error("Any Unit of Primitive cannot be part of Union")
                                }
                            }
                            .contains(node.identifier.value)
                    }
                    .map {
                        Reference.Custom(
                            value = it.identifier.value,
                            isNullable = false,
                        )
                    },
            )

            is Union -> node
            is Import -> node
        }
    }
}
