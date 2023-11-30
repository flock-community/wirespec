package community.flock.wirespec.compiler.cli.io

import kotlin.text.Charsets.UTF_8
import java.io.File as JavaFile

actual abstract class File actual constructor(actual val path: FullFilePath) : Copy {

    actual fun read(): String = JavaFile(path.toString()).readText(UTF_8)

    actual fun write(text: String) = path.run {
        JavaFile(directory).also { if (!it.exists()) it.mkdirs() }
        JavaFile(this.toString()).writeText(text, UTF_8)
    }

}
