package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Function
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.Type
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
                    element is Interface && element.name == "Handler" ->
                        element.transform(handlerTransformer) as Interface
                    else -> element.transformChildren(t)
                }
            },
        )) as Interface
    }

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value};" }
}