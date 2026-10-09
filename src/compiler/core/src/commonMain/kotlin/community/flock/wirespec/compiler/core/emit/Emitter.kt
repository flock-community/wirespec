package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger

public interface Emitter : HasExtension {
    public fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>

    /**
     * Whether this emitter writes field default values. Compiling a spec with defaults for an
     * emitter that does not is an error, unless the defaults are ignored.
     */
    public val supportsDefaults: Boolean get() = false
}

public interface HasEmitters {
    public val emitters: NonEmptySet<Emitter>
}

public interface HasExtension {
    public val extension: FileExtension
}
