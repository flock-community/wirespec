package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Rpc

/**
 * Sends and serves every `rpc` of the contract as a [JSON-RPC 2.0](https://www.jsonrpc.org/specification) method, the
 * way the emitters send and serve an endpoint: the generated code is dependency-free, and the consumer brings the
 * `Wirespec.Transportation` that moves a message and the `Wirespec.Serialization` that reads and writes the values.
 *
 * ```kotlin
 * val client = JsonRpcClient(serialization, transportation)
 * val user = client.getUser(id = "1")
 *
 * val server = JsonRpcServer(serialization, service)
 * val response: Wirespec.RawResponse = server.handle(request)
 * ```
 *
 * One file next to the `Client` of the endpoints holds:
 *
 * - a `JsonRpcClient` with a function for every rpc. It writes a request, posts it as the body of a
 *   `Wirespec.RawRequest` through the transportation, and reads the result from the body of the response;
 * - a `JsonRpcServer` that takes a service implementing the `Service` of every rpc. Its `handle` reads a single or a
 *   batch request from the body of a `Wirespec.RawRequest`, calls the service, and answers with a `Wirespec.RawResponse`.
 *   A notification gets no answer: when nothing is answered, the response has status 204 and no body;
 * - a `JsonRpcException` (`JsonRpcError` in TypeScript, Python and Rust) for an error object the server answers with,
 *   and the codes of the errors JSON-RPC defines.
 *
 * The method of an rpc is its name in camel case, `GetUser` is sent as `getUser`, unless it is annotated with
 * `@JsonRpcMethod("textDocument/hover")`. The params are sent by name: an object with the fields of the rpc under their
 * Wirespec names. The server reads params by name or by position, in the order of the fields.
 *
 * An rpc with an error type, `rpc GetUser { id: String } -> User ! UserNotFound`, answers its error as an error object
 * with the name of the error type as its message, the error as its data, and code [ERROR_CODE], or the code of
 * `@JsonRpcError(404)`. The client reads an error object with that code as the `Error` of the rpc, and any other error
 * object as a `JsonRpcException`.
 *
 * The client and the server follow the [JSON-RPC 2.0 specification](https://www.jsonrpc.org/specification): a method
 * may not start with `rpc.`, and the code of the error of an rpc is one JSON-RPC leaves to the application, outside
 * -32768 to -32000. The extension fails on a contract that breaks either.
 *
 * Register on any emitter but Wirespec; for any other [language] the IR is returned unchanged.
 */
public class JsonRpcExtension(
    private val packageName: PackageName,
    language: FileExtension,
) : IrExtension {

    private val dialect: JsonRpcDialect? = when (language) {
        FileExtension.Kotlin -> KotlinJsonRpc
        FileExtension.Java -> JavaJsonRpc
        FileExtension.Scala -> ScalaJsonRpc
        FileExtension.TypeScript -> TypeScriptJsonRpc
        FileExtension.Python -> PythonJsonRpc
        FileExtension.Rust -> RustJsonRpc
        FileExtension.Wirespec,
        FileExtension.JSON,
        FileExtension.YAML,
        FileExtension.AvroJson,
        FileExtension.AvroIdl,
        FileExtension.GraphQL,
        -> null
    }

    override fun extend(ir: IR, ast: AST): IR = dialect
        ?.let { dialect ->
            ast.modules.flatMap { it.statements }.filterIsInstance<Rpc>()
                .map { JsonRpcMethod(it, ir.serviceOf(it)) }
                .takeIf { it.isNotEmpty() }
                ?.let { dialect.extend(ir, packageName, it) }
        }
        ?: ir

    public companion object {
        /** The code of the error object an rpc answers its own error with, unless it is annotated with another one. */
        public const val ERROR_CODE: Int = 1

        /** Names the method of an rpc, when it is not the name of the rpc in camel case. */
        public const val METHOD_ANNOTATION: String = "JsonRpcMethod"

        /** Gives the code of the error object an rpc answers its own error with, when it is not [ERROR_CODE]. */
        public const val ERROR_ANNOTATION: String = "JsonRpcError"
    }
}
