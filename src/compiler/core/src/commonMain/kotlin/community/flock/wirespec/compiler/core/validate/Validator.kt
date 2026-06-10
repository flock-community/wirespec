package community.flock.wirespec.compiler.core.validate

import arrow.core.Either.Companion.zipOrAccumulate
import arrow.core.EitherNel
import arrow.core.left
import arrow.core.nel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.exceptions.DuplicateChannelError
import community.flock.wirespec.compiler.core.exceptions.DuplicateEndpointError
import community.flock.wirespec.compiler.core.exceptions.DuplicateTypeError
import community.flock.wirespec.compiler.core.exceptions.SpreadCycleError
import community.flock.wirespec.compiler.core.exceptions.SpreadTypeError
import community.flock.wirespec.compiler.core.exceptions.UnionError
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Statements
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union

object Validator {

    fun validate(options: ParseOptions, ast: AST): EitherNel<WirespecException, AST> = either {
        val validated = zipOrAccumulate(
            validateWithOptions(ast, options),
            validateEndpoints(ast),
            validateTypes(ast),
            validateChannels(ast),
        ) { a, _, _, _ -> a }.bind()
        validated.resolveSpreads().bind()
    }

    private fun AST.resolveSpreads(): EitherNel<WirespecException, AST> = either {
        val typesByName = modules
            .flatMap { it.statements }
            .filterIsInstance<Type>()
            .associateBy { it.identifier.value }
        AST(
            modules.map { module ->
                module.copy(
                    statements = module.statements.map { definition ->
                        when (definition) {
                            is Type -> definition.flattenSpread(typesByName, emptySet()).bind()
                            else -> definition
                        }
                    },
                )
            },
        )
    }

    private fun Type.flattenSpread(typesByName: Map<String, Type>, visiting: Set<String>): EitherNel<WirespecException, Type> = either {
        if (spread.isEmpty()) {
            this@flattenSpread
        } else {
            ensure(identifier.value !in visiting) { SpreadCycleError(identifier.value).nel() }
            val ownFields = shape.value
            val merged = mutableListOf<Field>()
            (0..ownFields.size).forEach { position ->
                spread.filter { it.index == position }.forEach { sp ->
                    val target = typesByName[sp.reference.value] ?: raise(SpreadTypeError(sp.reference.value).nel())
                    merged.addAll(target.flattenSpread(typesByName, visiting + identifier.value).bind().shape.value)
                }
                if (position < ownFields.size) merged.add(ownFields[position])
            }
            copy(shape = Type.Shape(merged), spread = emptyList())
        }
    }

    private fun validateWithOptions(ast: AST, options: ParseOptions): EitherNel<WirespecException, AST> = ast.modules
        .map { (uri, statements) -> runValidateOptions(options)(statements).map { Module(uri, it) } }
        .let { either { it.bindAll() } }
        .map { AST(it) }

    private fun runValidateOptions(options: ParseOptions): (Statements) -> EitherNel<WirespecException, Statements> = { it.runOption(options.allowUnions) { fillExtendsClause() } }

    private fun Statements.runOption(bool: Boolean, block: Statements.() -> EitherNel<WirespecException, Statements>) = if (bool) block() else right()

    private fun Statements.fillExtendsClause(): EitherNel<WirespecException, Statements> = either {
        map { definition ->
            when (definition) {
                is Channel -> definition
                is Endpoint -> definition
                is Enum -> definition
                is Refined -> definition
                is Type -> definition.copy(
                    extends = filterIsInstance<Union>()
                        .filter { union ->
                            union.entries
                                .map {
                                    when (it) {
                                        is Reference.Custom -> it.value
                                        else -> raise(UnionError().nel())
                                    }
                                }
                                .contains(definition.identifier.value)
                        }
                        .map { Reference.Custom(value = it.identifier.value, isNullable = false) },
                )

                is Union -> definition
            }
        }
    }

    private fun validateEndpoints(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { it.statements.filterIsInstance<Endpoint>() }
        .groupBy { it.identifier.value }
        .filter { it.value.size > 1 }
        .map { (name, endpoints) -> endpoints.map { DuplicateEndpointError(name) } }
        .flatten()
        .toNonEmptyListOrNull()
        ?.left()
        ?: ast.right()

    private fun validateTypes(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { module ->
            module.statements
                .filterIsInstance<Type>()
                .groupBy { it.identifier.value }
                .filter { it.value.size > 1 }
                .map { (name, types) -> types.map { DuplicateTypeError(name) } }
                .flatten()
        }
        .toNonEmptyListOrNull()
        ?.left()
        ?: ast.right()

    private fun validateChannels(ast: AST): EitherNel<WirespecException, AST> = ast.modules.toList()
        .flatMap { it.statements.filterIsInstance<Channel>() }
        .groupBy { it.identifier.value }
        .filter { it.value.size > 1 }
        .map { (name, channels) -> channels.map { DuplicateChannelError(name) } }
        .flatten()
        .toNonEmptyListOrNull()
        ?.left()
        ?: ast.right()
}
