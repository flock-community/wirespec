package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.parse.ast.AST

/**
 * Makes the server side synchronous: the function of every endpoint `Handler` and rpc `Service` becomes a plain
 * function instead of a `suspend` function in Kotlin, a `CompletableFuture` in Java, a promise in TypeScript or a
 * coroutine in Python.
 *
 * A synchronous handler runs on the thread that received the request, so state bound to that thread, such as a JDBC
 * transaction or a security context, stays available. A coroutine can resume on another thread and lose it. The
 * client side (`Call`, the generated clients and `Transportation`) stays asynchronous.
 *
 * Scala handlers already abstract over their effect, and the Rust `Handler` is implemented by the client, so both
 * are left as they are.
 */
public class SyncHandlersExtension(language: FileExtension) : IrExtension {

    private val serverInterfaces = mapOf(
        "Wirespec.Endpoint" to Name.of("Handler"),
        "Wirespec.Rpc" to Name.of("Service"),
    )

    private val supported = language in setOf(FileExtension.Java, FileExtension.Kotlin, FileExtension.TypeScript, FileExtension.Python)

    override fun extend(ir: IR, ast: AST): IR = if (supported) ir.map(::withSyncHandlers) else ir

    private fun withSyncHandlers(element: Element): Element = element.transform {
        matchingElements { namespace: Namespace ->
            namespace.extends
                ?.let { serverInterfaces[it.name.value()] }
                ?.let { namespace.withSyncInterface(it) }
                ?: namespace
        }
    }

    private fun Namespace.withSyncInterface(name: Name): Namespace = copy(
        elements = elements.map { element ->
            (element as? Interface)?.takeIf { it.name == name }?.withSyncFunctions() ?: element
        },
    )

    private fun Interface.withSyncFunctions(): Interface = copy(
        elements = elements.map { (it as? Function)?.copy(isAsync = false) ?: it },
    )
}
