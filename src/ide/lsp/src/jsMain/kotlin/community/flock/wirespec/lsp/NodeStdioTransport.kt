package community.flock.wirespec.lsp

class NodeStdioTransport : Transport {

    private val buffer = StringBuilder()
    private var contentLength: Int = -1

    override fun start(onMessage: (String) -> Unit) {
        process.stdin.setEncoding("utf8")
        process.stdin.on("data") { chunk ->
            buffer.append(chunk as String)
            drainFrames(onMessage)
        }
    }

    override fun send(json: String) {
        val byteLength = Buffer.byteLength(json, "utf8")
        process.stdout.write("Content-Length: $byteLength\r\n\r\n$json")
    }

    private tailrec fun drainFrames(onMessage: (String) -> Unit) {
        if (contentLength < 0) {
            val headerEnd = indexOf(buffer, "\r\n\r\n")
            if (headerEnd < 0) return
            val header = buffer.substring(0, headerEnd)
            contentLength = parseContentLength(header) ?: -2
            buffer.deleteRange(0, headerEnd + 4)
            if (contentLength == -2) {
                // Malformed header; reset and try again next chunk.
                contentLength = -1
                return
            }
        }
        if (buffer.length < contentLength) return
        val body = buffer.substring(0, contentLength)
        buffer.deleteRange(0, contentLength)
        contentLength = -1
        onMessage(body)
        drainFrames(onMessage)
    }
}

private fun indexOf(buffer: StringBuilder, needle: String): Int {
    val haystackLen = buffer.length
    val needleLen = needle.length
    if (needleLen == 0 || needleLen > haystackLen) return -1
    outer@ for (i in 0..(haystackLen - needleLen)) {
        for (j in 0 until needleLen) {
            if (buffer[i + j] != needle[j]) continue@outer
        }
        return i
    }
    return -1
}

private val CONTENT_LENGTH_REGEX = Regex("Content-Length: (\\d+)", RegexOption.IGNORE_CASE)

private fun parseContentLength(header: String): Int? = CONTENT_LENGTH_REGEX.find(header)?.groupValues?.get(1)?.toIntOrNull()
