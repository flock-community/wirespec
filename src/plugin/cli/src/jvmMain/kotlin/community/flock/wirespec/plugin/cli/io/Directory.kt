package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FullFilePath
import java.io.File

actual class Directory actual constructor(private val path: String) {
    actual fun wirespecFiles(): Set<WirespecFile> = File(path).listFiles()
        .orEmpty()
        .filter { it.isFile }
        .filter { it.extension == FileExtension.Wirespec.ext }
        .map { it.name }
        .map { it.dropLast(FileExtension.Wirespec.ext.length + 1) }
        .map { FullFilePath(path, it) }
        .map(::WirespecFile)
        .toSet()
}
