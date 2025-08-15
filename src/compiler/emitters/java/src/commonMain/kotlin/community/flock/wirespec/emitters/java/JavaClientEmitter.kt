package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.*
import community.flock.wirespec.compiler.core.emit.Emitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint

interface JavaClientEmitter: BaseEmitter, ClientEmitter,PackageNameEmitter, ParamEmitter, SpaceEmitter, ImportEmitter, JavaTypeDefinitionEmitter {

    override fun emitClient(ast: AST): Emitted = Emitted("Client.${extension.value}", """
        |package ${packageName.value};
        |
        |import community.flock.wirespec.java.Wirespec;
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint) -> "import ${packageName.value}.endpoint.${emit(endpoint.identifier)};" }}
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "import ${packageName.value}.model.${it.value};" }}   
        |
        |public class Client {
        |${Spacer}private final java.util.function.Function<Wirespec.Request<?>, java.util.concurrent.CompletableFuture<Wirespec.Response<?>>> handler;
        |
        |${Spacer}public Client(java.util.function.Function<Wirespec.Request<?>, java.util.concurrent.CompletableFuture<Wirespec.Response<?>>> handler) {
        |${Spacer(2)}this.handler = handler;
        |${Spacer}}
        |
        |${Spacer}public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> java.util.concurrent.CompletableFuture<Res> handle(Req req) {
        |${Spacer(2)}return (java.util.concurrent.CompletableFuture<Res>) this.handler.apply(req);
        |}
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") {(endpoint, request) -> endpoint.emitMethod(request) }.spacer(1)}
        |}
    """.trimMargin())

    fun Endpoint.emitMethod(request: Endpoint.Request) = """
        |public java.util.concurrent.CompletableFuture<${emit(identifier)}.Response<?>> ${emit(identifier).firstToLower()}(${request.emitClientInterface(this)}) {
        |${Spacer}var req = new ${emit(identifier)}.Request(${request.paramList(this).joinToString(", ") { emit(it.identifier) }});
        |${Spacer}return handle(req); 
        |}
        |
    """.trimMargin()

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${it.reference.emit()} ${emit(it.identifier)}" }
}