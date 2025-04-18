package community.flock.wirespec.plugin.io

import arrow.core.Either
import arrow.core.NonEmptySet
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.toNonEmptySetOrNull
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.plugin.io.Source.Type.Wirespec
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

fun <B> Either<IOError, B>.or(errorFn: (String) -> Nothing) = getOrElse { errorFn(it.message) }

fun getFullPath(input: String?, createIfNotExists: Boolean = false): Either<IOError, FullPath?> = either {
    when {
        input == null -> null
        input.startsWith("classpath:") -> SourcePath(input.substringAfter("classpath:"))
        else -> {
            val path = Path(input).createIfNotExists(createIfNotExists)
            val meta = SystemFileSystem.metadataOrNull(path) ?: raise(CannotAccessFileOrDirectory(input))
            val pathString = path.toString()
            when {
                meta.isDirectory -> DirectoryPath(pathString)
                meta.isRegularFile -> FilePath(pathString)
                else -> raise(IsNotAFileOrDirectory(pathString))
            }
        }
    }
}

fun getOutPutPath(inputPath: FullPath, output: String?): Either<IOError, DirectoryPath> = either {
    when (val it = getFullPath(output, true).bind()) {
        null -> DirectoryPath("${inputPath.path()}/out")
        is DirectoryPath -> it
        is FilePath, is SourcePath -> raise(OutputShouldBeADirectory())
    }
}

fun Path.createIfNotExists(create: Boolean = true) = also {
    when {
        create && !SystemFileSystem.exists(this) -> SystemFileSystem.createDirectories(this, true)
        else -> Unit
    }
}

fun FilePath.read(): String = Path(toString())
    .let { SystemFileSystem.source(it).buffered().readString() }

fun FilePath.write(string: String) = Path(toString())
    .also { it.parent?.createIfNotExists() }
    .let {
        SystemFileSystem.sink(it).buffered()
            .apply { writeString(string) }
            .flush()
    }

fun Directory.wirespecSources(): Either<WirespecFileError, NonEmptySet<Source<Wirespec>>> = either {
    Path(path.value)
        .let(SystemFileSystem::list)
        .filter(::isRegularFile)
        .filter(::isWirespecFile)
        .map { FilePath(it.toString()) to SystemFileSystem.source(it).buffered().readString() }
        .map { (path, source) -> Source<Wirespec>(name = path.name, content = source) }
        .toNonEmptySetOrNull()
        ?: raise(WirespecFileError())
}

private fun isRegularFile(path: Path) = SystemFileSystem.metadataOrNull(path)?.isRegularFile == true

private fun isWirespecFile(path: Path) = path.extension == FileExtension.Wirespec.value

private val Path.extension
    get() = name.substringAfterLast('.', "")
