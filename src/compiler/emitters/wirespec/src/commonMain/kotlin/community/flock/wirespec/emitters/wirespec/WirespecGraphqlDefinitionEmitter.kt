package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.GraphqlDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Graphql

interface WirespecGraphqlDefinitionEmitter :
    GraphqlDefinitionEmitter,
    WirespecTypeDefinitionEmitter {
    override fun emit(graphql: Graphql): String = graphql.inputs
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "(", ")") { it.emit() }
        .orEmpty()
        .let { inputs -> "graphql ${emit(graphql.identifier)} ${graphql.kind.name} ${emit(graphql.operation)}$inputs -> ${graphql.output.emit()}" }
}
