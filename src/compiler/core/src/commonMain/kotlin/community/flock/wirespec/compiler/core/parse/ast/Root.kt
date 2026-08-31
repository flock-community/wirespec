package community.flock.wirespec.compiler.core.parse.ast

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.FileUri

public typealias AST = Root
public typealias Statements = NonEmptyList<Definition>

public sealed interface Node

public data class Root(
    val modules: NonEmptyList<Module>,
) : Node

public data class Module(
    val fileUri: FileUri,
    val statements: Statements,
) : Node
