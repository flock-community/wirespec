package community.flock.wirespec.converter.graphql

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.exceptions.EmitterException
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.HasAnnotations
import community.flock.wirespec.compiler.core.parse.ast.SpecAnnotation
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.DIRECTIVE_DEFINITION
import community.flock.wirespec.converter.graphql.GraphQLAnnotations.SCHEMA
import community.flock.wirespec.converter.graphql.GraphQLDocumentPrinter.print

/**
 * Emits a single GraphQL schema (SDL) for all modules, as a GraphQL schema is one document. Wirespec converted from
 * GraphQL comes back as the schema it was converted from.
 */
public object GraphQLEmitter : Emitter {

    override val extension: FileExtension = FileExtension.GraphQL

    override val supportsDefaults: Boolean = true

    /** The annotations that only the schema as a whole has, so only the file header holds them. */
    private val schemaOnly = setOf(SCHEMA, DIRECTIVE_DEFINITION)

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast
        .also { it.modules.forEach { module -> logger.info("Emitting GraphQL from ${module.fileUri.value}") } }
        .let { WirespecToGraphQL(it).convert().print() }
        .let { nonEmptyListOf(Emitted("schema.graphql", it)) }

    override fun validate(ast: AST): List<EmitterException> = ast.misplacedSchemaAnnotations() + ast.annotations.declaredTwice()

    private fun AST.misplacedSchemaAnnotations(): List<EmitterException> = modules.toList()
        .flatMap { module -> module.statements.map { module.fileUri to it } }
        .flatMap { (fileUri, definition) ->
            (definition as? HasAnnotations)?.annotations.orEmpty()
                .filter { it.name in schemaOnly }
                .map {
                    EmitterException(
                        fileUri,
                        "@${it.name} on ${definition.identifier.value} belongs to the GraphQL schema as a whole, so it belongs in the file header: write it as @spec:${it.name}",
                    )
                }
        }

    /** All files of a spec make one schema, so what it has once can be declared in one file only. */
    private fun List<SpecAnnotation>.declaredTwice(): List<EmitterException> = mapNotNull { spec -> spec.annotation.declaration()?.let { it to spec } }
        .groupBy({ it.first }, { it.second })
        .filterValues { it.size > 1 }
        .map { (declaration, declared) ->
            EmitterException(
                declared[1].fileUri,
                "$declaration is declared ${declared.size} times (in ${declared.map { it.fileUri.value }.distinct().joinToString()}), but a GraphQL schema can only have one",
            )
        }

    /** What a `@spec:` annotation declares that a GraphQL schema has only one of: its schema definition, which extensions add to, or a directive by name. */
    private fun Annotation.declaration(): String? = when (name) {
        SCHEMA -> "The schema definition, a @spec:$SCHEMA without extend: \"true\",".takeUnless { value("extend") == "true" }
        DIRECTIVE_DEFINITION -> value("name")?.let { "The directive @$it" }
        else -> null
    }

    private fun Annotation.value(parameter: String): String? = (parameters.find { it.name == parameter }?.value as? Annotation.Value.Single)?.value
}
