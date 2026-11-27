package community.flock.wirespec.integration.kotlinxserialization.extension

import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.ir.Enum
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.Union
import community.flock.wirespec.compiler.core.ir.extension.Discriminator
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.extension.discriminatorErrors
import community.flock.wirespec.compiler.core.ir.extension.discriminators
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumDefinition
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionDefinition

/**
 * Adds kotlinx.serialization annotations to every generated model declaration:
 *
 * - records and refined types get `@Serializable` + `@SerialName("<wirespec name>")`
 * - enums get `@Serializable`
 * - a union gets a serializer of its own, which writes a member as it is, without the `type` property
 *   kotlinx.serialization adds to a sealed interface by default
 * - a union with `@KotlinxSerializationDiscriminator("kind")` is read by that field: its serializer picks the
 *   member that has the literal of the JSON in it (`kind: "dog"`). A union without the annotation cannot be read:
 *   nothing in the JSON says which member it is
 * - a field of a string literal type gets `@EncodeDefault`, so its literal is always written
 *
 * Refined types are emitted as `@JvmInline value class`, which kotlinx.serialization
 * flattens to the wrapped primitive: `{"id":"42"}` instead of `{"id":{"value":"42"}}`.
 *
 * The annotations are emitted fully qualified, so generated code needs only the
 * kotlinx-serialization runtime on its classpath, with its JSON format for a union — no imports are added and the
 * IR stays language-neutral.
 *
 * Only the top-level model declarations of each [LanguageFile] are annotated;
 * endpoint-internal structs live nested inside their endpoint `Namespace` and are
 * left untouched. Register on a Kotlin
 * [community.flock.wirespec.compiler.core.ir.emit.IrEmitter] built with `EmitShared(false)`, so
 * the runtime classes come from the wirespec-jvm dependency rather than being
 * re-emitted as source.
 */
public class KotlinxSerializationExtension : IrExtension {

    override fun validate(ast: AST): List<EmitterException> = ast.discriminatorErrors(DISCRIMINATOR)

    override fun extend(ir: IR, ast: AST): IR {
        val definitions = ast.modules.toList().flatMap { it.statements }
        val serialNames = definitions
            .filter { it is TypeDefinition || it is Refined }
            .associate { Name.of(it.identifier.value).pascalCase() to it.identifier.value }
        val refinedNames = definitions
            .filterIsInstance<Refined>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        val enumNames = definitions
            .filterIsInstance<EnumDefinition>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        val unionNames = definitions
            .filterIsInstance<UnionDefinition>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        val discriminators = ast.discriminators(DISCRIMINATOR).mapKeys { (union, _) -> Name.of(union).pascalCase() }
        return ir.map { element ->
            if (element is LanguageFile) element.annotateSerializable(serialNames, refinedNames, enumNames, unionNames, discriminators) else element
        }
    }

    private fun LanguageFile.annotateSerializable(
        serialNames: Map<String, String>,
        refinedNames: Set<String>,
        enumNames: Set<String>,
        unionNames: Set<String>,
        discriminators: Map<String, Discriminator>,
    ): LanguageFile = copy(
        elements = elements.flatMap { element ->
            when {
                element is Struct && element.name.pascalCase() in serialNames ->
                    listOfNotNull(
                        experimentalOptIn.takeIf { element.literalFields().isNotEmpty() },
                        serializable,
                        serialName(serialNames.getValue(element.name.pascalCase())),
                        if (element.name.pascalCase() in refinedNames) element.copy(kind = Struct.Kind.VALUE_CLASS) else element.encodingLiteralFields(),
                    )
                element is Enum && element.name.pascalCase() in enumNames ->
                    listOf(serializable, element)
                element is Union && element.name.pascalCase() in unionNames ->
                    element.name.pascalCase().let { union -> listOf(serializableWith(union), element, unionSerializer(union, discriminators[union])) }
                else ->
                    listOf(element)
            }
        },
    )

    /**
     * A field of a string literal type has that literal as its default, and kotlinx.serialization leaves a property
     * that still has its default out of the JSON. The literal is part of the contract, so it is always written.
     */
    private fun Struct.encodingLiteralFields(): Struct = literalFields()
        .let { literals -> copy(fields = fields.flatMap { field -> if (field in literals) listOf(encodeDefault, field) else listOf(field) }) }

    private fun Struct.literalFields(): List<Field> = fieldList().filter { it.type is Type.StringLiteral }

    private companion object {
        val encodeDefault = RawElement("@kotlinx.serialization.EncodeDefault")

        // `@EncodeDefault` is an experimental API of kotlinx.serialization.
        val experimentalOptIn = RawElement("@kotlin.OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)")
        val serializable = RawElement("@kotlinx.serialization.Serializable")

        /** The annotation on a union that names the field its members are told apart with: `@KotlinxSerializationDiscriminator("kind")`. */
        const val DISCRIMINATOR = "KotlinxSerializationDiscriminator"
        private const val JSON = "kotlinx.serialization.json"

        fun serializableWith(union: String) = RawElement("@kotlinx.serialization.Serializable(with = ${union}Serializer::class)")

        /**
         * kotlinx.serialization writes a sealed interface with a `type` property of its own and reads the member from
         * it, and it rejects a class that has a property with the name of that discriminator. So a union gets a
         * serializer that looks at the content: it writes a member as it is, and reads the member from a field the
         * members declare themselves.
         */
        fun unionSerializer(union: String, discriminator: Discriminator?) = RawElement(
            """
            |public object ${union}Serializer : $JSON.JsonContentPolymorphicSerializer<$union>($union::class) {
            |  override fun selectDeserializer(element: $JSON.JsonElement): kotlinx.serialization.DeserializationStrategy<$union> =
            |${discriminator?.selectMember(union) ?: cannotBeRead(union)}
            |}
            """.trimMargin(),
        )

        private fun Discriminator.selectMember(union: String) = literals
            .map { (member, literal) -> "      \"$literal\" -> ${Name.of(member).pascalCase()}.serializer()" }
            .joinToString("\n")
            .let { members ->
                """
                |    when (val literal = ((element as? $JSON.JsonObject)?.get("$field") as? $JSON.JsonPrimitive)?.content) {
                |$members
                |      else -> throw kotlinx.serialization.SerializationException("$union has no member with the $field '${'$'}literal'")
                |    }
                """.trimMargin()
            }

        private fun cannotBeRead(union: String) = "    throw kotlinx.serialization.SerializationException(\"$union cannot be read: name the field that tells its members apart with @$DISCRIMINATOR\")"

        fun serialName(name: String) = RawElement("@kotlinx.serialization.SerialName(\"$name\")")
    }
}
