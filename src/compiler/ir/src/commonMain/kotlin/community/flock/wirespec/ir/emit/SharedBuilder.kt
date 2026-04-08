package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.`interface`

enum class AccessorStyle {
    GETTER_METHODS,
    PROPERTIES,
}

fun buildClientServerInterfaces(style: AccessorStyle): List<Element> {
    val (pathTemplateName, methodName, clientFnName, serverFnName) = when (style) {
        AccessorStyle.GETTER_METHODS -> listOf("getPathTemplate", "getMethod", "getClient", "getServer")
        AccessorStyle.PROPERTIES -> listOf("pathTemplate", "method", "client", "server")
    }
    val useFields = style == AccessorStyle.PROPERTIES

    return listOf(
        `interface`("ServerEdge") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            function("from") {
                returnType(type("Req"))
                arg("request", type("RawRequest"))
            }
            function("to") {
                returnType(type("RawResponse"))
                arg("response", type("Res"))
            }
        },
        `interface`("ClientEdge") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            function("to") {
                returnType(type("RawRequest"))
                arg("request", type("Req"))
            }
            function("from") {
                returnType(type("Res"))
                arg("response", type("RawResponse"))
            }
        },
        `interface`("Client") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            if (useFields) {
                field(pathTemplateName, Type.String)
                field(methodName, Type.String)
            } else {
                function(pathTemplateName) { returnType(Type.String) }
                function(methodName) { returnType(Type.String) }
            }
            function(clientFnName) {
                returnType(type("ClientEdge", type("Req"), type("Res")))
                arg("serialization", type("Serialization"))
            }
        },
        `interface`("Server") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            if (useFields) {
                field(pathTemplateName, Type.String)
                field(methodName, Type.String)
            } else {
                function(pathTemplateName) { returnType(Type.String) }
                function(methodName) { returnType(Type.String) }
            }
            function(serverFnName) {
                returnType(type("ServerEdge", type("Req"), type("Res")))
                arg("serialization", type("Serialization"))
            }
        },
    )
}
