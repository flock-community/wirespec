package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Module

interface KotlinEnumDefinitionEmitter: KotlinIdentifierEmitter, EnumDefinitionEmitter, SpaceEmitter {

    override fun emit(enum: Enum, module: Module) = """
        |enum class ${enum.identifier.sanitize()} (override val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |}
        |
    """.trimMargin()

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()


}
