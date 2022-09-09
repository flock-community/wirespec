package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.js.fs

actual class Directory actual constructor(private val path: String) {
    actual fun wireSpecFiles(): Set<WireSpecFile> = fs.readdirSync(path, "utf-8").iterator()
        .asSequence()
        .map { it.unsafeCast<String>() }
        .filter { it.endsWith(Extension.WireSpec.ext) }
        .map { it.dropLast(Extension.WireSpec.ext.length + 1) }
        .map { Path(path, it, Extension.WireSpec) }
        .map(::WireSpecFile)
        .toSet()
}
