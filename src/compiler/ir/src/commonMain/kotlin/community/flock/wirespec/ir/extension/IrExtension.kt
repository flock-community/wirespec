package community.flock.wirespec.ir.extension

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.IR

fun interface IrExtension {
    fun transform(ir: IR, ast: AST): IR
}
