package community.flock.wirespec.integration.aeron

/** Shared defaults for the Wirespec-over-Aeron RPC protocol. */
object AeronRpc {
    const val DEFAULT_CHANNEL = "aeron:ipc"
    const val DEFAULT_REQUEST_STREAM_ID = 1001
    const val DEFAULT_REPLY_STREAM_ID = 1002

    /** Payload of a request without parameters. */
    val EMPTY_PARAMS = "{}".encodeToByteArray()
}

/** Outcome of an rpc declared with an error type (`rpc X {} -> R ! E`). */
sealed interface RpcResult<out R, out E> {
    data class Success<out R>(val value: R) : RpcResult<R, Nothing>
    data class Failure<out E>(val error: E) : RpcResult<Nothing, E>
}

/** Thrown by typed client calls when the server answers with an ERROR frame. */
class AeronRpcException(
    val method: String,
    val errorBody: String,
) : RuntimeException("Rpc '$method' failed: $errorBody")
