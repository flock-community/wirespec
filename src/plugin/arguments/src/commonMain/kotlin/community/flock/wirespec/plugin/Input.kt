package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.plugin.FileExtension.Wirespec
import kotlin.jvm.JvmInline

fun interface Reader {
    fun read(): String
}

sealed interface Input

@JvmInline
value class DirectoryPath(override val value: String) :
    Input,
    Value<String> {
    override fun toString() = value
    companion object {
        fun String.toDirectoryPath() = DirectoryPath(this)
    }
}

data class FilePath(val directory: DirectoryPath, val fileName: FileName, val extension: FileExtension = Wirespec) : Input {
    companion object {
        fun parse(input: String): FilePath {
            val list = input.split("/").let { it.dropLast(1) + it.last().split(".") }
            val extension = list.last().lowercase()
                .let { ext -> FileExtension.entries.find { it.value == ext } }
                ?: error("Invalid file extension")
            val idxOfFileName = list.size - 2
            val filename = FileName(list[idxOfFileName])
            val path = list.subList(0, idxOfFileName).joinToString("/")
            return FilePath(DirectoryPath(path), filename, extension)
        }
    }

    override fun toString() = "$directory/${fileName.value}.${extension.value}"
}

@JvmInline
value class FileName(override val value: String) : Value<String> {
    override fun toString() = value
}

data object Console : Input, Reader {
    override fun read() = generateSequence { readlnOrNull() }.joinToString("/n")
}
