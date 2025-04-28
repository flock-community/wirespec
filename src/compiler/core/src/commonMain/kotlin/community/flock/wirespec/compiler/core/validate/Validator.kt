package community.flock.wirespec.compiler.core.validate

import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.left
import arrow.core.nel
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.exceptions.DuplicateChannelError
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.DuplicateTypeError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type

object Validator {

    fun validate(ast: EitherNel<WirespecException, AST>): EitherNel<WirespecException, AST> = ast
        .flatMap {
            zipOrAccumulate(
                validateEndpoints(it),
                validateTypes(it),
                validateChannels(it),
            ) { _, _, _ -> it }
        }

    private fun validateEndpoints(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { module ->
            module.statements.filterIsInstance<Endpoint>()
        }.groupBy { it.identifier.value }
        .filter { it.value.size > 1 }
        .map { (name, endpoints) -> endpoints.map { DuplicateEndpointError(name) } }
        .flatten()
        .let { errors ->
            if (errors.isEmpty()) {
                ast.right()
            } else {
                (errors.toNonEmptyListOrNull() ?: errors.first().nel()).left()
            }
        }

    private fun validateTypes(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { module ->
            module.statements
                .filterIsInstance<Type>()
                .groupBy { it.identifier.value }
                .filter { it.value.size > 1 }
                .map { (name, types) -> types.map { DuplicateTypeError(name) } }
                .flatten()
        }
        .let { errors ->
            if (errors.isEmpty()) {
                ast.right()
            } else {
                (errors.toNonEmptyListOrNull() ?: errors.first().nel()).left()
            }
        }

    private fun validateChannels(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { module ->
            println(module)
            println(module.statements)
            module.statements.filterIsInstance<Channel>()
        }.groupBy { it.identifier.value }
        .filter { it.value.size > 1 }
        .map { (name, channels) ->
            println("yolo")
            channels.map { DuplicateChannelError(name) }
        }
        .flatten()
        .let { errors ->
            if (errors.isEmpty()) {
                ast.right()
            } else {
                (errors.toNonEmptyListOrNull() ?: errors.first().nel()).left()
            }
        }
}
