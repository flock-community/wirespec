package community.flock.wirespec.lsp

import community.flock.wirespec.compiler.core.emit.FileExtension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

internal const val FILE_SCHEME = "file://"

internal fun interface SourceFiles {
    /** The Wirespec files directly in the directory at [directoryUri], as file name to content. */
    fun wirespecFilesIn(directoryUri: String): Map<String, String>
}

internal object SystemSourceFiles : SourceFiles {
    override fun wirespecFilesIn(directoryUri: String): Map<String, String> = runCatching {
        SystemFileSystem.list(Path(directoryUri.fileUriToPath()))
            .filter { it.name.substringAfterLast('.', "") == FileExtension.Wirespec.value }
            .filter { SystemFileSystem.metadataOrNull(it)?.isRegularFile == true }
            .mapNotNull { path -> runCatching { path.name to SystemFileSystem.source(path).buffered().use { it.readString() } }.getOrNull() }
            .toMap()
    }.getOrDefault(emptyMap())
}

private val WINDOWS_DRIVE = Regex("^/[A-Za-z]:")

internal fun String.fileUriToPath(): String = removePrefix(FILE_SCHEME)
    .let { "/" + it.substringAfter('/', "") }
    .decodeUriComponent()
    .let { if (WINDOWS_DRIVE.containsMatchIn(it)) it.drop(1) else it }

private val PERCENT_ENCODED = Regex("(?:%[0-9A-Fa-f]{2})+")

internal fun String.decodeUriComponent(): String = PERCENT_ENCODED.replace(this) { match ->
    match.value.chunked(3).map { it.drop(1).toInt(16).toByte() }.toByteArray().decodeToString()
}

private const val UNRESERVED = "-._~"

internal fun String.encodeUriComponent(): String = encodeToByteArray().joinToString("") { byte ->
    byte.toInt().toChar()
        .takeIf { byte >= 0 && (it.isLetterOrDigit() || it in UNRESERVED) }
        ?.toString()
        ?: "%${(byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')}"
}
