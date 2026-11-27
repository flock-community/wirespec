package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.parse.ast.AST

public fun interface IrExtension {
    public fun extend(ir: IR, ast: AST): IR

    /** What keeps this extension from extending the IR of [ast], such as an annotation of its own that names a field a type does not have. */
    public fun validate(ast: AST): List<EmitterException> = emptyList()
}
