package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.ir.raw
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint

internal fun Definition.buildModelImports(packageName: PackageName): List<Import> = importReferences()
    .distinctBy { it.value }
    .map { import("${packageName.value}.model", Name.of(it.value).pascalCase()) }

internal fun Namespace.injectApiObject(endpoint: Endpoint): Namespace = transform {
    injectAfter { _: Namespace ->
        listOf(buildApiObject(endpoint))
    }
}

private fun buildApiObject(endpoint: Endpoint): RawElement {
    val pathTemplate = "/" + endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return """
        |public object api : Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        |  override val pathTemplate: String = "$pathTemplate"
        |  override val method: String = "${endpoint.method}"
        |  override fun server(serialization: Wirespec.Serialization): Wirespec.ServerEdge<Request, Response<*>> = object : Wirespec.ServerEdge<Request, Response<*>> {
        |    override fun from(request: Wirespec.RawRequest) = fromRawRequest(serialization, request)
        |    override fun to(response: Response<*>) = toRawResponse(serialization, response)
        |  }
        |  override fun client(serialization: Wirespec.Serialization): Wirespec.ClientEdge<Request, Response<*>> = object : Wirespec.ClientEdge<Request, Response<*>> {
        |    override fun to(request: Request) = toRawRequest(serialization, request)
        |    override fun from(response: Wirespec.RawResponse) = fromRawResponse(serialization, response)
        |  }
        |}
    """.trimMargin().let(::raw)
}

/** A string literal type admits one value, so the field takes that value as its default. */
internal fun File.withLiteralDefaults(): File = transform {
    matchingElements { struct: Struct -> struct.copy(fields = struct.fields.map { (it as? Field)?.withLiteralDefault() ?: it }) }
}

private fun Field.withLiteralDefault(): Field = when (val type = type) {
    is Type.StringLiteral -> copy(initializer = Literal(type.value, Type.String))
    else -> this
}
