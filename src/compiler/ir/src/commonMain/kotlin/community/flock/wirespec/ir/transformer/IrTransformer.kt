package community.flock.wirespec.ir.transformer

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.IR

fun interface IrTransformer {
    fun transform(ir: IR, ast: AST): IR
}
