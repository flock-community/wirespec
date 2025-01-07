package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.compiler.core.exceptions.WirespecException.IOException.FileReadException
import community.flock.wirespec.compiler.core.exceptions.WirespecException.IOException.FileWriteException
import community.flock.wirespec.plugin.FullFilePath
import community.flock.wirespec.plugin.Reader
import community.flock.wirespec.plugin.Writer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.EOF
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.mkdir
import platform.posix.opendir

@OptIn(ExperimentalForeignApi::class)
actual abstract class File actual constructor(actual val path: FullFilePath) : Reader, Writer, Copy {

    actual override fun read() = StringBuilder().apply {
        fopen(path.toString(), "r")?.let { file ->
            try {
                memScoped {
                    val buffer = allocArray<ByteVar>(BUFFER_LENGTH)
                    var line = fgets(buffer, BUFFER_LENGTH, file)?.toKString()
                    while (line != null) {
                        append(line)
                        line = fgets(buffer, BUFFER_LENGTH, file)?.toKString()
                    }
                }
            } finally {
                fclose(file)
            }
        } ?: throw FileReadException("Cannot open input file $path")
    }.toString()

    actual override fun write(string: String) {
        val directory = path.directory
            .apply {
                split("/").reduce { acc, cur -> "$acc/$cur".also { opendir(it) ?: makeDir(it) } }
            }

        val nativePath = path.copy(directory = directory)

        fopen(nativePath.toString(), "w")?.runCatching {
            let { memScoped { if (fputs(string, it) == EOF) throw FileWriteException("File write error") } }
            fclose(this)
        } ?: throw FileReadException("Cannot open output file $nativePath")
    }

    @OptIn(kotlinx.cinterop.UnsafeNumber::class)
    private fun makeDir(dir: String) = mkdir(dir, 493u)

    companion object {
        private const val BUFFER_LENGTH = 64 * 1024
    }
}
