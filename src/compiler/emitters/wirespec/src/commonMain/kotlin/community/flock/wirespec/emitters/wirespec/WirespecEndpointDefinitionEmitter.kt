package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.fixStatus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Field

interface WirespecEndpointDefinitionEmitter:  EndpointDefinitionEmitter, WirespecTypeDefinitionEmitter {

    override fun emit(endpoint: Endpoint) = """
        |endpoint ${emit(endpoint.identifier)} ${endpoint.method}${endpoint.requests.emitRequest()} ${endpoint.path.emitPath()}${endpoint.queries.emitQuery()}${endpoint.headers.emitHeader()} -> {
        |${endpoint.responses.joinToString("\n") { "$Spacer${it.status.fixStatus()} -> ${it.content?.reference?.emit() ?: "Unit"}${if (it.content?.reference?.isNullable == true) "?" else ""}${it.headers.emitHeader()}" }}
        |}
        |
    """.trimMargin()

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}: ${it.reference.emit()}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Endpoint.Request>.emitRequest() =
        firstOrNull()?.content?.reference?.emit()?.let { " $it" }.orEmpty()

    private fun List<Field>.emitQuery() = takeIf { it.isNotEmpty() }
        ?.joinToString(",", "{", "}") { it.emit() }
        ?.let { " ?$it" }
        .orEmpty()

    private fun List<Field>.emitHeader() = takeIf { it.isNotEmpty() }
        ?.joinToString(",", "{", "}") { it.emit() }
        ?.let { " #$it" }
        .orEmpty()
}
