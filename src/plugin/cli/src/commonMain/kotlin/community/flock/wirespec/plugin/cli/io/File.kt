package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
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

abstract class File(val path: FullFilePath) :
    Reader,
    Writer,
    Copy {

    override fun read(): String = Path(path.toString())
        .let { SystemFileSystem.source(it).buffered().readString() }

    override fun write(string: String) = Path(path.toString())
        .let { SystemFileSystem.sink(it).buffered().writeString(string) }
}
