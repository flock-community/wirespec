package community.flock.wirespec.compiler.cli.io

expect abstract class AbstractFile(path: String) {

    fun read(): String

    fun write(text: String)

}
