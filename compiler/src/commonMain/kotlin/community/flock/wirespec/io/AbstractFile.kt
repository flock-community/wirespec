package community.flock.wirespec.io

expect abstract class AbstractFile(path: String) {

    fun read(): String

    fun write(text: String)

}
