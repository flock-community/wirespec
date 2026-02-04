package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.AnonymousClass
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.MethodCall
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.struct
import community.flock.wirespec.language.core.transform
import community.flock.wirespec.language.core.transformChildren
import community.flock.wirespec.language.core.transformer
import community.flock.wirespec.language.generator.generateJava
import community.flock.wirespec.language.core.Function as LanguageFunction
import community.flock.wirespec.language.core.function as dslFunction

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

    fun buildHandleFunction(endpoint: Endpoint) = dslFunction(
        name = emit(endpoint.identifier).firstToLower(),
        returnType = Type.Custom(
            "java.util.concurrent.CompletableFuture",
            listOf(Type.Custom("Response", listOf(Type.Wildcard))),
        ),
    ) {
        arg("request", Type.Custom("Request"))
    }

    private fun Interface.injectHandleFunction(endpoint: Endpoint): Interface {
        val rawHandleFunction = RawElement(emitHandleFunction(endpoint))
        val targetFunctionName = endpoint.identifier.value.replaceFirstChar { it.lowercase() }
        val handlersStruct = buildHandlers(endpoint)

        val handlerTransformer = transformer(
            transformElement = { element, t ->
                when {
                    element is LanguageFunction && element.name == targetFunctionName -> rawHandleFunction
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

        return struct(
            name = "Handlers",
            interfaces = listOf(
                Type.Custom("Wirespec.Server", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Wildcard)))),
                Type.Custom("Wirespec.Client", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Wildcard)))),
            ),
        ) {
            function("getPathTemplate", Type.String, isOverride = true) {
                returns(literal(pathTemplate))
            }
            function("getMethod", Type.String, isOverride = true) {
                returns(literal(endpoint.method.name))
            }
            function("getServer", Type.Custom("Wirespec.ServerEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Wildcard)))), isOverride = true) {
                arg("serialization", Type.Custom("Wirespec.Serialization"))
                returns(
                    AnonymousClass(
                        baseType = Type.Custom("Wirespec.ServerEdge"),
                        typeArguments = emptyList(),
                        methods = listOf(
                            dslFunction("from", Type.Custom("Request"), isOverride = true) {
                                arg("request", Type.Custom("Wirespec.RawRequest"))
                                returns(
                                    MethodCall(
                                        method = "fromRawRequest",
                                        arguments = mapOf(
                                            "serialization" to VariableReference("serialization"),
                                            "request" to VariableReference("request"),
                                        ),
                                    ),
                                )
                            },
                            dslFunction("to", Type.Custom("Wirespec.RawResponse"), isOverride = true) {
                                arg("response", Type.Custom("Response", listOf(Type.Wildcard)))
                                returns(
                                    MethodCall(
                                        method = "toRawResponse",
                                        arguments = mapOf(
                                            "serialization" to VariableReference("serialization"),
                                            "response" to VariableReference("response"),
                                        ),
                                    ),
                                )
                            },
                        ),
                    ),
                )
            }
            function("getClient", Type.Custom("Wirespec.ClientEdge", listOf(Type.Custom("Request"), Type.Custom("Response", listOf(Type.Wildcard)))), isOverride = true) {
                arg("serialization", Type.Custom("Wirespec.Serialization"))
                returns(
                    AnonymousClass(
                        baseType = Type.Custom("Wirespec.ClientEdge"),
                        typeArguments = emptyList(),
                        methods = listOf(
                            dslFunction("to", Type.Custom("Wirespec.RawRequest"), isOverride = true) {
                                arg("request", Type.Custom("Request"))
                                returns(
                                    MethodCall(
                                        method = "toRawRequest",
                                        arguments = mapOf(
                                            "serialization" to VariableReference("serialization"),
                                            "request" to VariableReference("request"),
                                        ),
                                    ),
                                )
                            },
                            dslFunction("from", Type.Custom("Response", listOf(Type.Wildcard)), isOverride = true) {
                                arg("response", Type.Custom("Wirespec.RawResponse"))
                                returns(
                                    MethodCall(
                                        method = "fromRawResponse",
                                        arguments = mapOf(
                                            "serialization" to VariableReference("serialization"),
                                            "response" to VariableReference("response"),
                                        ),
                                    ),
                                )
                            },
                        ),
                    ),
                )
            }
        }
    }

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value};" }
}