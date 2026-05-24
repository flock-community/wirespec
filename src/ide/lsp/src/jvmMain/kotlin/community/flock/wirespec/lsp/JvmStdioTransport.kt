package community.flock.wirespec.lsp

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintStream

class JvmStdioTransport(
    private val input: InputStream = System.`in`,
    private val output: OutputStream = System.out,
) : Transport {

    private val out = PrintStream(output, true, Charsets.UTF_8)

    override fun start(onMessage: (String) -> Unit) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        Thread({
            try {
                while (true) {
                    val message = readFrame(reader) ?: break
                    onMessage(message)
                }
            } catch (_: InterruptedException) {
                // server shutting down
            }
        }, "wirespec-lsp-reader").also {
            it.isDaemon = false
            it.start()
        }
    }

    override fun send(json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        synchronized(out) {
            out.print("Content-Length: ${bytes.size}\r\n\r\n")
            out.write(bytes)
            out.flush()
        }
    }

    private fun readFrame(reader: BufferedReader): String? {
        var contentLength = -1
        while (true) {
            val line = reader.readLine() ?: return null
            if (line.isEmpty()) break
            val match = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE).matchEntire(line.trim())
            if (match != null) {
                contentLength = match.groupValues[1].toInt()
            }
        }
        if (contentLength < 0) return null
        val buf = CharArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = reader.read(buf, read, contentLength - read)
            if (n < 0) return null
            read += n
        }
        return String(buf)
    }
}
