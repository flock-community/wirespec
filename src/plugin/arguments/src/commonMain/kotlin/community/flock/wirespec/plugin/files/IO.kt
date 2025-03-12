package community.flock.wirespec.plugin.files

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.FileExtension.entries
import kotlin.jvm.JvmInline

sealed interface Input

sealed interface Output

class Directory(val path: DirectoryPath) :
    Input,
    Output

operator fun Directory.plus(packageName: PackageName) = when (packageName.createDirectory) {
    true -> "/${packageName.value.split('.').joinToString("/")}"
    false -> ""
}.let { Directory(path + it) }

abstract class File(val path: FilePath) : Input {
    abstract fun changeName(fileName: FileName): File
}

fun Directory.inferOutputFile(file: File) = file.path.copy(directory = path)

sealed interface FullPath

@JvmInline
value class DirectoryPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString() = value
}

data class FilePath(val directory: DirectoryPath, val fileName: FileName, val extension: FileExtension = Wirespec) : FullPath {
    companion object {
        operator fun invoke(input: String): FilePath {
            val list = input.split("/").let { it.dropLast(1) + it.last().split(".") }
            val extension = list.last().lowercase()
                .let { ext -> entries.find { it.value == ext } }
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
