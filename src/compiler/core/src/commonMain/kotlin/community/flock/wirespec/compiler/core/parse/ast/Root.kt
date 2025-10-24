package community.flock.wirespec.compiler.core.parse.ast

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.FileUri

typealias AST = Root
typealias Statements = NonEmptyList<Definition>

sealed interface Node

data class Root(
    val modules: NonEmptyList<Module>,
) : Node

data class Module(
    val fileUri: FileUri,
    val statements: Statements,
) : Node
