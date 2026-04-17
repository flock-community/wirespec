package community.flock.wirespec.integration.spring.java.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.java.JavaEmitter

abstract class SpringJavaEmitterHelper(packageName: PackageName) : JavaEmitter(packageName, EmitShared(false)) {

    abstract fun injectFiles(definitions: List<Definition>): List<Emitted>

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger).let { results ->
        injectFiles(ast.modules.flatMap { it.statements })
            .takeIf { it.isNotEmpty() }
            ?.let { results + it }
            ?: results
    }

    override fun emitResponseBodyType(response: Endpoint.Response): String {
        response.validateStreaming()
        return if (response.isStreaming()) RESOURCE_TYPE
        else super.emitResponseBodyType(response)
    }

    override fun emitResponseSerializeBody(response: Endpoint.Response): String =
        if (response.isStreaming()) "java.util.Optional.empty()"
        else super.emitResponseSerializeBody(response)

    override fun emitResponseDeserializeBody(response: Endpoint.Response): String =
        if (response.isStreaming())
            "response.body().<$RESOURCE_TYPE>map(body -> (org.springframework.core.io.Resource) new org.springframework.core.io.ByteArrayResource(body)).orElse(null)"
        else super.emitResponseDeserializeBody(response)

    override fun emitHandlersExtras(endpoint: Endpoint): String =
        if (endpoint.responses.any { it.isStreaming() }) "${Spacer(3)}public static final boolean STREAMING = true;"
        else ""

    private fun Endpoint.Response.validateStreaming() {
        if (!isStreaming()) return
        val ref = content?.reference
        require(ref is Reference.Primitive && ref.type is Reference.Primitive.Type.Bytes) {
            "@Streaming is only allowed on responses whose body is `Bytes`"
        }
    }

    companion object {
        private const val STREAMING_ANNOTATION = "Streaming"
        private const val RESOURCE_TYPE = "org.springframework.core.io.Resource"

        private fun Endpoint.Response.isStreaming(): Boolean =
            annotations.any { it.name == STREAMING_ANNOTATION }
    }
}
