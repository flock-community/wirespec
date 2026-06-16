package community.flock.wirespec.ir.extension

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.emit.IrEmitter

/**
 * Wraps an [IrEmitter] so the given [extensions] are applied to the IR
 * before code generation, without modifying the wrapped emitter.
 */
class ExtendingIrEmitter(
    delegate: IrEmitter,
    override val extensions: List<IrExtension>,
) : IrEmitter by delegate {
    // Route through the IrEmitter default pipeline so this wrapper's
    // extensions are picked up; all other members delegate.
    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger)
}

fun Emitter.applyExtensions(extensions: List<IrExtension>): Emitter = if (this is IrEmitter && extensions.isNotEmpty()) ExtendingIrEmitter(this, extensions) else this
