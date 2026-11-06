package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.transform
import community.flock.wirespec.compiler.core.parse.ast.AST

/**
 * Makes the server side synchronous: the functions of every endpoint `Handler`, channel `Sender` and `Listener`, and
 * rpc `Service` become plain functions instead of a `suspend` function in Kotlin, a `CompletableFuture` in Java, a
 * promise in TypeScript or a coroutine in Python. Function types they take, such as a listener's callback, become
 * synchronous too.
 *
 * A synchronous handler runs on the thread that received the request, so state bound to that thread, such as a JDBC
 * transaction or a security context, stays available. A coroutine can resume on another thread and lose it. The
 * endpoint client side (`Call`, the generated clients and `Transportation`) stays asynchronous.
 *
 * Scala handlers already abstract over their effect, and the Rust `Handler` is implemented by the client, so both
 * are left as they are.
 */
public class SyncExtension(language: FileExtension) : IrExtension {

    private val syncInterfaces = mapOf(
        "Wirespec.Endpoint" to setOf(Name.of("Handler")),
        "Wirespec.Channel" to setOf(Name.of("Sender"), Name.of("Listener")),
        "Wirespec.Rpc" to setOf(Name.of("Service")),
    )

    private val supported = language in setOf(FileExtension.Java, FileExtension.Kotlin, FileExtension.TypeScript, FileExtension.Python)

    override fun extend(ir: IR, ast: AST): IR = if (supported) ir.map(::withSync) else ir

    private fun withSync(element: Element): Element = element.transform {
        matchingElements { namespace: Namespace ->
            namespace.extends
                ?.let { syncInterfaces[it.name.value()] }
                ?.let { namespace.withSyncInterfaces(it) }
                ?: namespace
        }
    }

    private fun Namespace.withSyncInterfaces(names: Set<Name>): Namespace = copy(
        elements = elements.map { element ->
            (element as? Interface)?.takeIf { it.name in names }?.sync() ?: element
        },
    )

    private fun Interface.sync(): Interface = transform {
        matchingElements { function: Function -> function.copy(isAsync = false) }
        matching { function: Type.Function -> function.copy(isAsync = false) }
    }
}
