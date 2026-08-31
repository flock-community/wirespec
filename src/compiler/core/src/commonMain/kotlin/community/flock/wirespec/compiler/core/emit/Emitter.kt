package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger

public interface Emitter : HasExtension {
    public fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>
}

public interface HasEmitters {
    public val emitters: NonEmptySet<Emitter>
}

public interface HasExtension {
    public val extension: FileExtension
}
