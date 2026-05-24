package community.flock.wirespec.lsp

import community.flock.wirespec.lsp.protocol.Position

class Document(val uri: String, var version: Int, text: String) {
    var text: String = text
        private set
    private var lineStartsCache: IntArray? = computeLineStarts(text)

    fun update(version: Int, newText: String) {
        this.version = version
        this.text = newText
        this.lineStartsCache = computeLineStarts(newText)
    }

    fun offsetAt(position: Position): Int {
        val lineStarts = lineStartsCache ?: computeLineStarts(text).also { lineStartsCache = it }
        if (position.line < 0) return 0
        if (position.line >= lineStarts.size) return text.length
        val lineStart = lineStarts[position.line]
        val nextLine = if (position.line + 1 < lineStarts.size) lineStarts[position.line + 1] else text.length
        val maxCol = nextLine - lineStart
        return lineStart + position.character.coerceIn(0, maxCol)
    }

    fun positionAt(offset: Int): Position {
        val lineStarts = lineStartsCache ?: computeLineStarts(text).also { lineStartsCache = it }
        val clamped = offset.coerceIn(0, text.length)
        // Binary search for the line containing the offset.
        var low = 0
        var high = lineStarts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val midStart = lineStarts[mid]
            when {
                midStart == clamped -> return Position(mid, 0)
                midStart < clamped -> low = mid + 1
                else -> high = mid - 1
            }
        }
        val line = (low - 1).coerceAtLeast(0)
        val character = clamped - lineStarts[line]
        return Position(line, character)
    }
}

private fun computeLineStarts(text: String): IntArray {
    val starts = mutableListOf(0)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\n') {
            starts.add(i + 1)
        } else if (c == '\r') {
            // Treat \r\n and lone \r as line terminators.
            if (i + 1 < text.length && text[i + 1] == '\n') i++
            starts.add(i + 1)
        }
        i++
    }
    return starts.toIntArray()
}

class DocumentStore {
    private val documents = mutableMapOf<String, Document>()

    fun open(uri: String, version: Int, text: String): Document = Document(uri, version, text).also { documents[uri] = it }

    fun update(uri: String, version: Int, text: String): Document? = documents[uri]?.also { it.update(version, text) }

    fun close(uri: String) {
        documents.remove(uri)
    }

    operator fun get(uri: String): Document? = documents[uri]
}
