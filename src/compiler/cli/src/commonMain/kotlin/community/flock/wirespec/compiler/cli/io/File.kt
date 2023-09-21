package community.flock.wirespec.compiler.cli.io


interface Copy {
    fun copy(fileName: String): File
}

expect abstract class File(path: FullFilePath) : Copy {

    val path: FullFilePath

    fun read(): String

    fun write(text: String)

}
