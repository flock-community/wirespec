package community.flock.wirespec.plugin.files

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Wirespec
import community.flock.wirespec.compiler.core.emit.common.FileExtension.entries
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.plugin.files.Source.Type
import kotlin.jvm.JvmInline

sealed interface Input

sealed interface Output

data class Source<out E : Type>(val name: Name, val content: String) : Input {
    sealed interface Type {
        data object Wirespec : Type
        data object JSON : Type
    }
}

class Directory(val path: DirectoryPath) :
    Input,
    Output

fun Directory.toFilePath(name: Name) = FilePath(path, name, Wirespec)

operator fun Directory.plus(packageName: PackageName) = Directory(path + packageName)

sealed interface FullPath

fun FullPath.path() = when (this) {
    is SourcePath -> value
    is DirectoryPath -> value
    is FilePath -> directory.value
}

@JvmInline
value class SourcePath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString() = value
}

@JvmInline
value class DirectoryPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString() = value
}

operator fun DirectoryPath.plus(packageName: PackageName) = when (packageName.createDirectory) {
    true -> "/${packageName.value.split('.').joinToString("/")}"
    false -> ""
}.let { this + it }

data class FilePath(val directory: DirectoryPath, val name: Name, val extension: FileExtension) : FullPath {
    companion object {
        operator fun invoke(input: String): FilePath {
            val list = input.split("/").let { it.dropLast(1) + it.last().split(".") }
            val extension = list.last().lowercase()
                .let { ext -> entries.find { it.value == ext } }
                ?: error("Invalid file extension")
            val idxOfFileName = list.size - 2
            val filename = Name(list[idxOfFileName])
            val path = list.subList(0, idxOfFileName).joinToString("/")
            return FilePath(DirectoryPath(path), filename, extension)
        }
    }

    override fun toString() = "$directory/${name.value}.${extension.value}"
}

@JvmInline
value class Name(override val value: String) : Value<String> {
    override fun toString() = value
}

operator fun FilePath.plus(string: String) = directory + string

operator fun DirectoryPath.plus(string: String) = DirectoryPath(value + string)
