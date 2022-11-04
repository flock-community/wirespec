package community.flock.wirespec.compiler.cli.io


interface Copy {
    fun copy(fileName: String): File
}

expect abstract class File(path: DirPath) : Copy {

    val path: DirPath

    fun read(): String

    fun write(text: String)

}
