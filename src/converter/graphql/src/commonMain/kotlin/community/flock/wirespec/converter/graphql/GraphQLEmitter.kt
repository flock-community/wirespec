package community.flock.wirespec.converter.graphql

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.graphql.GraphQLDocumentPrinter.print

/**
 * Emits a single GraphQL schema (SDL) for all modules, as a GraphQL schema is one document. Wirespec converted from
 * GraphQL comes back as the schema it was converted from.
 */
public object GraphQLEmitter : Emitter {

    override val extension: FileExtension = FileExtension.GraphQL

    override val supportsDefaults: Boolean = true

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .onEach { logger.info("Emitting GraphQL from ${it.fileUri.value}") }
        .flatMap { it.statements }
        .let { WirespecToGraphQL(it).convert().print() }
        .let { nonEmptyListOf(Emitted("schema.graphql", it)) }
}
