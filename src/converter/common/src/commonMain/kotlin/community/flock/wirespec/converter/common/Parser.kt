package community.flock.wirespec.converter.common

import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST

public fun interface Parser {
    public fun parse(moduleContent: ModuleContent, strict: Boolean): AST
}
