package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.emitters.kotlin.KotlinEmitter.Companion.reservedKeywords

interface KotlinIdentifierEmitter: IdentifierEmitter {

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.sanitize()
        is FieldIdentifier -> identifier.sanitize().sanitizeKeywords()
    }

    override fun Identifier.sanitize() = this.value
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

}
