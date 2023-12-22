package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.cli.FullFilePath
import community.flock.wirespec.compiler.cli.Reader

interface Copy {
    fun copy(fileName: String): File
}

expect abstract class File(path: FullFilePath) : Reader, Copy {

    val path: FullFilePath

    override fun read(): String

    fun write(text: String)

}
