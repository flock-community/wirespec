package community.flock.wirespec.compiler.core.emit

import arrow.core.EitherNel
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.exceptions.DefaultsNotSupportedException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

/**
 * Fails for every emitter that does not support default values when the spec has any, naming the
 * fields that have one.
 */
public fun AST.ensureDefaultsSupportedBy(emitters: Iterable<Emitter>): EitherNel<WirespecException, AST> = emitters
    .filterNot { it.supportsDefaults }
    .flatMap { emitter ->
        modules.mapNotNull { module ->
            module.fieldsWithDefaults()
                .toNonEmptyListOrNull()
                ?.let { DefaultsNotSupportedException(module.fileUri, emitter.extension.name, it) }
        }
    }
    .toNonEmptyListOrNull()
    ?.left()
    ?: right()

public fun AST.withoutDefaults(): AST = copy(
    modules = modules.map { module ->
        module.copy(
            statements = module.statements.map { definition ->
                when (definition) {
                    is Type -> definition.copy(shape = Type.Shape(definition.shape.value.map { it.copy(defaultValue = null) }))
                    is Endpoint, is Channel, is Rpc, is Enum, is Union, is Refined -> definition
                }
            },
        )
    },
)

private fun Module.fieldsWithDefaults(): List<String> = statements
    .filterIsInstance<Type>()
    .flatMap { type -> type.shape.value.filter { it.defaultValue != null }.map { "${type.identifier.value}.${it.identifier.value}" } }
