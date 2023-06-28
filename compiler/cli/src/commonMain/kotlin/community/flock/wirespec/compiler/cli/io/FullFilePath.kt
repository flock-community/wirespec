package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.io.Extension.Wirespec

data class FullFilePath(val directory: String, val fileName: String, val extension: Extension = Wirespec) {
    companion object {
        fun fromString(input: String): FullFilePath {
            val list = input.split("/", ".")
            val extension = list.last().lowercase()
                .let { ext -> Extension.values().find { it.ext == ext } }
                ?: error("Invalid file extension")
            val filename = list[list.size - 2]
            val path = list.subList(0, list.size - 2).joinToString("/")
            return FullFilePath(path, filename, extension)
        }
    }

    override fun toString() = "$directory/$fileName.${extension.ext}"
}
