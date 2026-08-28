package community.flock.wirespec.plugin.io

import community.flock.wirespec.compiler.core.Value
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.FileExtension.entries
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.plugin.io.Source.Type
import kotlin.jvm.JvmInline

internal sealed interface Input

internal sealed interface Output

public data class Source<out E : Type>(val name: Name, val content: String) : Input {
    public sealed interface Type {
        public data object Wirespec : Type
        public data object JSON : Type
    }

    public fun map(fn: (String) -> String): Source<E> = Source<E>(name = name, content = fn(content))
}

private data class Sink(val name: String, val content: String) : Output

public class Directory(public val path: DirectoryPath) :
    Input,
    Output

private operator fun Directory.plus(packageName: PackageName) = Directory(path + packageName)

public sealed interface FullPath

internal fun FullPath.path(): String = when (this) {
    is ClassPath -> value
    is DirectoryPath -> value
    is FilePath -> directory.value
}

@JvmInline
public value class ClassPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString(): String = value
}

@JvmInline
public value class DirectoryPath(override val value: String) :
    FullPath,
    Value<String> {
    override fun toString(): String = value
    public fun resolve(path: String): DirectoryPath = DirectoryPath("$value/$path")
}

private operator fun DirectoryPath.plus(packageName: PackageName) = when (packageName.createDirectory) {
    true -> "/${packageName.value.split('.').joinToString("/")}"
    false -> ""
}.let { this + it }

public data class FilePath(val directory: DirectoryPath, val name: Name, val extension: FileExtension) : FullPath {
    public companion object {
        public operator fun invoke(input: String): FilePath {
            val list = input.split("/").let { it.dropLast(1) + it.last().split(".") }
            val ext = list.last().lowercase()
            val extension = entries.find { it.value == ext } ?: error("Invalid file extension: $ext")
            val idxOfFileName = list.size - 2
            val filename = Name(list[idxOfFileName])
            val path = list.subList(0, idxOfFileName).joinToString("/")
            return FilePath(DirectoryPath(path), filename, extension)
        }
    }

    override fun toString(): String = "$directory/${name.value}.${extension.value}"
}

@JvmInline
public value class Name(override val value: String) : Value<String> {
    override fun toString(): String = value
}

private operator fun FilePath.plus(string: String) = directory + string

private operator fun DirectoryPath.plus(string: String) = DirectoryPath(value + string)
