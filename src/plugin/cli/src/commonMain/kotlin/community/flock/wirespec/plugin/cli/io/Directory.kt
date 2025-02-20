package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

private val Path.extension: String
    get() = name.substringAfterLast('.', "")

class Directory(private val path: String) {
    fun wirespecFiles(): Set<WirespecFile> = Path(path)
        .let { SystemFileSystem.list(it) }
        .asSequence()
        .filter { SystemFileSystem.metadataOrNull(it)?.isRegularFile ?: false }
        .filter { it.extension == FileExtension.Wirespec.value }
        .map { it.name }
        .map { it.dropLast(FileExtension.Wirespec.value.length + 1) }
        .map { FullFilePath(path, FileName(it)) }
        .map(::WirespecFile)
        .toSet()
}
