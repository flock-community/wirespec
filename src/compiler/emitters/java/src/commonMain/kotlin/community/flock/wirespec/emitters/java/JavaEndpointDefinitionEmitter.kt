package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.AnonymousClass
import community.flock.wirespec.language.core.Function
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.MethodCall
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.transform
import community.flock.wirespec.language.core.transformChildren
import community.flock.wirespec.language.core.transformer
import community.flock.wirespec.language.generator.generateJava

interface JavaEndpointDefinitionEmitter : EndpointDefinitionEmitter, HasPackageName, JavaTypeDefinitionEmitter {

    override fun emit(endpoint: Endpoint): String {
        val imports = endpoint.emitImports()
        val body = endpoint.convert()
            .injectHandleFunction(endpoint)
            .generateJava()

        return if (imports.isNotEmpty()) {
            "$imports\n\n$body"
        } else {
            body
        }
    }

    fun emitHandleFunction(endpoint: Endpoint): String {
        val name = emit(endpoint.identifier).firstToLower()
        return "public java.util.concurrent.CompletableFuture<Response<?>> $name(Request request);\n"
    }

    fun buildHandleFunction(endpoint: Endpoint): Function = Function(
        name = emit(endpoint.identifier).firstToLower(),
        parameters = listOf(Parameter("request", Type.Custom("Request"))),
        returnType = Type.Custom(
            "java.util.concurrent.CompletableFuture",
            listOf(Type.Custom("Response", listOf(Type.Custom("?"))))
        ),
        body = emptyList(),
        isAsync = false,
        isStatic = false,
        isOverride = false,
    )

    private fun Interface.injectHandleFunction(endpoint: Endpoint): Interface {
        val rawHandleFunction = RawElement(emitHandleFunction(endpoint))
        val targetFunctionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
        val handlersStruct = buildHandlers(endpoint)

        val handlerTransformer = transformer(
            transformElement = { element, t ->
                when {
                    element is Function && element.name == targetFunctionName -> rawHandleFunction
                    else -> element.transformChildren(t)
                }
            },
        )

        return transform(transformer(
            transformElement = { element, t ->
                when {
                    element is Interface && element.name == "Handler" -> {
                        val transformed = element.transform(handlerTransformer) as Interface
                        transformed.copy(elements = transformed.elements + handlersStruct)
                    }
                    else -> element.transformChildren(t)
                }
            },
        )) as Interface
    }

    private fun buildHandlers(endpoint: Endpoint): Struct {
        val pathTemplate = "/" + endpoint.path.joinToString("/") {
            when (it) {
                is Endpoint.Segment.Literal -> it.value
                is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            }
        }

        return Struct(
            name = "Handlers",
            fields = emptyList(),
            interfaces = listOf(
                Type.Custom("Wirespec.Server", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
                Type.Custom("Wirespec.Client", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
            ),
            elements = listOf(
                Function(
                    name = "getPathTemplate",
                    parameters = emptyList(),
                    returnType = Type.String,
                    isOverride = true,
                    body = listOf(ReturnStatement(Literal(pathTemplate, Type.String))),
                ),
                Function(
                    name = "getMethod",
                    parameters = emptyList(),
                    returnType = Type.String,
                    isOverride = true,
                    body = listOf(ReturnStatement(Literal(endpoint.method.name, Type.String))),
                ),
                Function(
                    name = "getServer",
                    parameters = listOf(Parameter("serialization", Type.Custom("Wirespec.Serialization"))),
                    returnType = Type.Custom("Wirespec.ServerEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
                    isOverride = true,
                    body = listOf(
                        ReturnStatement(
                            AnonymousClass(
                                baseType = Type.Custom("Wirespec.ServerEdge"),
                                typeArguments = emptyList(),
                                methods = listOf(
                                    Function(
                                        name = "from",
                                        parameters = listOf(Parameter("request", Type.Custom("Wirespec.RawRequest"))),
                                        returnType = Type.Custom("Request"),
                                        isOverride = true,
                                        body = listOf(
                                            ReturnStatement(
                                                MethodCall(
                                                    method = "fromRequest",
                                                    arguments = mapOf(
                                                        "serialization" to VariableReference("serialization"),
                                                        "request" to VariableReference("request"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                    Function(
                                        name = "to",
                                        parameters = listOf(Parameter("response", Type.Custom("Response", listOf(Type.Custom("?"))))),
                                        returnType = Type.Custom("Wirespec.RawResponse"),
                                        isOverride = true,
                                        body = listOf(
                                            ReturnStatement(
                                                MethodCall(
                                                    method = "toResponse",
                                                    arguments = mapOf(
                                                        "serialization" to VariableReference("serialization"),
                                                        "response" to VariableReference("response"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Function(
                    name = "getClient",
                    parameters = listOf(Parameter("serialization", Type.Custom("Wirespec.Serialization"))),
                    returnType = Type.Custom("Wirespec.ClientEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Custom("?"))))),
                    isOverride = true,
                    body = listOf(
                        ReturnStatement(
                            AnonymousClass(
                                baseType = Type.Custom("Wirespec.ClientEdge"),
                                typeArguments = emptyList(),
                                methods = listOf(
                                    Function(
                                        name = "to",
                                        parameters = listOf(Parameter("request", Type.Custom("Request"))),
                                        returnType = Type.Custom("Wirespec.RawRequest"),
                                        isOverride = true,
                                        body = listOf(
                                            ReturnStatement(
                                                MethodCall(
                                                    method = "toRequest",
                                                    arguments = mapOf(
                                                        "serialization" to VariableReference("serialization"),
                                                        "request" to VariableReference("request"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                    Function(
                                        name = "from",
                                        parameters = listOf(Parameter("response", Type.Custom("Wirespec.RawResponse"))),
                                        returnType = Type.Custom("Response", listOf(Type.Custom("?"))),
                                        isOverride = true,
                                        body = listOf(
                                            ReturnStatement(
                                                MethodCall(
                                                    method = "fromResponse",
                                                    arguments = mapOf(
                                                        "serialization" to VariableReference("serialization"),
                                                        "response" to VariableReference("response"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value};" }
}