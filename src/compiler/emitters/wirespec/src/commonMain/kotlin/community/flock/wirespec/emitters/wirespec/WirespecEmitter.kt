package community.flock.wirespec.emitters.wirespec

import arrow.core.NonEmptyList
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

    override val emitsRpcFields: Boolean = true

    override val shared: Shared? = null

    override val singleLineComment: String = "\n"

    override fun notYetImplemented(): String = singleLineComment

    /**
     * Every module is a file of its own, named after the file it comes from, so the `@file:` annotations of a module
     * keep describing that module only. A `@spec:` annotation stays in the file it is written in. One that is written
     * in a file without definitions, which is no module, goes to the first file.
     */
    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules.map { it.fileUri }.let { files ->
        ast.modules.mapIndexed { index, module ->
            Emitted(
                "${module.fileName()}.${extension.value}",
                emitHeader(ast.annotations.filter { it.fileUri == module.fileUri || (index == 0 && it.fileUri !in files) }.map { it.annotation }, module.annotations) +
                    emit(module, logger).joinToString("\n") { it.result },
            )
        }
    }

    private fun emitHeader(spec: List<Annotation>, file: List<Annotation>): String =
        (spec.map { it.emit("spec") } + file.map { it.emit("file") })
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n", postfix = "\n\n")
            .orEmpty()

    private fun Module.fileName(): String = fileUri.value.substringAfterLast('/').substringBeforeLast('.').ifBlank { DEFAULT_FILE }

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
