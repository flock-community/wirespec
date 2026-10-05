package community.flock.wirespec.converter.graphql

import arrow.core.NonEmptyList
import arrow.core.nel
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.graphql.GraphQLDocumentPrinter.print

/**
 * Emits a GraphQL schema (SDL) for all modules together. Wirespec converted from GraphQL becomes the schema it came
 * from; see [WirespecToGraphQL] for how other Wirespec maps onto GraphQL.
 */
public object GraphQLEmitter : Emitter {

    override val extension: FileExtension = FileExtension.GraphQL

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = WirespecToGraphQL(ast.modules.flatMap { it.statements }, logger)
        .convert()
        .let { Emitted("schema.${extension.value}", it.print()) }
        .nel()
}
