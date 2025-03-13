package community.flock.wirespec.plugin.maven

import arrow.core.NonEmptySet
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.plugin.FileExtension.Wirespec
import community.flock.wirespec.plugin.files.Directory
import community.flock.wirespec.plugin.files.File
import community.flock.wirespec.plugin.files.FileName
import community.flock.wirespec.plugin.files.FilePath
import community.flock.wirespec.plugin.files.WirespecFile
import java.io.File as JavaFile

fun JavaFile.createIfNotExists(create: Boolean = true) = also {
    when {
        create && !it.exists() -> it.mkdirs()
        else -> Unit
    }
}

fun File.read() = path.toString()
    .also(::println)
    .let(::JavaFile)
    .also(::println)
    .readText(Charsets.UTF_8)
    .also(::println)

fun File.write(string: String) = path.toString()
    .let(::JavaFile)
    .also(::println)
    .apply { parentFile.mkdirs() }
    .writeText(string)

fun Directory.wirespecFiles(): NonEmptySet<WirespecFile> = path.value
    .let(::JavaFile)
    .listFiles()
    .orEmpty()
    .asSequence()
    .filter { it.isFile }
    .filter { it.extension == Wirespec.value }
    .map { it.name.split(".").first() }
    .map { FilePath(path, FileName(it)) }
    .map(::WirespecFile)
    .toList()
    .onEach(::println)
    .toNonEmptySetOrNull()
    ?: throw WirespecFileError()
