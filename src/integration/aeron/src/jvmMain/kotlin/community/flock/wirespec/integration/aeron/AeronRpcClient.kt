package community.flock.wirespec.integration.aeron

import community.flock.wirespec.kotlin.Wirespec
import io.aeron.Aeron
import io.aeron.FragmentAssembler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.agrona.concurrent.BackoffIdleStrategy
import org.agrona.concurrent.UnsafeBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.reflect.typeOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Calls Wirespec rpc definitions over Aeron.
 *
 * Requests go out on [requestChannel]/[requestStreamId]; responses come back on
 * [replyChannel]/[replyStreamId], correlated by id. Give each client sharing a
 * media driver its own reply stream.
 */
public class AeronRpcClient<S>(
    private val aeron: Aeron,
    public val serialization: S,
    private val requestChannel: String = AeronRpc.DEFAULT_CHANNEL,
    private val requestStreamId: Int = AeronRpc.DEFAULT_REQUEST_STREAM_ID,
    private val replyChannel: String = AeronRpc.DEFAULT_CHANNEL,
    private val replyStreamId: Int = AeronRpc.DEFAULT_REPLY_STREAM_ID,
) : AutoCloseable where S : Wirespec.BodySerializer, S : Wirespec.BodyDeserializer {

    private val pending = ConcurrentHashMap<Long, CompletableDeferred<RpcFrame>>()
    private val correlationIds = AtomicLong(ThreadLocalRandom.current().nextLong())
    private val running = AtomicBoolean(false)
    private val publication by lazy { aeron.addPublication(requestChannel, requestStreamId) }
    private var poller: Thread? = null

    public fun start() {
        check(running.compareAndSet(false, true)) { "AeronRpcClient already started" }
        val subscription = aeron.addSubscription(replyChannel, replyStreamId)
        poller = thread(name = "wirespec-aeron-client", isDaemon = true) {
            val assembler = FragmentAssembler { buffer, offset, length, _ ->
                val bytes = ByteArray(length).also { buffer.getBytes(offset, it) }
                // A frame this client cannot decode must not kill the poll loop.
                runCatching { RpcFrame.decode(bytes) }.getOrNull()?.let { frame -> pending.remove(frame.correlationId)?.complete(frame) }
            }
            val idle = BackoffIdleStrategy()
            while (running.get()) {
                idle.idle(subscription.poll(assembler, FRAGMENT_LIMIT))
            }
            subscription.close()
        }
    }

    /** Call on the raw frame level; returns the RESULT or ERROR frame. The typed overloads are usually what you want. */
    public suspend fun call(method: String, payload: ByteArray, timeout: Duration = 10.seconds): RpcFrame {
        val correlationId = correlationIds.incrementAndGet()
        val deferred = CompletableDeferred<RpcFrame>()
        pending[correlationId] = deferred
        return try {
            val request = RpcFrame.Request(correlationId, method, replyChannel, replyStreamId, payload)
            withContext(Dispatchers.IO) { publication.offerFully(UnsafeBuffer(request.encode())) }
            withTimeout(timeout) { deferred.await() }
        } finally {
            pending.remove(correlationId)
        }
    }

    /** Call an rpc without an error type: `rpc Method { params } -> R`. */
    public suspend inline fun <reified P : Any, reified R : Any> call(method: String, params: P): R = call(method, serialization.serializeBody(params, typeOf<P>())).toResult(method)

    /** Call a parameterless rpc: `rpc Method {} -> R`. */
    public suspend inline fun <reified R : Any> call(method: String): R = call(method, AeronRpc.EMPTY_PARAMS).toResult(method)

    /** Call an rpc with an error type: `rpc Method { params } -> R ! E`. */
    public suspend inline fun <reified P : Any, reified R : Any, reified E : Any> callResult(method: String, params: P): RpcResult<R, E> = when (val frame = call(method, serialization.serializeBody(params, typeOf<P>()))) {
        is RpcFrame.Result -> RpcResult.Success(serialization.deserializeBody(frame.payload, typeOf<R>()))
        is RpcFrame.Error -> RpcResult.Failure(serialization.deserializeBody(frame.payload, typeOf<E>()))
        is RpcFrame.Request -> error("Unexpected REQUEST frame for rpc '$method'")
    }

    public inline fun <reified R : Any> RpcFrame.toResult(method: String): R = when (this) {
        is RpcFrame.Result -> serialization.deserializeBody(payload, typeOf<R>())
        is RpcFrame.Error -> throw AeronRpcException(method, payload.decodeToString())
        is RpcFrame.Request -> error("Unexpected REQUEST frame for rpc '$method'")
    }

    override fun close() {
        if (!running.compareAndSet(true, false)) return
        poller?.join()
        publication.close()
        pending.values.forEach { it.cancel() }
        pending.clear()
    }

    internal companion object {
        const val FRAGMENT_LIMIT = 10
    }
}
