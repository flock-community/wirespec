package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.io.Extension.WireSpec

data class Path(val directory: String, val fileName: String, val extension: Extension = WireSpec) {
    val fullFilePath get() = "$directory/$fileName.${extension.ext}"
}
