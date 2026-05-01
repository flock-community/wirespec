package community.flock.wirespec.emitters.scala

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Import as LanguageImport
import community.flock.wirespec.ir.core.Type as LanguageType

internal fun Definition.buildModelImports(packageName: PackageName): List<LanguageImport> = importReferences()
    .distinctBy { it.value }
    .map { import("${packageName.value}.model", it.value) }

internal fun <T : Element> T.convertToStringCallsToFieldAccess(): T = transform {
    expression { expr, tr ->
        when {
            expr is FunctionCall && expr.name.camelCase() == "toString" && expr.receiver != null ->
                FieldCall(receiver = expr.receiver?.let { tr.transformExpression(it) }, field = Name.of("toString"))
            else -> expr.transformChildren(tr)
        }
    }
}

internal fun <T : Element> T.addIdentityTypeToCall(): T = transform {
    matchingElements { struct: Struct ->
        struct.copy(
            interfaces = struct.interfaces.map { type ->
                (type as? LanguageType.Custom)?.takeIf { it.name.endsWith(".Call") }
                    ?.copy(generics = listOf(LanguageType.Custom("[A] =>> A")))
                    ?: type
            },
        )
    }
}

internal fun isRequestObject(namespace: Namespace): Boolean {
    val requestStruct = namespace.elements.filterIsInstance<Struct>()
        .firstOrNull { it.name.pascalCase() == "Request" } ?: return false
    return (requestStruct.constructors.size == 1 && requestStruct.constructors.single().parameters.isEmpty()) ||
        (requestStruct.fields.isEmpty() && requestStruct.constructors.isEmpty())
}

internal fun Namespace.injectHandleFunction(): Namespace = transform {
    matchingElements { iface: Interface ->
        if (iface.name != Name.of("Handler") && iface.name != Name.of("Call")) return@matchingElements iface
        iface.copy(
            typeParameters = listOf(TypeParameter(LanguageType.Custom("F[_]"))),
            elements = iface.elements.map { element ->
                (element as? LanguageFunction)?.copy(
                    isAsync = false,
                    returnType = element.returnType?.let { LanguageType.Custom("F", generics = listOf(it)) },
                ) ?: element
            },
        )
    }
}

internal fun Namespace.appendClientServerObjects(endpoint: Endpoint, requestIsObject: Boolean): Namespace {
    val reqType = if (requestIsObject) "Request.type" else "Request"
    val pathTemplate = "/" + endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    val clientObject = raw(
        """
        |object Client extends Wirespec.Client[$reqType, Response[?]] {
        |  override val pathTemplate: String = "$pathTemplate"
        |  override val method: String = "${endpoint.method}"
        |  override def client(serialization: Wirespec.Serialization): Wirespec.ClientEdge[$reqType, Response[?]] = new Wirespec.ClientEdge[$reqType, Response[?]] {
        |    override def to(request: $reqType): Wirespec.RawRequest = toRawRequest(serialization, request)
        |    override def from(response: Wirespec.RawResponse): Response[?] = fromRawResponse(serialization, response)
        |  }
        |}
        """.trimMargin(),
    )
    val serverObject = raw(
        """
        |object Server extends Wirespec.Server[$reqType, Response[?]] {
        |  override val pathTemplate: String = "$pathTemplate"
        |  override val method: String = "${endpoint.method}"
        |  override def server(serialization: Wirespec.Serialization): Wirespec.ServerEdge[$reqType, Response[?]] = new Wirespec.ServerEdge[$reqType, Response[?]] {
        |    override def from(request: Wirespec.RawRequest): $reqType = fromRawRequest(serialization, request)
        |    override def to(response: Response[?]): Wirespec.RawResponse = toRawResponse(serialization, response)
        |  }
        |}
        """.trimMargin(),
    )
    return copy(elements = elements + clientObject + serverObject)
}
