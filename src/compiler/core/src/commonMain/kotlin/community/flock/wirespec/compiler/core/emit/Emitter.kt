package community.flock.wirespec.compiler.core.emit

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger

public interface Emitter : HasExtension {
    public fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>

    /**
     * Whether this emitter writes field default values. Compiling a spec with defaults for an
     * emitter that does not is an error, unless the defaults are ignored.
     */
    public val supportsDefaults: Boolean get() = false

    /** What keeps this emitter from emitting [ast], such as an annotation that names something the spec does not declare. */
    public fun validate(ast: AST): List<EmitterException> = emptyList()
}

public fun AST.ensureValidFor(emitters: Iterable<Emitter>): EitherNel<WirespecException, AST> = emitters
    .flatMap { it.validate(this) }
    .toNonEmptyListOrNull()
    ?.left()
    ?: right()

public interface HasEmitters {
    public val emitters: NonEmptySet<Emitter>
}

public interface HasExtension {
    public val extension: FileExtension
}
