package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import community.flock.wirespec.compiler.core.ValidationContext
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.DuplicateTypeError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type

object Validator {

    fun ValidationContext.validate(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, AST> = ast.flatMap { validateAST(it) }

    private fun validateAST(ast: AST): EitherNel<WirespecException, AST> {
        val endpointErrors = ast.validateEndpoints()
        val typeErrors = ast.validateTypes()

        val allErrors = mutableListOf<WirespecException>()

        if (endpointErrors is arrow.core.Either.Left) {
            allErrors.addAll(endpointErrors.value.all)
        }

        if (typeErrors is arrow.core.Either.Left) {
            allErrors.addAll(typeErrors.value.all)
        }

        return if (allErrors.isNotEmpty()) {
            allErrors.first().nel().left()
                .mapLeft { nel -> arrow.core.NonEmptyList(nel.head, allErrors.drop(1)) }
        } else {
            ast.right()
        }
    }

    private fun AST.validateEndpoints(): EitherNel<WirespecException, AST> {
        val endpointNames = mutableSetOf<String>()
        val errors = mutableListOf<WirespecException>()

        for (module in modules) {
            for (statement in module.statements) {
                if (statement is Endpoint) {
                    val endpointName = statement.identifier.value
                    if (endpointName in endpointNames) {
                        errors.add(DuplicateEndpointError(endpointName))
                    }
                    endpointNames.add(endpointName)
                }
            }
        }

        return if (errors.isNotEmpty()) {
            errors.first().nel().left()
                .mapLeft { nel -> arrow.core.NonEmptyList(nel.head, errors.drop(1)) }
        } else {
            right()
        }
    }

    private fun AST.validateTypes(): EitherNel<WirespecException, AST> {
        val errors = mutableListOf<WirespecException>()

        for (module in modules) {
            val typeNames = mutableSetOf<String>()

            for (statement in module.statements) {
                if (statement is Type) {
                    val typeName = statement.identifier.value
                    if (typeName in typeNames) {
                        errors.add(DuplicateTypeError(typeName))
                    }
                    typeNames.add(typeName)
                }
            }
        }

        return if (errors.isNotEmpty()) {
            errors.first().nel().left()
                .mapLeft { nel -> arrow.core.NonEmptyList(nel.head, errors.drop(1)) }
        } else {
            right()
        }
    }
}
