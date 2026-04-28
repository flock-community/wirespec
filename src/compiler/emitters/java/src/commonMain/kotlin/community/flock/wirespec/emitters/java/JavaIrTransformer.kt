package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.function
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.plus
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.Function as LanguageFunction

internal fun Definition.buildModelImports(packageName: PackageName): List<Import> = importReferences()
    .filter { identifier.value != it.value }
    .map { import("${packageName.value}.model", it.value) }

internal fun File.applyRefinedStructShape(refined: Refined): File = transform {
    matchingElements { struct: Struct ->
        struct
            .copy(
                interfaces = listOf(Type.Custom("Wirespec.Refined")),
                elements = struct.elements.map { element ->
                    if (element is LanguageFunction) {
                        element.copy(isOverride = true)
                    } else element
                }
            )
            .plus(
                function("value", isOverride = true) {
                    returnType(refined.reference.convert())
                    returns(VariableReference(Name.of("value")))
                }
            )
    }
}

internal fun File.applyFunctionalInterface(fullyQualifiedPrefix: String): File = transform {
    matchingElements { it: Interface -> it.withFullyQualifiedPrefix(fullyQualifiedPrefix) }
    matchingElements { file: File ->
        val interfaceElement = file.findElement<Interface>()!!
        file.copy(elements = listOf(RawElement("@FunctionalInterface\n"), interfaceElement))
    }
}

internal fun <T : Element> T.injectHandleFunction(endpoint: Endpoint): T {
    val handlersStruct = endpoint.buildHandlersStruct()
    return transform {
        matchingElements { iface: Interface ->
            if (iface.name == Name.of("Handler")) {
                iface.transform { injectAfter { _: Interface -> listOf(handlersStruct) } }
            } else {
                iface
            }
        }
    }
}

internal fun <T : Element> T.wrapAsyncReturnInThenApply(endpointName: String): T = transform {
    matchingElements { func: LanguageFunction ->
        if (func.isAsync && func.body.size >= 2) {
            val transportAssign = func.body[func.body.size - 2]
            val returnStmt = func.body.last()
            if (transportAssign is Assignment && returnStmt is ReturnStatement) {
                val bodyPrefix = func.body.dropLast(2)
                func.copy(
                    body = bodyPrefix + ReturnStatement(
                        FunctionCall(
                            name = Name.of("thenApply"),
                            receiver = transportAssign.value,
                            arguments = mapOf(
                                Name.of("mapper") to RawExpression(
                                    "rawResponse -> $endpointName.fromRawResponse(serialization(), rawResponse)"
                                )
                            )
                        )
                    )
                )
            } else func
        } else func
    }
}

internal fun Endpoint.buildHandlersStruct(): Struct {
    val pathTemplate = "/" + path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }

    return struct(name = "Handlers") {
        implements(
            type("Wirespec.Server", type("Request"), type("Response", Type.Wildcard))
        )
        implements(
            type("Wirespec.Client", type("Request"), type("Response", Type.Wildcard))
        )
        function("getPathTemplate", isOverride = true) {
            returnType(Type.String)
            returns(literal(pathTemplate))
        }
        function("getMethod", isOverride = true) {
            returnType(Type.String)
            returns(literal(method.name))
        }
        function("getServer", isOverride = true) {
            returnType(
                type("Wirespec.ServerEdge", type("Request"), type("Response", Type.Wildcard))
            )
            arg("serialization", type("Wirespec.Serialization"))
            returns(
                RawExpression(
                    "new Wirespec.ServerEdge<>() {\n" +
                            "@Override public Request from(Wirespec.RawRequest request) {\n" +
                            "    return fromRawRequest(serialization, request);\n" +
                            "}\n" +
                            "@Override public Wirespec.RawResponse to(Response<?> response) {\n" +
                            "    return toRawResponse(serialization, response);\n" +
                            "}\n" +
                            "}"
                ),
            )
        }
        function("getClient", isOverride = true) {
            returnType(
                type("Wirespec.ClientEdge", type("Request"), type("Response", Type.Wildcard))
            )
            arg("serialization", type("Wirespec.Serialization"))
            returns(
                RawExpression(
                    "new Wirespec.ClientEdge<>() {\n" +
                            "@Override public Wirespec.RawRequest to(Request request) {\n" +
                            "    return toRawRequest(serialization, request);\n" +
                            "}\n" +
                            "@Override public Response<?> from(Wirespec.RawResponse response) {\n" +
                            "    return fromRawResponse(serialization, response);\n" +
                            "}\n" +
                            "}"
                ),
            )
        }
    }
}

internal fun Interface.withFullyQualifiedPrefix(prefix: String): Interface =
    if (prefix.isNotEmpty()) {
        transform {
            parametersWhere(
                predicate = { it.name == Name.of("message") },
                transform = { param ->
                    when (val t = param.type) {
                        is Type.Custom -> param.copy(type = t.copy(name = prefix + t.name))
                        else -> param
                    }
                },
            )
        }
    } else {
        this
    }

internal fun <T : Element> T.transformTypeDescriptors(): T = transform {
    statementAndExpression { stmt, tr ->
        when (stmt) {
            is TypeDescriptor -> {
                val rootType = stmt.type.findRoot()
                val containerStr = stmt.type.rawContainerClass()
                val rootStr = "${rootType.toJavaName()}.class"
                val containerArg = containerStr?.let { "$it.class" } ?: "null"
                RawExpression("Wirespec.getType($rootStr, $containerArg)")
            }
            else -> stmt.transformChildren(tr)
        }
    }
}

private fun Type.findRoot(): Type = when (this) {
    is Type.Nullable -> type.findRoot()
    is Type.Array -> elementType.findRoot()
    is Type.Dict -> valueType.findRoot()
    else -> this
}

private fun Type.rawContainerClass(): String? = when (this) {
    is Type.Nullable -> "java.util.Optional"
    is Type.Array -> "java.util.List"
    is Type.Dict -> "java.util.Map"
    else -> null
}

private fun Type.toJavaName(): String = when (this) {
    is Type.Integer -> when (precision) {
        Precision.P32 -> "Integer"; Precision.P64 -> "Long"
    }

    is Type.Number -> when (precision) {
        Precision.P32 -> "Float"; Precision.P64 -> "Double"
    }

    Type.String -> "String"
    Type.Boolean -> "Boolean"
    Type.Bytes -> "byte[]"
    Type.Any -> "Object"
    Type.Unit -> "Void"
    is Type.Custom -> name
    else -> "Object"
}
