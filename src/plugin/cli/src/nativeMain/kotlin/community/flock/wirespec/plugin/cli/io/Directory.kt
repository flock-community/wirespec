@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir

actual class Directory actual constructor(private val path: String) {
    actual fun wirespecFiles(): Set<WirespecFile> {
        val filenames = mutableListOf<String>()
        val dir = opendir(path)
        if (dir != null) {
            try {
                var dirPointer = readdir(dir)
                while (dirPointer != null) {
                    filenames.add(dirPointer.pointed.d_name.toKString())
                    dirPointer = readdir(dir)
                }
            } finally {
                closedir(dir)
            }
        }
        return filenames
            .asSequence()
            .filter { it.endsWith(FileExtension.Wirespec.value) }
            .map { it.dropLast(FileExtension.Wirespec.value.length + 1) }
            .map { FullFilePath(path, FileName(it)) }
            .map(::WirespecFile)
            .toSet()
    }
}
