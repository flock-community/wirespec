package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier

interface PythonIdentifierEmitter: IdentifierEmitter {

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.sanitize()
        is FieldIdentifier -> identifier.sanitize().sanitizeKeywords()
    }

     fun Identifier.sanitize() = value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "False", "None", "True", "and", "as", "assert",
            "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for",
            "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or",
            "pass", "raise", "return", "try", "while",
            "with", "yield"
        )
    }
}
