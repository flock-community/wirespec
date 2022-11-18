package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.io.Extension.Wirespec

data class DirPath(val directory: String, val fileName: String, val extension: Extension = Wirespec) {
    val fullFilePath get() = "$directory/$fileName.${extension.ext}"
}
