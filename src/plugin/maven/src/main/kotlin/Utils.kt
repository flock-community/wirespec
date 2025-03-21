package community.flock.wirespec.plugin.maven

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.common.FileExtension.Wirespec
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.Name
import community.flock.wirespec.plugin.files.Source
import community.flock.wirespec.plugin.files.Source.Type.Wirespec
import java.io.File

fun File.createIfNotExists(create: Boolean = true) = also {
    when {
        create && !it.exists() -> it.mkdirs()
        else -> Unit
    }
}

fun FilePath.read() = toString()
    .let(::File)
    .readText(Charsets.UTF_8)

fun FilePath.write(string: String) = toString()
    .let(::File)
    .apply { parentFile.mkdirs() }
    .writeText(string)

fun Directory.wirespecFiles(): NonEmptySet<Source<Wirespec>> = File(path.value)
    .listFiles()
    .orEmpty()
    .filter { it.isFile }
    .filter { it.extension == Wirespec.value }
    .map { it.name.split(".").first() to it.readText(Charsets.UTF_8) }
    .map { (name, source) -> Source<Wirespec>(Name(name), source) }
    .toNonEmptySetOrNull()
    ?: throw WirespecFileError()
