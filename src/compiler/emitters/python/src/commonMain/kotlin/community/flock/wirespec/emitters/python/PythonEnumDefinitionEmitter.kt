package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.EnumDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.Spacer.invoke
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Module

interface PythonEnumDefinitionEmitter: EnumDefinitionEmitter, PythonIdentifierEmitter {

    override fun emit(enum: Enum, module: Module) = """
        |class ${enum.identifier.sanitize()}(str, Enum):
        |${enum.entries.joinToString("\n") { "${Spacer}${it.sanitizeEnum().sanitizeKeywords()} = \"$it\"" }}
        |
        |${Spacer}@property
        |${Spacer}def label(self) -> str:
        |${Spacer(2)}return self.value
        |
        |${Spacer}def __str__(self) -> str:
        |${Spacer(2)}return self.value
    """.trimMargin()

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
}
