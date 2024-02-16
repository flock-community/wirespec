package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Reader
import kotlin.text.Charsets.UTF_8
import java.io.File as JavaFile

actual abstract class File actual constructor(actual val path: FullFilePath) : Reader, Copy {

    actual override fun read(): String = JavaFile(path.toString()).readText(UTF_8)

    actual fun write(text: String) = path.run {
        JavaFile(directory).also { if (!it.exists()) it.mkdirs() }
        JavaFile(this.toString()).writeText(text, UTF_8)
    }

}
