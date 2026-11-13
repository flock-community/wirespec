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

/** The title and version a build gives the spec, such as the version of the project, instead of the ones the spec declares. */
public data class SpecInfo(
    val title: String? = null,
    val version: String? = null,
)

/** An emitter that writes a document with a title and version, such as OpenAPI or AsyncAPI, so a build can set them. */
public interface SpecInfoEmitter : Emitter {
    /** This emitter, writing the title and version of [info] where they are set. */
    public fun withSpecInfo(info: SpecInfo): Emitter
}

/** Emitters that write no title and version, such as the language emitters, have nothing to set and stay as they are. */
public fun Emitter.withSpecInfo(info: SpecInfo): Emitter = when (this) {
    is SpecInfoEmitter -> withSpecInfo(info)
    else -> this
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
