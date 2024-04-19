package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.cli.js.fs

actual class Directory actual constructor(private val path: String) {
    actual fun wirespecFiles(): Set<WirespecFile> = fs.readdirSync(path, "utf-8").iterator()
        .asSequence()
        .map { it.unsafeCast<String>() }
        .filter { it.endsWith(FileExtension.Wirespec.value) }
        .map { it.dropLast(FileExtension.Wirespec.value.length + 1) }
        .map { FullFilePath(path, FileName(it)) }
        .map(::WirespecFile)
        .toSet()
}
