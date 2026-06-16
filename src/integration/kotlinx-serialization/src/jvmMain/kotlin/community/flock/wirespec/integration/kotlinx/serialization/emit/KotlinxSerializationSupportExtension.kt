package community.flock.wirespec.integration.kotlinx.serialization.emit

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.ir.core.Enum
import community.flock.wirespec.ir.core.HasName
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumDefinition
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionDefinition
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds kotlinx.serialization annotations to every generated model declaration,
 * mirroring the legacy `SerializableKotlinEmitter`:
 *
 * - records and refined types get `@Serializable` + `@SerialName("<wirespec name>")`
 * - unions and enums get `@Serializable`
 *
 * The annotations are emitted fully qualified, so generated code needs only the
 * kotlinx-serialization runtime on its classpath — no imports are added and the
 * IR stays language-neutral.
 *
 * Only the top-level model declarations of each [LanguageFile] are annotated;
 * endpoint-internal structs live nested inside their endpoint `Namespace` and are
 * left untouched, just as the legacy emitter only overrode `emit(type)`,
 * `emit(refined)` and `emit(union)`. Register on a Kotlin
 * [community.flock.wirespec.ir.emit.IrEmitter] built with `EmitShared(false)`, so
 * the runtime classes come from the wirespec-jvm dependency rather than being
 * re-emitted as source.
 */
open class KotlinxSerializationSupportExtension : IrExtension {

    override fun transform(ir: IR, ast: AST): IR {
        val definitions = ast.modules.toList().flatMap { it.statements }
        val serialNames = definitions
            .filter { it is TypeDefinition || it is Refined }
            .associate { Name.of(it.identifier.value).pascalCase() to it.identifier.value }
        val serializableOnlyNames = definitions
            .filter { it is UnionDefinition || it is EnumDefinition }
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        return ir.map { element ->
            if (element is LanguageFile) element.annotateSerializable(serialNames, serializableOnlyNames) else element
        }
    }

    private fun LanguageFile.annotateSerializable(
        serialNames: Map<String, String>,
        serializableOnlyNames: Set<String>,
    ): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when {
                element is Struct && element.name.pascalCase() in serialNames ->
                    listOf(serializable, serialName(serialNames.getValue(element.name.pascalCase())), element)
                element is HasName && (element is Union || element is Enum) && element.name.pascalCase() in serializableOnlyNames ->
                    listOf(serializable, element)
                else ->
                    listOf(element)
            }
        },
    )

    private companion object {
        val serializable = RawElement("@kotlinx.serialization.Serializable")
        fun serialName(name: String) = RawElement("@kotlinx.serialization.SerialName(\"$name\")")
    }
}
