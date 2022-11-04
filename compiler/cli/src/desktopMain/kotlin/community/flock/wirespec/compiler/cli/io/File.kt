package community.flock.wirespec.compiler.cli.io

import community.flock.wirespec.compiler.core.exceptions.WireSpecException.IOException.FileReadException
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.IOException.FileWriteException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.EOF
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs

actual abstract class File actual constructor(actual val path: DirPath) : Copy {

    actual fun read() = StringBuilder().apply {
        fopen(path.fullFilePath, "r")?.let { file ->
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
        } ?: throw FileReadException("Cannot open input file ${path.fullFilePath}")
    }.toString()

    actual fun write(text: String) {
        val file = fopen(path.fullFilePath.split(".").joinToString("-from-native."), "w")
            ?: throw FileReadException("Cannot open output file ${path.fullFilePath}")
        try {
            memScoped {
                if (fputs(text, file) == EOF) throw FileWriteException("File write error")
            }
        } finally {
            fclose(file)
        }
    }

    companion object {
        private const val bufferLength = 64 * 1024
    }
}
