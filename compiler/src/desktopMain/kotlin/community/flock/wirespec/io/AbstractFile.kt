package community.flock.wirespec.io

import community.flock.wirespec.WireSpecException.IOException.FileReadException
import community.flock.wirespec.WireSpecException.IOException.FileWriteException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.EOF
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs

actual abstract class AbstractFile actual constructor(private val path: String) {

    companion object {
        private const val bufferLength = 64 * 1024
    }

    actual fun read() = StringBuilder().apply {
        fopen(path, "r")?.let { file ->
            try {
                memScoped {
                    val buffer = allocArray<ByteVar>(bufferLength)
                    var line = fgets(buffer, bufferLength, file)?.toKString()
                    while (line != null) {
                        append(line)
                        line = fgets(buffer, bufferLength, file)?.toKString()
                    }
                }
            } finally {
                fclose(file)
            }
        } ?: throw FileReadException("Cannot open input file $path")
    }.toString()

    actual fun write(text: String) {
        val file = fopen(path.split(".").joinToString("-from-kt."), "w")
            ?: throw FileReadException("Cannot open output file $path")
        try {
            memScoped {
                if (fputs(text, file) == EOF) throw FileWriteException("File write error")
            }
        } finally {
            fclose(file)
        }
    }
}
