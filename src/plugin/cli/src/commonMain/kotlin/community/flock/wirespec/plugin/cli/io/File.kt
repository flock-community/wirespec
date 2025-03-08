package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FilePath
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.Writer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

interface Copy {
    fun copy(fileName: FileName): File
}

abstract class File(val path: FilePath) :
    Reader,
    Writer,
    Copy {

    override fun read(): String = readFromFile()

    override fun write(string: String) = Path(path.toString())
        .let { path ->
            path.parent
                ?.takeIf { !SystemFileSystem.exists(it) }
                ?.let { SystemFileSystem.createDirectories(it, true) }
            SystemFileSystem.sink(path).buffered()
                .apply { writeString(string) }
                .flush()
        }
}

fun File.readFromFile() = Path(path.toString())
    .let { SystemFileSystem.source(it).buffered().readString() }
