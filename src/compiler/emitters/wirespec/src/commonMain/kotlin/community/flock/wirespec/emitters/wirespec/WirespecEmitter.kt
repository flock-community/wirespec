package community.flock.wirespec.emitters.wirespec

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.LanguageEmitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.StringFormat
import community.flock.wirespec.compiler.utils.Logger

private interface WirespecEmitters:
    WirespecIdentifierEmitter,
    WirespecTypeDefinitionEmitter,
    WirespecEndpointDefinitionEmitter,
    WirespecChannelDefinitionEmitter,
    WirespecRpcDefinitionEmitter,
    WirespecEnumDefinitionEmitter,
    WirespecUnionDefinitionEmitter,
    WirespecRefinedTypeDefinitionEmitter,
    WirespecPartDefinitionEmitter

public open class WirespecEmitter : LanguageEmitter(), WirespecEmitters {

    override val extension: FileExtension = FileExtension.Wirespec

    override val supportsDefaults: Boolean = true

    override val shared: Shared? = null

    override val singleLineComment: String = "\n"

    override fun notYetImplemented(): String = singleLineComment

    /**
     * The modules are written together in one file, `wirespec.ws`. The `@file:` annotations of a module describe the
     * definitions of that module only, so among several modules, each one that has them is a file of its own, named
     * after the module. The first file holds the `@spec:` annotations.
     */
    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules.toList()
        .let { modules ->
            when (modules.size) {
                1 -> listOf(DEFAULT_FILE to modules)
                else -> modules.partition { it.annotations.isEmpty() }
                    .let { (together, apart) -> listOfNotNull(together.takeIf { it.isNotEmpty() }?.let { DEFAULT_FILE to it }) + apart.map { it.fileName() to listOf(it) } }
            }
        }
        .mapIndexed { index, (file, modules) ->
            Emitted(
                "$file.${extension.value}",
                emitHeader(ast.annotations.takeIf { index == 0 }.orEmpty().map { it.annotation }, modules.flatMap { it.annotations }) +
                    modules.flatMap { emit(it, logger) }.joinToString("\n") { it.result },
            )
        }
        .toNonEmptyListOrNull()!!

    private fun emitHeader(spec: List<Annotation>, file: List<Annotation>): String =
        (spec.map { it.emit("spec") } + file.map { it.emit("file") })
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n", postfix = "\n\n")
            .orEmpty()

    private fun Module.fileName(): String = fileUri.value.substringAfterLast('/').substringBeforeLast('.')

    private companion object {
        const val DEFAULT_FILE = "wirespec"
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted =
        super<LanguageEmitter>.emit(definition, module, logger).let { emitted -> emitted.copy(result = definition.emitMetaData() + emitted.result) }

    private fun Definition.emitMetaData(): String =
        ((this as? HasAnnotations)?.annotations.orEmpty().map { it.emit() } + listOfNotNull(comment?.emit())).joinToString("") { "$it\n" }

    override fun Reference.Primitive.Type.Constraint.emit(): String = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> "(${StringFormat.of(this)?.label ?: value})"
        is Reference.Primitive.Type.Constraint.Bound -> "(${min ?: "_"}, ${max ?: "_"})"
    }
}
