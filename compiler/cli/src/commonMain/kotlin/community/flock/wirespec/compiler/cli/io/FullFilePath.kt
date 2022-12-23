package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.io.Extension.Wirespec

data class FullFilePath(val directory: String, val fileName: String, val extension: Extension = Wirespec) {
    override fun toString() = "$directory/$fileName.${extension.ext}"
}
