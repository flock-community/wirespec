package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.fields

/**
 * The field that tells the members of a union apart, and the literal every member has in it. Wirespec has no
 * discriminator of its own: an extension reads it from an annotation it owns, such as `@JacksonDiscriminator("kind")`.
 * Every member declares the field itself, with a string literal type: `kind: "dog"`. The [literals] are keyed by the
 * name of the member.
 */
public data class Discriminator(val field: String, val literals: Map<String, String>)

/** The discriminator of every union that has the [annotation], by the name of the union. */
public fun AST.discriminators(annotation: String): Map<String, Discriminator> = definitions()
    .let { definitions ->
        definitions
            .filterIsInstance<Union>()
            .mapNotNull { union -> union.discriminator(annotation, definitions)?.let { union.identifier.value to it } }
            .toMap()
    }

/**
 * What keeps the unions with the [annotation] from being told apart by it: an annotation without a field, a member
 * that does not have the field as a string literal, and two members with the same literal.
 */
public fun AST.discriminatorErrors(annotation: String): List<EmitterException> = definitions()
    .let { definitions ->
        modules.toList().flatMap { module ->
            module.statements
                .filterIsInstance<Union>()
                .filter { union -> union.annotations.any { it.name == annotation } }
                .flatMap { it.discriminatorErrors(annotation, definitions) }
                .map { EmitterException(module.fileUri, it) }
        }
    }

private fun AST.definitions(): List<Definition> = modules.toList().flatMap { it.statements }

private fun Union.discriminator(annotation: String, definitions: List<Definition>): Discriminator? = discriminatorField(annotation)
    ?.let { field ->
        Discriminator(
            field = field,
            literals = entries
                .mapNotNull { member -> definitions.literalOf(member, field)?.let { member.value to it } }
                .toMap(),
        )
    }

private fun Union.discriminatorField(annotation: String): String? = annotations
    .find { it.name == annotation }
    ?.parameters
    ?.find { it.name == "default" }
    ?.value
    ?.let { it as? Annotation.Value.Single }
    ?.value

private fun List<Definition>.literalOf(member: Reference, field: String): String? = filterIsInstance<Type>()
    .find { it.identifier.value == member.value }
    ?.shape
    ?.fields
    ?.find { it.identifier.value == field }
    ?.reference
    ?.let { it as? Reference.Literal }
    ?.value

private fun Union.discriminatorErrors(annotation: String, definitions: List<Definition>): List<String> = discriminator(annotation, definitions)
    ?.let { it.membersWithoutLiteral(annotation, this) + it.membersWithTheSameLiteral(annotation, this) }
    ?: listOf("@$annotation on union '${identifier.value}' needs the name of a field: @$annotation(\"kind\")")

private fun Discriminator.membersWithoutLiteral(annotation: String, union: Union): List<String> = union.entries
    .map { it.value }
    .filterNot { it in literals }
    .map { "@$annotation(\"$field\") on union '${union.identifier.value}': its member '$it' is not a type with a field '$field' of a string literal type" }

private fun Discriminator.membersWithTheSameLiteral(annotation: String, union: Union): List<String> = literals.entries
    .groupBy({ it.value }, { it.key })
    .filterValues { it.size > 1 }
    .map { (literal, members) -> "@$annotation(\"$field\") on union '${union.identifier.value}': its members ${members.joinToString(" and ")} all have \"$literal\" in it" }
