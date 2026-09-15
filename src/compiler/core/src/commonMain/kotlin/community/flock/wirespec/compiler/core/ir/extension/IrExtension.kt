package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.parse.ast.AST

public fun interface IrExtension {
    public fun extend(ir: IR, ast: AST): IR
}
