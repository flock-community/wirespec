package community.flock.wirespec.integration.aeron

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A single Wirespec RPC message as it travels through Aeron.
 *
 * Wire format, version 1, all integers little-endian:
 *
 * ```
 * u8   protocol version (1)
 * u8   frame kind (1 = REQUEST, 2 = RESULT, 3 = ERROR)
 * i64  correlation id
 * u16  method length, followed by that many bytes of UTF-8 method name
 * -- REQUEST only --
 * u16  reply channel length, followed by that many bytes of UTF-8 Aeron channel URI
 * i32  reply stream id
 * ------------------
 * u32  payload length, followed by that many bytes of payload
 * ```
 *
 * The payload follows the Wirespec body serialization contract: JSON, with plain
 * string bodies as raw UTF-8 text. A REQUEST payload is a JSON object keyed by
 * the rpc's parameter names; RESULT and ERROR payloads hold the rpc's result and
 * error values respectively.
 */
public sealed interface RpcFrame {
    public val correlationId: Long
    public val method: String
    public val payload: ByteArray

    public class Request(
        override val correlationId: Long,
        override val method: String,
        public val replyChannel: String,
        public val replyStreamId: Int,
        override val payload: ByteArray,
    ) : RpcFrame

    public class Result(
        override val correlationId: Long,
        override val method: String,
        override val payload: ByteArray,
    ) : RpcFrame

    public class Error(
        override val correlationId: Long,
        override val method: String,
        override val payload: ByteArray,
    ) : RpcFrame

    public fun encode(): ByteArray {
        val method = method.encodeToByteArray()
        val replyChannel = (this as? Request)?.replyChannel?.encodeToByteArray()
        val size = HEADER_SIZE + method.size + (replyChannel?.let { Short.SIZE_BYTES + it.size + Int.SIZE_BYTES } ?: 0) + Int.SIZE_BYTES + payload.size
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(VERSION)
            put(
                when (this@RpcFrame) {
                    is Request -> KIND_REQUEST
                    is Result -> KIND_RESULT
                    is Error -> KIND_ERROR
                },
            )
            putLong(correlationId)
            putShort(method.size.toShort())
            put(method)
            replyChannel?.let {
                putShort(it.size.toShort())
                put(it)
                putInt((this@RpcFrame as Request).replyStreamId)
            }
            putInt(payload.size)
            put(payload)
        }.array()
    }

    public companion object {
        public const val VERSION: Byte = 1
        public const val KIND_REQUEST: Byte = 1
        public const val KIND_RESULT: Byte = 2
        public const val KIND_ERROR: Byte = 3
        private const val HEADER_SIZE = Byte.SIZE_BYTES + Byte.SIZE_BYTES + Long.SIZE_BYTES + Short.SIZE_BYTES

        public fun decode(bytes: ByteArray): RpcFrame = with(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)) {
            val version = get()
            require(version == VERSION) { "Unsupported Wirespec Aeron protocol version: $version" }
            val kind = get()
            val correlationId = long
            val method = utf8()
            when (kind) {
                KIND_REQUEST -> Request(correlationId, method, replyChannel = utf8(), replyStreamId = int, payload = payload())
                KIND_RESULT -> Result(correlationId, method, payload())
                KIND_ERROR -> Error(correlationId, method, payload())
                else -> throw IllegalArgumentException("Unknown Wirespec Aeron frame kind: $kind")
            }
        }

        private fun ByteBuffer.utf8(): String = ByteArray(short.toInt() and 0xFFFF).also(::get).decodeToString()

        private fun ByteBuffer.payload(): ByteArray = ByteArray(int).also(::get)
    }
}
