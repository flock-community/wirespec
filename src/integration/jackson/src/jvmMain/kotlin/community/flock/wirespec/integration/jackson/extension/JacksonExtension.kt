package community.flock.wirespec.integration.jackson.extension

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds Jackson annotations to every generated model declaration so the emitted
 * classes (de)serialize correctly without registering a custom Jackson module:
 *
 * - record fields get `@JsonProperty("<wirespec name>")`, preserving the original
 *   field name even when the emitter has to sanitize a reserved keyword.
 * - unions get `@JsonTypeInfo` + `@JsonSubTypes`, mapping each member type to its
 *   wirespec name so polymorphic values round-trip through a `type` discriminator.
 *
 * The annotations are emitted fully qualified against `com.fasterxml.jackson.annotation`
 * (shared by Jackson 2 and 3), so generated code needs only the jackson-annotations
 * runtime on its classpath — no imports are added and the IR stays language-neutral.
 *
 * Only the top-level model declarations of each [LanguageFile] are annotated;
 * endpoint-internal structs live nested inside their endpoint `Namespace` and are
 * left untouched. Register on a Kotlin
 * [community.flock.wirespec.ir.emit.IrEmitter] built with `EmitShared(false)`, so
 * the runtime classes come from the wirespec-jvm dependency rather than being
 * re-emitted as source.
 */
class JacksonExtension : IrExtension {

    override fun extend(ir: IR, ast: AST): IR {
        val definitions = ast.modules.toList().flatMap { it.statements }
        val recordNames = definitions
            .filterIsInstance<TypeDefinition>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        return ir.map { element ->
            if (element is LanguageFile) element.annotateJackson(recordNames) else element
        }
    }

    private fun LanguageFile.annotateJackson(recordNames: Set<String>): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when {
                element is Struct && element.name.pascalCase() in recordNames ->
                    listOf(element.annotateFields())
                element is Union ->
                    listOf(jsonTypeInfo, jsonSubTypes(element), element)
                else ->
                    listOf(element)
            }
        },
    )

    private fun Struct.annotateFields(): Struct = copy(
        fields = fields.map { field ->
            field.copy(annotations = field.annotations + jsonProperty(field.name.value()))
        },
    )

    private companion object {
        private const val ANNOTATION = "com.fasterxml.jackson.annotation"

        fun jsonProperty(name: String) = "@$ANNOTATION.JsonProperty(\"$name\")"

        val jsonTypeInfo = RawElement(
            "@$ANNOTATION.JsonTypeInfo(" +
                "use = $ANNOTATION.JsonTypeInfo.Id.NAME, " +
                "include = $ANNOTATION.JsonTypeInfo.As.PROPERTY, " +
                "property = \"type\")",
        )

        fun jsonSubTypes(union: Union): RawElement {
            val types = union.members.joinToString(",\n") { member ->
                val reference = member.name.referenceName()
                "    $ANNOTATION.JsonSubTypes.Type(value = $reference::class, name = \"${member.name.pascalCase()}\")"
            }
            return RawElement("@$ANNOTATION.JsonSubTypes(\n$types,\n)")
        }
    }
}
