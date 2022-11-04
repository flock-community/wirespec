package community.flock.wirespec.compiler.cli.io

import kotlin.text.Charsets.UTF_8
import java.io.File as JavaFile

actual abstract class File actual constructor(actual val path: DirPath) : Copy {

    actual fun read(): String = JavaFile(path.fullFilePath).readText(UTF_8)

    actual fun write(text: String) = path
        .also { JavaFile(it.directory).run { if (!exists()) mkdirs() } }
        .fullFilePath.split(".")
        .joinToString("-from-jvm.")
        .let { JavaFile(it) }
        .writeText(text, UTF_8)

}
