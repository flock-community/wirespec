package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.Logger

interface Emitter {

    val extension: FileExtension

    fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>
}

interface HasEmitters {
    val emitters: NonEmptySet<Emitter>
}
