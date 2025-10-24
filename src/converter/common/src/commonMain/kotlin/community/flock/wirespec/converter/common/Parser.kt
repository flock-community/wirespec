package community.flock.wirespec.converter.common

import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST

interface Parser {
    fun parse(moduleContent: ModuleContent, strict: Boolean): AST
}
