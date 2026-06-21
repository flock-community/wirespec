package community.flock.wirespec.integration.jackson.extension

import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.Field
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
 * - unions get `@JsonTypeInfo(... property = "type")`, and each union member record
 *   gets `@JsonTypeName("<wirespec name>")`, so polymorphic values round-trip through
 *   a `type` discriminator. Subtypes are resolved from the sealed hierarchy that both
 *   the Java (`permits`) and Kotlin (`sealed interface`) emitters generate.
 *
 * The annotations are emitted fully qualified against `com.fasterxml.jackson.annotation`
 * (shared by Jackson 2 and 3) and use only syntax that is valid in **both** Java and
 * Kotlin, so the same extension instance can be registered on either emitter. Field
 * annotations are injected as [RawElement]s directly in front of the [Field] they
 * annotate; the generators render any raw element that precedes a field as that field's
 * annotation. No imports are added and the IR stays language-neutral.
 *
 * Only the top-level model declarations of each [LanguageFile] are annotated;
 * endpoint-internal structs live nested inside their endpoint `Namespace` and are
 * left untouched. Register on a Java or Kotlin
 * [community.flock.wirespec.ir.emit.IrEmitter] built with `EmitShared(false)`, so the
 * runtime classes come from the wirespec-jvm dependency rather than being re-emitted as
 * source.
 */
class JacksonExtension : IrExtension {

    override fun extend(ir: IR, ast: AST): IR {
        val recordNames = ast.modules.toList()
            .flatMap { it.statements }
            .filterIsInstance<TypeDefinition>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        val unionMemberNames = ir.filterIsInstance<LanguageFile>()
            .flatMap { it.elements }
            .filterIsInstance<Union>()
            .flatMap { union -> union.members.map { it.name.pascalCase() } }
            .toSet()
        return ir.map { element ->
            if (element is LanguageFile) element.annotate(recordNames, unionMemberNames) else element
        }
    }

    private fun LanguageFile.annotate(recordNames: Set<String>, unionMemberNames: Set<String>): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when (element) {
                is Union -> listOf(jsonTypeInfo, element)
                is Struct -> {
                    val pascal = element.name.pascalCase()
                    when {
                        pascal !in recordNames -> listOf(element)
                        pascal in unionMemberNames -> listOf(jsonTypeName(pascal), element.annotateFields())
                        else -> listOf(element.annotateFields())
                    }
                }
                else -> listOf(element)
            }
        },
    )

    private fun Struct.annotateFields(): Struct = copy(
        fields = fields.flatMap { element ->
            if (element is Field) listOf(jsonProperty(element.name.value()), element) else listOf(element)
        },
    )

    private companion object {
        private const val ANNOTATION = "com.fasterxml.jackson.annotation"

        fun jsonProperty(name: String) = RawElement("@$ANNOTATION.JsonProperty(\"$name\")")

        val jsonTypeInfo = RawElement(
            "@$ANNOTATION.JsonTypeInfo(" +
                "use = $ANNOTATION.JsonTypeInfo.Id.NAME, " +
                "include = $ANNOTATION.JsonTypeInfo.As.PROPERTY, " +
                "property = \"type\")\n",
        )

        fun jsonTypeName(name: String) = RawElement("@$ANNOTATION.JsonTypeName(\"$name\")\n")
    }
}
