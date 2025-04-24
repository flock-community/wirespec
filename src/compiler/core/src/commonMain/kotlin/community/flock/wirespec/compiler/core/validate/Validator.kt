package community.flock.wirespec.compiler.core.validate

import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.nel
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.ValidationContext
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.DuplicateTypeError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type

object Validator {

    fun ValidationContext.validate(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, AST> =
        ast.flatMap {
            zipOrAccumulate(
                it.validateEndpoints(),
                it.validateTypes()
            ) { _, _ ->
                it
            }
        }

    private fun AST.validateEndpoints(): EitherNel<WirespecException, AST> = either {
        val endpointNames = mutableSetOf<String>()
        val errors = mutableListOf<WirespecException>()

        modules.forEach { module ->
            module.statements.filterIsInstance<Endpoint>().forEach { endpoint ->
                val endpointName = endpoint.identifier.value
                if (endpointName in endpointNames) {
                    errors.add(DuplicateEndpointError(endpointName))
                }
                endpointNames.add(endpointName)
            }
        }

        if (errors.isNotEmpty()) {
            raise(errors.toNonEmptyListOrNull() ?: errors.first().nel())
        }

        this@validateEndpoints
    }

    private fun AST.validateTypes(): EitherNel<WirespecException, AST> = either {
        val errors = mutableListOf<WirespecException>()

        modules.forEach { module ->
            val typeNames = mutableSetOf<String>()

            module.statements.filterIsInstance<Type>().forEach { type ->
                val typeName = type.identifier.value
                if (typeName in typeNames) {
                    errors.add(DuplicateTypeError(typeName))
                }
                typeNames.add(typeName)
            }
        }

        if (errors.isNotEmpty()) {
            raise(errors.toNonEmptyListOrNull() ?: errors.first().nel())
        }

        this@validateTypes
    }

    private fun List<WirespecException>.toNonEmptyListOrNull(): NonEmptyList<WirespecException>? =
        if (isNotEmpty()) NonEmptyList(first(), drop(1)) else null
}
