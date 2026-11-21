package community.flock.wirespec.integration.rustserde.extension

import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Enum
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Main
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Union
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Model
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumDefinition
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionDefinition

/**
 * Makes the generated Rust models serializable with [serde](https://serde.rs), in the JSON form of the contract:
 *
 * - a type derives `Serialize` and `Deserialize`, and every field is renamed to its Wirespec name: `item_id` is
 *   written as `itemId`;
 * - a refined type derives them as `#[serde(transparent)]`, so it is written as the value it wraps;
 * - an enum is written as its label, the Wirespec name of the entry;
 * - a union derives them as `#[serde(tag = "type")]`: the name of the member goes into a `type` property, like the
 *   Jackson and kotlinx.serialization extensions do. Serde can only add that property to a member that is a type,
 *   not to an enum or a refined type.
 *
 * In the shared `wirespec` module, `BodySerializer` and `BodyDeserializer` get a serde bound on the body, so one
 * implementation can hand every body to a serde data format such as `serde_json`.
 *
 * The attributes are fully qualified and no import is added: the crate that holds the generated code only needs the
 * `serde` crate with its `derive` feature. Register on a Rust
 * [community.flock.wirespec.compiler.core.ir.emit.IrEmitter].
 */
public class RustSerdeExtension : IrExtension {

    override fun extend(ir: IR, ast: AST): IR = ast.modules.flatMap { it.statements }
        .filterIsInstance<Model>()
        .associateBy { Name.of(it.identifier.value).pascalCase() }
        .let { models -> ir.map { element -> (element as? LanguageFile)?.withSerde(models) ?: element } }

    private fun LanguageFile.withSerde(models: Map<String, Model>): LanguageFile = when {
        isModel() -> copy(elements = elements.flatMap { it.withSerde(models) })
        isSharedModule() -> copy(elements = elements.map { it.withSerdeBodies() })
        else -> this
    }

    // An endpoint has structs of its own, and one of them can share its name with a model. Only a file in the model
    // directory declares a model.
    private fun LanguageFile.isModel(): Boolean = name.value().substringBeforeLast('/', "").substringAfterLast('/') == MODEL_DIRECTORY

    private fun LanguageFile.isSharedModule(): Boolean = name.value().substringAfterLast('/') == SHARED_MODULE

    private fun Element.withSerde(models: Map<String, Model>): List<Element> = when (this) {
        is Struct -> when (val model = models[name.pascalCase()]) {
            is TypeDefinition -> listOf(DERIVE, withWirespecFieldNames(model))
            is Refined -> listOf(DERIVE, TRANSPARENT, this)
            is EnumDefinition, is UnionDefinition, null -> listOf(this)
        }

        is Union -> if (name.pascalCase() in models) listOf(DERIVE, TAGGED, this) else listOf(this)
        is Enum -> if (name.pascalCase() in models && entries.isNotEmpty()) listOf(this, labelSerialization()) else listOf(this)
        is Package, is Import, is Function, is Namespace, is Interface, is Main, is LanguageFile, is Field, is RawElement -> listOf(this)
    }

    // The emitter writes a field in snake case, so the Wirespec name comes from the definition: both list the fields
    // in the same order.
    private fun Struct.withWirespecFieldNames(type: TypeDefinition): Struct = copy(
        fields = fieldList().zip(type.shape.fields) { field, definition -> listOf(rename(definition.identifier.value), field) }.flatten(),
    )

    private fun Enum.labelSerialization(): RawElement = name.pascalCase().let { enum ->
        RawElement(
            """
            impl serde::Serialize for $enum {
                fn serialize<S: serde::Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
                    serializer.serialize_str(self.label())
                }
            }
            impl<'de> serde::Deserialize<'de> for $enum {
                fn deserialize<D: serde::Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
                    <String as serde::Deserialize>::deserialize(deserializer)?.parse().map_err(serde::de::Error::custom)
                }
            }
            """.trimIndent(),
        )
    }

    /**
     * The Rust emitter hands the shared module over as source, not as IR, so the bounds are added in that source.
     * Without them a serializer gets a body it knows nothing about, and cannot pass it on to serde.
     */
    private fun Element.withSerdeBodies(): Element = (this as? RawElement)
        ?.let { RawElement(BODY_BOUNDS.entries.fold(it.code) { code, (unbound, bound) -> code.replace(unbound, bound) }) }
        ?: this

    private companion object {
        const val MODEL_DIRECTORY = "model"
        const val SHARED_MODULE = "wirespec"

        val DERIVE = RawElement("#[derive(serde::Serialize, serde::Deserialize)]")
        val TRANSPARENT = RawElement("#[serde(transparent)]")
        val TAGGED = RawElement("#[serde(tag = \"type\")]")

        val BODY_BOUNDS = mapOf(
            "fn serialize_body<T: 'static>" to "fn serialize_body<T: serde::Serialize + 'static>",
            "fn deserialize_body<T: 'static>" to "fn deserialize_body<T: serde::de::DeserializeOwned + 'static>",
        )

        fun rename(name: String) = RawElement("#[serde(rename = \"${name.replace("\\", "\\\\").replace("\"", "\\\"")}\")]")
    }
}
