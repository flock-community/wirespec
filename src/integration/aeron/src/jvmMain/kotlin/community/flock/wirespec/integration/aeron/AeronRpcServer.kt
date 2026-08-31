package community.flock.wirespec.integration.aeron

import community.flock.wirespec.kotlin.Wirespec
import io.aeron.Aeron
import io.aeron.FragmentAssembler
import io.aeron.Publication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.agrona.concurrent.BackoffIdleStrategy
import org.agrona.concurrent.UnsafeBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.reflect.typeOf

/**
 * Serves Wirespec rpc definitions over Aeron.
 *
 * Bind one handler per rpc method, then [start]. Requests arrive on
 * [channel]/[streamId]; each response is published on the reply channel and
 * stream the request names, so any number of clients can share the request
 * stream while receiving answers on their own.
 */
class AeronRpcServer<S>(
    private val aeron: Aeron,
    val serialization: S,
    private val channel: String = AeronRpc.DEFAULT_CHANNEL,
    private val streamId: Int = AeronRpc.DEFAULT_REQUEST_STREAM_ID,
) : AutoCloseable where S : Wirespec.BodySerializer, S : Wirespec.BodyDeserializer {

    sealed interface Reply {
        val payload: ByteArray

        class Result(override val payload: ByteArray) : Reply
        class Error(override val payload: ByteArray) : Reply
    }

    private val handlers = ConcurrentHashMap<String, suspend (ByteArray) -> Reply>()
    private val replyPublications = ConcurrentHashMap<Pair<String, Int>, Publication>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = AtomicBoolean(false)
    private var poller: Thread? = null

    /** Bind a handler on the raw frame level; the typed overloads are usually what you want. */
    fun bindRaw(method: String, handler: suspend (ByteArray) -> Reply) {
        handlers[method] = handler
    }

    /** Bind an rpc without an error type: `rpc Method { params } -> R`. */
    inline fun <reified P : Any, reified R : Any> bind(method: String, noinline handler: suspend (P) -> R) = bindRaw(method) { payload ->
        Reply.Result(serialization.serializeBody(handler(serialization.deserializeBody(payload, typeOf<P>())), typeOf<R>()))
    }

    /** Bind a parameterless rpc: `rpc Method {} -> R`. */
    inline fun <reified R : Any> bindEmpty(method: String, noinline handler: suspend () -> R) = bindRaw(method) {
        Reply.Result(serialization.serializeBody(handler(), typeOf<R>()))
    }

    /** Bind an rpc with an error type: `rpc Method { params } -> R ! E`. */
    inline fun <reified P : Any, reified R : Any, reified E : Any> bindResult(method: String, noinline handler: suspend (P) -> RpcResult<R, E>) = bindRaw(method) { payload ->
        when (val outcome = handler(serialization.deserializeBody(payload, typeOf<P>()))) {
            is RpcResult.Success -> Reply.Result(serialization.serializeBody(outcome.value, typeOf<R>()))
            is RpcResult.Failure -> Reply.Error(serialization.serializeBody(outcome.error, typeOf<E>()))
        }
    }

    fun start() {
        check(running.compareAndSet(false, true)) { "AeronRpcServer already started" }
        val subscription = aeron.addSubscription(channel, streamId)
        poller = thread(name = "wirespec-aeron-server", isDaemon = true) {
            val assembler = FragmentAssembler { buffer, offset, length, _ ->
                val bytes = ByteArray(length).also { buffer.getBytes(offset, it) }
                // A frame this server cannot decode must not kill the poll loop.
                runCatching { RpcFrame.decode(bytes) }.onSuccess(::dispatch)
            }
            val idle = BackoffIdleStrategy()
            while (running.get()) {
                idle.idle(subscription.poll(assembler, FRAGMENT_LIMIT))
            }
            subscription.close()
        }
    }

    private fun dispatch(frame: RpcFrame) {
        if (frame !is RpcFrame.Request) return
        scope.launch {
            val reply = handlers[frame.method]
                ?.let { handler -> runCatching { handler(frame.payload) }.getOrElse { Reply.Error("Rpc '${frame.method}' failed: ${it.message}".encodeToByteArray()) } }
                ?: Reply.Error("Unknown rpc method: ${frame.method}".encodeToByteArray())
            val response = when (reply) {
                is Reply.Result -> RpcFrame.Result(frame.correlationId, frame.method, reply.payload)
                is Reply.Error -> RpcFrame.Error(frame.correlationId, frame.method, reply.payload)
            }
            replyPublications
                .computeIfAbsent(frame.replyChannel to frame.replyStreamId) { (channel, streamId) -> aeron.addPublication(channel, streamId) }
                .offerFully(UnsafeBuffer(response.encode()))
        }
    }

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        poller?.join()
        scope.cancel()
        replyPublications.values.forEach(Publication::close)
        replyPublications.clear()
    }

    internal companion object {
        const val FRAGMENT_LIMIT = 10
    }
}
