package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual class Directory actual constructor(private val path: String) {
    actual fun wirespecFiles(): Set<WirespecFile> = fs.readdirSync(path, "utf-8").iterator()
        .asSequence()
        .map { it.unsafeCast<String>() }
        .filter { it.endsWith(Extension.Wirespec.ext) }
        .map { it.dropLast(Extension.Wirespec.ext.length + 1) }
        .map { DirPath(path, it, Extension.Wirespec) }
        .map(::WirespecFile)
        .toSet()
}
