package community.flock.wirespec.integration.jackson.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.extension.IrExtension
import community.flock.wirespec.ir.core.File as LanguageFile

/**
 * Adds Jackson polymorphism annotations to every generated `union` so the
 * concrete subtype is inferred from the JSON shape:
 *
 * ```
 * @JsonTypeInfo(use = DEDUCTION)
 * @JsonSubTypes(JsonSubTypes.Type(A::class), JsonSubTypes.Type(B::class))
 * sealed interface UserAccount
 * ```
 *
 * Records, refined types and enums need no annotations here: the Wirespec
 * Jackson module already flattens `Wirespec.Refined` to its scalar, serializes
 * `Wirespec.Enum` via `toString()`, and binds record properties by name.
 *
 * The annotations are injected as raw, fully qualified [RawElement] siblings
 * before each union declaration, so the IR stays language-neutral and generated
 * code needs only the `jackson-annotations` runtime on its classpath. Only the
 * [language]-specific class-literal (`::class` vs `.class`) and array syntax
 * (`[ ]` vs `{ }`) differ. Register on a Kotlin or Java
 * [community.flock.wirespec.ir.emit.IrEmitter] built with `EmitShared(false)`.
 */
open class JacksonExtension(
    private val language: FileExtension,
) : IrExtension {

    override fun extend(ir: IR, ast: AST): IR = ir.map { element ->
        if (element is LanguageFile) element.annotateUnions() else element
    }

    private fun LanguageFile.annotateUnions(): LanguageFile = copy(
        elements = elements.flatMap { element ->
            if (element is Union) listOf(element.polymorphismAnnotation(), element) else listOf(element)
        },
    )

    private fun Union.polymorphismAnnotation(): RawElement {
        val subTypes = members.map { it.name.pascalCase() }
        // Trailing newline keeps the annotations on their own lines in Java too,
        // whose RawElement generator (unlike Kotlin's) does not append one.
        return RawElement("$JSON_TYPE_INFO\n${jsonSubTypes(subTypes)}\n")
    }

    private fun jsonSubTypes(subTypes: List<String>): String = when (language) {
        FileExtension.Kotlin ->
            subTypes
                .joinToString(", ") { "$JACKSON.JsonSubTypes.Type(value = $it::class)" }
                .let { "@$JACKSON.JsonSubTypes(value = [$it])" }
        else ->
            subTypes
                .joinToString(", ") { "@$JACKSON.JsonSubTypes.Type(value = $it.class)" }
                .let { "@$JACKSON.JsonSubTypes({$it})" }
    }

    private companion object {
        const val JACKSON = "com.fasterxml.jackson.annotation"
        const val JSON_TYPE_INFO = "@$JACKSON.JsonTypeInfo(use = $JACKSON.JsonTypeInfo.Id.DEDUCTION)"
    }
}
