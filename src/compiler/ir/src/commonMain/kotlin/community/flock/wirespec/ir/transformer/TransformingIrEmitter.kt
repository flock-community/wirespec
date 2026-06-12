package community.flock.wirespec.ir.transformer

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.emit.IrEmitter

/**
 * Wraps an [IrEmitter] so the given [transformers] are applied to the IR
 * before code generation, without modifying the wrapped emitter.
 */
class TransformingIrEmitter(
    delegate: IrEmitter,
    override val transformers: List<IrTransformer>,
) : IrEmitter by delegate {
    // Route through the IrEmitter default pipeline so this wrapper's
    // transformers are picked up; all other members delegate.
    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger)
}

fun Emitter.applyTransformers(transformers: List<IrTransformer>): Emitter = if (this is IrEmitter && transformers.isNotEmpty()) TransformingIrEmitter(this, transformers) else this
