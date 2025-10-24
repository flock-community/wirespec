package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger

interface Emitter : HasExtension {
    fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>
}

interface HasEmitters {
    val emitters: NonEmptySet<Emitter>
}

interface HasExtension {
    val extension: FileExtension
}
