package community.flock.wirespec.compiler.core.validate

import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import community.flock.wirespec.compiler.core.ValidationContext
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.tokenize.Token

object Validator {

    fun ValidationContext.validate(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, AST> = ast.flatMap { validateAST(it) }

    private fun validateAST(ast: AST): EitherNel<WirespecException, AST> {
        val allEndpoints = mutableListOf<Pair<String, String>>()

        ast.modules.forEach { module ->
            module.statements.filterIsInstance<Endpoint>().forEach { endpoint ->
                allEndpoints.add(endpoint.identifier.value to module.uri)
            }
        }

        val endpointsByName = allEndpoints.groupBy { it.first }
        val duplicates = endpointsByName.filter { it.value.size > 1 }

        return if (duplicates.isNotEmpty()) {
            val firstDuplicate = duplicates.entries.first()
            val duplicateEndpointName = firstDuplicate.key
            DuplicateEndpointError(Token.Coordinates(), duplicateEndpointName).nel().left()
        } else {
            ast.right()
        }
    }
}
