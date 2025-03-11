package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.plugin.FileExtension.*
import community.flock.wirespec.plugin.files.JavaFile
import community.flock.wirespec.plugin.files.JsonFile
import community.flock.wirespec.plugin.files.KotlinFile
import community.flock.wirespec.plugin.files.ScalaFile
import community.flock.wirespec.plugin.files.TypeScriptFile
import community.flock.wirespec.plugin.files.WirespecFile
import kotlin.jvm.JvmInline

fun interface Reader {
    fun read(): String
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

fun File.withExtension(extension: FileExtension) = when (extension) {
    Java -> JavaFile(path.copy(extension = extension))
    Kotlin -> KotlinFile(path.copy(extension = extension))
    Scala -> ScalaFile(path.copy(extension = extension))
    TypeScript -> TypeScriptFile(path.copy(extension = extension))
    Wirespec -> WirespecFile(path.copy(extension = extension))
    JSON -> JsonFile(path.copy(extension = extension))
}

sealed interface FullPath

@JvmInline
value class DirectoryPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString() = value
}

data class FilePath(val directory: DirectoryPath, val fileName: FileName, val extension: FileExtension = Wirespec) :
    FullPath {
    companion object {
        operator fun invoke(input: String): FilePath {
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
