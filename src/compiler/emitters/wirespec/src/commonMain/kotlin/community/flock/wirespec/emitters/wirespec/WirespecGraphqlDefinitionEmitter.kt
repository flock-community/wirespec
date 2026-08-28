package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.GraphqlDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Graphql

interface WirespecGraphqlDefinitionEmitter :
    GraphqlDefinitionEmitter,
    WirespecTypeDefinitionEmitter {
    override fun emit(graphql: Graphql): String = graphql.inputs
        .joinToString(", ", "{", "}") { it.emit() }
        .let { inputs -> "graphql ${emit(graphql.identifier)} ${graphql.kind.name} $inputs -> ${graphql.output.emit()}" }
}
