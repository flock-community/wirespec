package community.flock.wirespec.compiler.cli.io

import kotlin.text.Charsets.UTF_8

actual abstract class File actual constructor(actual val path: Path) {

    actual fun read(): String = java.io.File(path.fullFilePath).readText(UTF_8)

    actual fun write(text: String) = path.fullFilePath.split(".")
        .joinToString("-from-jvm.")
        .let { java.io.File(it) }
        .writeText(text, UTF_8)

}
