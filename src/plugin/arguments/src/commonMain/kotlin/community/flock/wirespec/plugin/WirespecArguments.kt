package community.flock.wirespec.plugin

import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.plugin.io.Source
import community.flock.wirespec.plugin.io.Source.Type
import community.flock.wirespec.plugin.io.Source.Type.JSON
import community.flock.wirespec.plugin.io.Source.Type.Wirespec

sealed interface WirespecArguments {
    val input: NonEmptySet<Source<Type>>
    val emitters: NonEmptySet<Emitter>
    val writer: (NonEmptyList<Emitted>) -> Unit
    val error: (String) -> Unit
    val packageName: PackageName
    val logger: Logger
    val shared: Boolean
    val strict: Boolean
    val ir: Boolean
}

data class CompilerArguments(
    override val input: NonEmptySet<Source<Wirespec>>,
    override val emitters: NonEmptySet<Emitter>,
    override val writer: (NonEmptyList<Emitted>) -> Unit,
    override val error: (String) -> Unit,
    override val packageName: PackageName,
    override val logger: Logger,
    override val shared: Boolean,
    override val strict: Boolean,
    override val ir: Boolean,
) : WirespecArguments

data class ConverterArguments(
    val format: Format,
    override val input: NonEmptySet<Source<JSON>>,
    override val emitters: NonEmptySet<Emitter>,
    override val writer: (NonEmptyList<Emitted>) -> Unit,
    override val error: (String) -> Unit,
    override val packageName: PackageName,
    override val logger: Logger,
    override val shared: Boolean,
    override val strict: Boolean,
    override val ir: Boolean,
) : WirespecArguments

fun PackageName?.toDirectory() = this?.value
    ?.replace('.', '/')
    ?: ""
