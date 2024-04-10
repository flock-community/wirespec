package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

abstract class Emitter(
    val logger: Logger,
    val split: Boolean = false
) {

    abstract val shared: String?

    abstract fun emit(ast: AST): List<Emitted>

    fun Endpoint.Content.emitContentType() = type
        .substringBefore(";")
        .split("/", "-")
        .joinToString("") { it.firstToUpper() }
        .replace("+", "")

    companion object {
        const val SPACER = "  "
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase)
        fun String.firstToLower() = replaceFirstChar(Char::lowercase)
        fun AST.needImports() = any { it is Endpoint || it is Enum || it is Refined }
        fun AST.hasEndpoints() = any { it is Endpoint }
        fun String.isInt() = toIntOrNull() != null
        fun String.isStatusCode() = toIntOrNull()?.let { it in 0..599 } ?: false
    }
}
