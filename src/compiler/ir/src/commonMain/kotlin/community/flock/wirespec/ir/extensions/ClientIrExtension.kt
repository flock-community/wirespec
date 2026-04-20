package community.flock.wirespec.ir.extensions

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.converter.requestParameters
import community.flock.wirespec.ir.converter.toName
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.ThisExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.`interface`
import community.flock.wirespec.ir.emit.AccessorStyle

interface ClientIrExtension : IrExtension {
    fun emitEndpointClient(endpoint: Endpoint): File
    fun emitClient(endpoints: List<Endpoint>, logger: Logger): File
    fun buildClientServerInterfaces(style: AccessorStyle): List<Element>
}

fun Endpoint.convertEndpointClient(): File {
    val endpointName = identifier.toName()
    val endpointNameStr = endpointName.value()

    return file(Name.of("${endpointNameStr}Client")) {
        struct(Name.of("${endpointNameStr}Client")) {
            field("serialization", Type.Custom("Wirespec.Serialization"))
            field("transportation", Type.Custom("Wirespec.Transportation"))
            implements(Type.Custom("$endpointNameStr.Call"))

            asyncFunction(endpointName, isOverride = true) {
                requestParameters().forEach { (name, type) -> arg(name, type) }
                returnType(Type.Custom("$endpointNameStr.Response", listOf(Type.Wildcard)))

                assign(
                    "request",
                    ConstructorStatement(
                        type = Type.Custom("$endpointNameStr.Request"),
                        namedArguments = requestParameters().associate { (name, _) ->
                            name to VariableReference(name)
                        },
                    ),
                )

                assign(
                    "rawRequest",
                    FunctionCall(
                        name = Name(listOf("$endpointNameStr.toRawRequest")),
                        arguments = mapOf(
                            Name.of("serialization") to FieldCall(receiver = ThisExpression, field = Name.of("serialization")),
                            Name.of("request") to VariableReference(Name.of("request")),
                        ),
                    ),
                )

                assign(
                    "rawResponse",
                    FunctionCall(
                        name = Name.of("transport"),
                        receiver = FieldCall(receiver = ThisExpression, field = Name.of("transportation")),
                        arguments = mapOf(
                            Name.of("request") to VariableReference(Name.of("rawRequest")),
                        ),
                        isAwait = true,
                    ),
                )

                returns(
                    FunctionCall(
                        name = Name(listOf("$endpointNameStr.fromRawResponse")),
                        arguments = mapOf(
                            Name.of("serialization") to FieldCall(receiver = ThisExpression, field = Name.of("serialization")),
                            Name.of("response") to VariableReference(Name.of("rawResponse")),
                        ),
                    ),
                )
            }
        }
    }
}

fun List<Endpoint>.convertClient(): File {
    val endpoints = this
    return file(Name.of("Client")) {
        struct(Name.of("Client")) {
            field("serialization", Type.Custom("Wirespec.Serialization"))
            field("transportation", Type.Custom("Wirespec.Transportation"))

            endpoints.forEach { endpoint ->
                implements(Type.Custom("${endpoint.identifier.toName().value()}.Call"))
            }

            endpoints.forEach { endpoint ->
                val endpointName = endpoint.identifier.toName()
                val endpointNameStr = endpointName.value()

                asyncFunction(endpointName, isOverride = true) {
                    endpoint.requestParameters().forEach { (name, type) -> arg(name, type) }
                    returnType(Type.Custom("$endpointNameStr.Response", listOf(Type.Wildcard)))

                    returns(
                        FunctionCall(
                            name = Name(listOf(endpointName.camelCase())),
                            receiver = ConstructorStatement(
                                type = Type.Custom("${endpointNameStr}Client"),
                                namedArguments = mapOf(
                                    Name.of("serialization") to FieldCall(receiver = ThisExpression, field = Name.of("serialization")),
                                    Name.of("transportation") to FieldCall(receiver = ThisExpression, field = Name.of("transportation")),
                                ),
                            ),
                            arguments = endpoint.requestParameters().associate { (name, _) ->
                                name to VariableReference(name)
                            },
                        ),
                    )
                }
            }
        }
    }
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
