package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.Directory
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private val Path.extension: String
    get() = name.substringAfterLast('.', "")

fun Directory.wirespecFiles(): Set<WirespecFile> = Path(path.value)
    .let { SystemFileSystem.list(it) }
    .asSequence()
    .filter { SystemFileSystem.metadataOrNull(it)?.isRegularFile ?: false }
    .filter { it.extension == Wirespec.value }
    .map { it.name }
    .map { it.dropLast(Wirespec.value.length + 1) }
    .map { FilePath(path, FileName(it)) }
    .map(::WirespecFile)
    .toSet()
