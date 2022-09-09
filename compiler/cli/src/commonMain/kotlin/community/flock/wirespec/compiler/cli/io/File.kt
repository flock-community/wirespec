package community.flock.wirespec.compiler.cli.io

expect abstract class File(path: Path) {

    val path: Path

    fun read(): String

    fun write(text: String)

}
