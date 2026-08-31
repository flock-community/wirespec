package community.flock.wirespec.integration.aeron

/** Shared defaults for the Wirespec-over-Aeron RPC protocol. */
public object AeronRpc {
    public const val DEFAULT_CHANNEL: String = "aeron:ipc"
    public const val DEFAULT_REQUEST_STREAM_ID: Int = 1001
    public const val DEFAULT_REPLY_STREAM_ID: Int = 1002

    /** Payload of a request without parameters. */
    public val EMPTY_PARAMS: ByteArray = "{}".encodeToByteArray()
}

/** Outcome of an rpc declared with an error type (`rpc X {} -> R ! E`). */
public sealed interface RpcResult<out R, out E> {
    public data class Success<out R>(public val value: R) : RpcResult<R, Nothing>
    public data class Failure<out E>(public val error: E) : RpcResult<Nothing, E>
}

/** Thrown by typed client calls when the server answers with an ERROR frame. */
public class AeronRpcException(
    public val method: String,
    public val errorBody: String,
) : RuntimeException("Rpc '$method' failed: $errorBody")
