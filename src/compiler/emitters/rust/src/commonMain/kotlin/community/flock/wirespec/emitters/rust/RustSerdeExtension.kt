package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds serde derives to every generated model struct so the emitted types
 * (de)serialize through serde_json without hand-written mapping code — the
 * Rust counterpart of the Jackson extension on the JVM side:
 *
 * - model structs get `#[derive(serde::Serialize, serde::Deserialize)]`;
 * - every field gets `#[serde(rename = "<wirespec name>")]`, preserving the
 *   original field name even when the emitter snake_cases it or escapes a
 *   reserved keyword.
 *
 * The derives are emitted fully qualified, so the generated code needs no
 * `use` statements; the consuming crate adds `serde` with the `derive`
 * feature. Enums and unions are left untouched. Opt-in — without this
 * extension the generated code stays dependency-free.
 */
public class RustSerdeExtension : IrExtension {

    override fun extend(ir: IR, ast: AST): IR {
        val fieldNamesByRecord = ast.modules.toList()
            .flatMap { it.statements }
            .filterIsInstance<TypeDefinition>()
            .associate { type -> Name.of(type.identifier.value).pascalCase() to type.shape.value.map { it.identifier.value } }
        return ir.map { element ->
            if (element is LanguageFile) element.annotate(fieldNamesByRecord) else element
        }
    }

    private fun LanguageFile.annotate(fieldNamesByRecord: Map<String, List<String>>): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when {
                element is Struct -> fieldNamesByRecord[element.name.pascalCase()]
                    ?.let { listOf(serdeDerive, element.rename(it)) }
                    ?: listOf(element)

                else -> listOf(element)
            }
        },
    )

    private fun Struct.rename(originalNames: List<String>): Struct {
        val names = originalNames.iterator()
        return copy(
            fields = fields.map { element ->
                if (element is Field && names.hasNext()) {
                    element.copy(annotations = element.annotations + """#[serde(rename = "${names.next()}")]""")
                } else {
                    element
                }
            },
        )
    }

    private companion object {
        val serdeDerive = RawElement("#[derive(serde::Serialize, serde::Deserialize)]")
    }
}
