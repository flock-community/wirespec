package community.flock.wirespec.compiler.core.ir.extension

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger

/**
 * Wraps an [IrEmitter] so the given [extensions] are applied to the IR after the wrapped
 * emitter's own, before code generation, without modifying the wrapped emitter.
 */
public class ExtendingIrEmitter(
    private val delegate: IrEmitter,
    private val added: List<IrExtension>,
) : IrEmitter by delegate {
    override val extensions: List<IrExtension> = delegate.extensions + added

    override fun validate(ast: AST): List<EmitterException> = delegate.validate(ast) + added.flatMap { it.validate(ast) }

    // Route through the IrEmitter default pipeline so this wrapper's
    // extensions are picked up; all other members delegate.
    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger)
}

public fun IrEmitter.applyExtensions(extensions: NonEmptyList<IrExtension>): IrEmitter = ExtendingIrEmitter(this, extensions)
