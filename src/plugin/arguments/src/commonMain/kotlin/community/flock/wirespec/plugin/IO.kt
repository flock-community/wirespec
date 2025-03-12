package community.flock.wirespec.plugin

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.plugin.FileExtension.Wirespec
import kotlin.jvm.JvmInline

fun interface Reader {
    fun read(): NonEmptyList<String>
}

fun interface Writer {
    fun write(string: String)
}

fun interface Copy {
    fun copy(fileName: FileName): File
}

sealed interface Input

sealed interface Output

class Directory(val path: DirectoryPath) :
    Input,
    Output

abstract class File(val path: FilePath) :
    Input,
    Copy

sealed interface FullPath

@JvmInline
value class DirectoryPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString() = value
}

data class FilePath(val directory: DirectoryPath, val fileName: FileName, val extension: FileExtension = Wirespec) : FullPath {
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

operator fun FilePath.plus(string: String) = directory + string

operator fun DirectoryPath.plus(string: String) = DirectoryPath(value + string)
