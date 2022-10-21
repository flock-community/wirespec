package community.flock.wirespec.compiler.cli.io

expect abstract class File(path: DirPath) {

    val path: DirPath

    fun read(): String

    fun write(text: String)

}
