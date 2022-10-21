package community.flock.wirespec.compiler.cli.io

import java.io.File

actual class Directory actual constructor(private val path: String) {
    actual fun wireSpecFiles(): Set<WireSpecFile> = File(path).walk()
        .filter { it.isFile }
        .filter { it.extension == Extension.WireSpec.ext }
        .map { it.name }
        .map { it.dropLast(Extension.WireSpec.ext.length + 1) }
        .map { DirPath(path, it, Extension.WireSpec) }
        .map(::WireSpecFile)
        .toSet()
}
