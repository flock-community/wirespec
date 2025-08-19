package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition

interface BaseEmitter {

    val extension: FileExtension

    val shared: Shared?

    val AST.statements: NonEmptyList<Definition>
        get() = modules.flatMap { module -> module.statements }
}
