package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.plugin.FileExtension.Wirespec
import kotlin.jvm.JvmInline

interface Reader {
    fun read(): String
}

sealed interface Input

data class FullDirPath(val path: String) : Input

data class FullFilePath(val directory: String, val fileName: FileName, val extension: FileExtension = Wirespec) : Input {
    companion object {
        fun parse(input: String): FullFilePath {
            val list = input.split("/").let { it.dropLast(1) + it.last().split(".") }
            val extension = list.last().lowercase()
                .let { ext -> FileExtension.entries.find { it.value == ext } }
                ?: error("Invalid file extension")
            val filename = FileName(list[list.size - 2])
            val path = list.subList(0, list.size - 2).joinToString("/")
            return FullFilePath(path, filename, extension)
        }
    }

    override fun toString() = "$directory/${fileName.value}.${extension.value}"
}

@JvmInline
value class FileName(override val value: String) : Value<String>

data object Console : Input, Reader {
    override fun read() = generateSequence { readlnOrNull() }.joinToString("/n")
}
