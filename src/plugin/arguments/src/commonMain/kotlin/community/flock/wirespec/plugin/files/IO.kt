package community.flock.wirespec.plugin.files

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.FileExtension.JSON
import community.flock.wirespec.plugin.FileExtension.Java
import community.flock.wirespec.plugin.FileExtension.Kotlin
import community.flock.wirespec.plugin.FileExtension.Scala
import community.flock.wirespec.plugin.FileExtension.TypeScript
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.FileExtension.entries
import kotlin.jvm.JvmInline

fun interface Reader {
    fun read(): String
}

fun interface Copy {
    fun copy(fileName: FileName): File
}

sealed interface Input

sealed interface Output

class Directory(val path: DirectoryPath) :
    Input,
    Output

operator fun Directory.plus(packageName: PackageName) = when (packageName.createDirectory) {
    true -> "/${packageName.value.split('.').joinToString("/")}"
    false -> ""
}.let { Directory(path + it) }

abstract class File(val path: FilePath) :
    Input,
    Copy

fun File.withExtension(extension: FileExtension) = when (extension) {
    Java -> JavaFile(path.copy(extension = extension))
    Kotlin -> KotlinFile(path.copy(extension = extension))
    Scala -> ScalaFile(path.copy(extension = extension))
    TypeScript -> TypeScriptFile(path.copy(extension = extension))
    Wirespec -> WirespecFile(path.copy(extension = extension))
    JSON -> JSONFile(path.copy(extension = extension))
}

fun File.changeName(name: FileName) = path.copy(fileName = name)
fun File.changeDirectory(directory: Directory) = path.copy(directory = directory.path)

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
