package community.flock.wirespec.compiler.cli.io

import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.closedir
import platform.posix.opendir
import platform.posix.readdir

actual class Directory actual constructor(private val path: String) {
    actual fun wireSpecFiles(): Set<WireSpecFile> {
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
            .filter { it.endsWith(Extension.WireSpec.ext) }
            .map { it.dropLast(Extension.WireSpec.ext.length + 1) }
            .map { DirPath(path, it, Extension.WireSpec) }
            .map(::WireSpecFile)
            .toSet()
    }
}
