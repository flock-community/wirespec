package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Reader

interface Copy {
    fun copy(fileName: FileName): File
}

expect abstract class File(path: FullFilePath) : Reader, Copy {

    val path: FullFilePath

    override fun read(): String

    fun write(text: String)

}
