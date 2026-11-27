package community.flock.wirespec.integration.jackson.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Union
import community.flock.wirespec.compiler.core.ir.extension.Discriminator
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.extension.discriminatorErrors
import community.flock.wirespec.compiler.core.ir.extension.discriminators
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition

/**
 * Adds Jackson annotations to every generated model declaration so the emitted
 * classes (de)serialize correctly without registering a custom Jackson module:
 *
 * - record fields get `@JsonProperty("<wirespec name>")`, preserving the original
 *   field name even when the emitter has to sanitize a reserved keyword.
 * - a union with `@JacksonDiscriminator("kind")` gets `@JsonTypeInfo` on that field, and
 *   `@JsonSubTypes` with the literal every member has in it (`kind: "dog"`). The field is
 *   one the spec declares, so Jackson reads the member from it and writes it as the
 *   property it already is: nothing is added to the JSON.
 * - a union without the annotation is not annotated. Jackson writes its members as they
 *   are, and cannot read one back: nothing in the JSON says which member it is.
 *
 * The annotations are emitted fully qualified against `com.fasterxml.jackson.annotation`
 * (shared by Jackson 2 and 3). Only `@JsonSubTypes` is written differently in Java and
 * Kotlin, so the extension takes the language it extends. Field
 * annotations are injected as [RawElement]s directly in front of the [Field] they
 * annotate; the generators render any raw element that precedes a field as that field's
 * annotation. No imports are added and the IR stays language-neutral.
 *
 * Only the top-level model declarations of each [LanguageFile] are annotated;
 * endpoint-internal structs live nested inside their endpoint `Namespace` and are
 * left untouched. Register on a Java or Kotlin
 * [community.flock.wirespec.compiler.core.ir.emit.IrEmitter] built with `EmitShared(false)`, so the
 * runtime classes come from the wirespec-jvm dependency rather than being re-emitted as
 * source.
 */
public class JacksonExtension(private val language: FileExtension) : IrExtension {

    override fun validate(ast: AST): List<EmitterException> = ast.discriminatorErrors(DISCRIMINATOR)

    override fun extend(ir: IR, ast: AST): IR {
        val recordNames = ast.modules.asSequence()
            .flatMap { it.statements }
            .filterIsInstance<TypeDefinition>()
            .map { it.identifier.value }
            .map(Name::of)
            .map { it.pascalCase() }
            .toSet()
        val discriminators = ast.discriminators(DISCRIMINATOR).mapKeys { (union, _) -> Name.of(union).pascalCase() }
        return ir.map { (it as? LanguageFile)?.annotate(recordNames, discriminators) ?: it }
    }

    private fun LanguageFile.annotate(recordNames: Set<String>, discriminators: Map<String, Discriminator>): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when (element) {
                is Union -> discriminators[element.name.pascalCase()]
                    ?.let { listOf(jsonTypeInfo(it.field), it.jsonSubTypes(), element) }
                    ?: listOf(element)

                is Struct -> if (element.name.pascalCase() in recordNames) listOf(element.annotateFields()) else listOf(element)

                else -> listOf(element)
            }
        },
    )

    /**
     * Jackson 2 does not find the members of a sealed interface of Java by itself, so the union lists them. An array of
     * annotations and a class are written differently in Java and Kotlin.
     */
    private fun Discriminator.jsonSubTypes(): RawElement = literals
        .map { (member, literal) -> Name.of(member).pascalCase() to literal }
        .let { members ->
            when (language) {
                FileExtension.Kotlin -> members.joinToString(prefix = "(", postfix = ")") { (member, literal) -> "$ANNOTATION.JsonSubTypes.Type(value = $member::class, name = \"$literal\")" }
                else -> members.joinToString(prefix = "({", postfix = "})") { (member, literal) -> "@$ANNOTATION.JsonSubTypes.Type(value = $member.class, name = \"$literal\")" }
            }
        }
        .let { RawElement("@$ANNOTATION.JsonSubTypes$it\n") }

    private fun Struct.annotateFields(): Struct = copy(
        fields = fields.flatMap { element ->
            if (element is Field) listOf(jsonProperty(element.name.value()), element) else listOf(element)
        },
    )

    private companion object {
        private const val ANNOTATION = "com.fasterxml.jackson.annotation"

        fun jsonProperty(name: String) = RawElement("@$ANNOTATION.JsonProperty(\"$name\")")

        /** The annotation on a union that names the field Jackson tells its members apart with: `@JacksonDiscriminator("kind")`. */
        const val DISCRIMINATOR = "JacksonDiscriminator"

        /**
         * `EXISTING_PROPERTY` keeps Jackson from writing a property of its own next to the field, and `visible` hands
         * the field to the member after Jackson has read which member it is: without it the member is built without
         * its literal.
         */
        fun jsonTypeInfo(field: String) = RawElement(
            "@$ANNOTATION.JsonTypeInfo(" +
                "use = $ANNOTATION.JsonTypeInfo.Id.NAME, " +
                "include = $ANNOTATION.JsonTypeInfo.As.EXISTING_PROPERTY, " +
                "property = \"$field\", " +
                "visible = true)\n",
        )
    }
}
