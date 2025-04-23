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
        // First validate endpoints
        val endpointValidation = validateEndpoints(ast)
        if (endpointValidation is arrow.core.Either.Left) {
            return endpointValidation
        }

        // Then validate types
        return validateTypes(ast)
    }

    private fun validateEndpoints(ast: AST): EitherNel<WirespecException, AST> {
        val endpointNames = mutableSetOf<String>()

        for (module in ast.modules) {
            for (statement in module.statements) {
                if (statement is Endpoint) {
                    val endpointName = statement.identifier.value
                    if (endpointName in endpointNames) {
                        return DuplicateEndpointError(endpointName).nel().left()
                    }
                    endpointNames.add(endpointName)
                }
            }
        }

        return ast.right()
    }

    private fun validateTypes(ast: AST): EitherNel<WirespecException, AST> {
        for (module in ast.modules) {
            val typeNames = mutableSetOf<String>()

            for (statement in module.statements) {
                if (statement is Type) {
                    val typeName = statement.identifier.value
                    if (typeName in typeNames) {
                        return DuplicateTypeError(typeName).nel().left()
                    }
                    typeNames.add(typeName)
                }
            }
        }

        return ast.right()
    }
}
