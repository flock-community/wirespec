package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.io.Extension.Wirespec

data class FullFilePath(val directory: String, val fileName: String, val extension: Extension = Wirespec) {
    companion object{
        fun fromString(input: String): FullFilePath {
            val regex = """^(.*[\\/])([^\\/]+?)(\.[^.]*$|$)""".toRegex()
            val matchResult = regex.find(input) ?: error("Invalid file path format")
            val (path, filename, extension) = matchResult.destructured
            return FullFilePath(path, filename, Extension.values().find { it.name == extension}?:error("Invalid file extension"))
        }
    }
    override fun toString() = "$directory/$fileName.${extension.ext}"
}
