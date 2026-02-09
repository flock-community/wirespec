package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.spacer
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module

interface KotlinEnumDefinitionEmitter : EnumDefinitionEmitter, KotlinIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) = """
        |enum class ${enum.identifier.sanitize()} (override val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |}
        |
    """.trimMargin()

    private fun String.sanitizeNegative() = if (startsWith("-")) "__${substring(1)}" else this

    private fun String.sanitizeEnum() = sanitizeNegative()
        .split("-", ", ", ".", " ", "//")
        .joinToString("_")
        .sanitizeFirstIsDigit()


}
