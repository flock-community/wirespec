package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform

internal fun Definition.buildModelImports(packageName: PackageName): List<Import> = importReferences()
    .distinctBy { it.value }
    .map { import("${packageName.value}.model", it.value) }

internal fun Namespace.injectCompanionObject(endpoint: Endpoint): Namespace =
    transform {
        injectAfter { iface: Interface ->
            if (iface.name == Name.of("Handler")) listOf(buildCompanionObject(endpoint)) else emptyList()
        }
    }

private fun buildCompanionObject(endpoint: Endpoint): RawElement {
    val pathTemplate = "/" + endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return """
        |companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        |  override val pathTemplate = "$pathTemplate"
        |  override val method = "${endpoint.method}"
        |  override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
        |    override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
        |    override fun to(response: Response<*>) = toRawResponse(serialization, response)
        |  }
        |  override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
        |    override fun to(request: Request) = toRawRequest(serialization, request)
        |    override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
        |  }
        |}
    """.trimMargin().let(::raw)
}
