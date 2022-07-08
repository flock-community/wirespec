package community.flock.wirespec.compiler.cli.io

import java.io.File
import kotlin.text.Charsets.UTF_8

actual abstract class AbstractFile actual constructor(private val path: String) {

    actual fun read(): String = File(path).readText(UTF_8)

    actual fun write(text: String) = path.split(".")
        .joinToString("-from-jvm.")
        .let(::File)
        .writeText(text, UTF_8)

}
