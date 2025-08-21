package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.paramList
import community.flock.wirespec.compiler.core.emit.spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint

interface JavaClientEmitter: ClientEmitter, HasPackageName, JavaTypeDefinitionEmitter {

    override fun emitClient(ast: AST): List<Emitted> {
        return emitClientInterfaces(ast) + listOf(emitClientClass(ast))
    }

    override fun emitClientInterfaces(ast: AST): List<Emitted> = emptyList()

    override fun emitClientClass(ast: AST): Emitted = Emitted("${packageName.toDir()}/Client.${extension.value}", """
        |package ${packageName.value};
        |
        |import community.flock.wirespec.java.Wirespec;
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint) -> "import ${packageName.value}.endpoint.${emit(endpoint.identifier)};" }}
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "import ${packageName.value}.model.${it.value};" }}   
        |
        |public class Client {
        |${Spacer}private final Wirespec.Serialization<String> serialization;
        |${Spacer}private final java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler;
        |
        |${Spacer}public Client(Wirespec.Serialization<String> serialization, java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler) {
        |${Spacer(2)}this.serialization = serialization;
        |${Spacer(2)}this.handler = handler;
        |${Spacer}}
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint, request) -> endpoint.emitMethod(request) }.spacer(1)}
        |}
    """.trimMargin())

    fun Endpoint.emitMethod(request: Endpoint.Request) = """
        |public java.util.concurrent.CompletableFuture<${emit(identifier)}.Response<?>> ${emit(identifier).firstToLower()}(${request.emitClientInterface(this)}) {
        |${Spacer}var req = new ${emit(identifier)}.Request(${request.paramList(this).joinToString(", ") { emit(it.identifier) }});
        |${Spacer}var rawReq = ${emit(identifier)}.Handler.toRequest(serialization, req);
        |${Spacer}return handler.apply(rawReq)
        |   .thenApply(rawRes -> ${emit(identifier)}.Handler.fromResponse(serialization, rawRes));
        |}
    """.trimMargin()

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${it.reference.emit()} ${emit(it.identifier)}" }
}
