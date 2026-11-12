package community.flock.wirespec.lsp

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.lsp.protocol.Position

internal class Document(val uri: String, var version: Int, text: String) {
    var text: String = text
        private set
    private var lineStartsCache: IntArray? = computeLineStarts(text)

    /** Only a `file:` document has a directory, and with it sibling files to resolve against. */
    val directoryUri: String? get() = uri.takeIf { it.startsWith(FILE_SCHEME) }?.substringBeforeLast('/')

    val fileName: String get() = uri.substringAfterLast('/').decodeUriComponent()

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

internal class DocumentStore(private val sourceFiles: SourceFiles) {
    private val documents = mutableMapOf<String, Document>()

    fun open(uri: String, version: Int, text: String): Document = Document(uri, version, text).also { documents[uri] = it }

    fun update(uri: String, version: Int, text: String): Document? = documents[uri]?.also { it.update(version, text) }

    fun close(uri: String): Document? = documents.remove(uri)

    operator fun get(uri: String): Document? = documents[uri]

    fun all(): List<Document> = documents.values.toList()

    fun openIn(directoryUri: String?): List<Document> = directoryUri
        ?.let { directory -> documents.values.filter { it.directoryUri == directory } }
        .orEmpty()

    /**
     * The compiler takes every Wirespec file of a directory as one input, so a document is checked
     * together with its siblings. [document] comes first; an open sibling wins over its copy on
     * disk, because the editor may hold changes that are not saved yet.
     */
    fun moduleOf(document: Document): NonEmptyList<Document> = nonEmptyListOf(document) + siblingsOf(document)

    private fun siblingsOf(document: Document): List<Document> = document.directoryUri
        ?.let { directory ->
            openIn(directory).filterNot { it.uri == document.uri }.let { open ->
                (open + document).map { it.fileName }.toSet().let { openNames ->
                    open + sourceFiles.wirespecFilesIn(directory)
                        .filterKeys { it !in openNames }
                        .map { (name, text) -> Document("$directory/${name.encodeUriComponent()}", 0, text) }
                }
            }
        }
        .orEmpty()
        .sortedBy { it.uri }
}
