package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.DirectoryPath
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private val Path.extension: String
    get() = name.substringAfterLast('.', "")

class Directory(private val path: DirectoryPath) {
    fun wirespecFiles(): Set<WirespecFile> = Path(path.value)
        .let { SystemFileSystem.list(it) }
        .asSequence()
        .filter { SystemFileSystem.metadataOrNull(it)?.isRegularFile ?: false }
        .filter { it.extension == FileExtension.Wirespec.value }
        .map { it.name }
        .map { it.dropLast(FileExtension.Wirespec.value.length + 1) }
        .map { FilePath(path, FileName(it)) }
        .map(::WirespecFile)
        .toSet()
}
