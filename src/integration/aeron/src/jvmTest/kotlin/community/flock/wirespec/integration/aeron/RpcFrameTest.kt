package community.flock.wirespec.integration.aeron

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RpcFrameTest {

    // These hex vectors are shared verbatim with the Rust client example
    // (examples/rust-aeron); a change here is a wire protocol change.
    private val goldenRequest =
        "01012a00000000000000080047657451756f746509006165726f6e3a697063ea030000110000007b2273796d626f6c223a224141504c227d"
    private val goldenResult =
        "01022a00000000000000080047657451756f74652e0000007b2273796d626f6c223a224141504c222c227072696365223a312e352c2263757272656e6379223a22555344227d"
    private val goldenError =
        "01032a00000000000000080047657451756f74652a0000007b22636f6465223a22554e4b4e4f574e5f53594d424f4c222c226d657373616765223a226e6f7065227d"

    @Test
    fun `request encodes to the golden wire format`() = assertEquals(
        goldenRequest,
        RpcFrame.Request(42, "GetQuote", "aeron:ipc", 1002, """{"symbol":"AAPL"}""".encodeToByteArray()).encode().toHex(),
    )

    @Test
    fun `result encodes to the golden wire format`() = assertEquals(
        goldenResult,
        RpcFrame.Result(42, "GetQuote", """{"symbol":"AAPL","price":1.5,"currency":"USD"}""".encodeToByteArray()).encode().toHex(),
    )

    @Test
    fun `error encodes to the golden wire format`() = assertEquals(
        goldenError,
        RpcFrame.Error(42, "GetQuote", """{"code":"UNKNOWN_SYMBOL","message":"nope"}""".encodeToByteArray()).encode().toHex(),
    )

    @Test
    fun `request round-trips`() {
        val frame = assertIs<RpcFrame.Request>(RpcFrame.decode(goldenRequest.fromHex()))
        assertEquals(42, frame.correlationId)
        assertEquals("GetQuote", frame.method)
        assertEquals("aeron:ipc", frame.replyChannel)
        assertEquals(1002, frame.replyStreamId)
        assertContentEquals("""{"symbol":"AAPL"}""".encodeToByteArray(), frame.payload)
    }

    @Test
    fun `result round-trips`() {
        val frame = assertIs<RpcFrame.Result>(RpcFrame.decode(goldenResult.fromHex()))
        assertEquals(42, frame.correlationId)
        assertEquals("GetQuote", frame.method)
        assertContentEquals("""{"symbol":"AAPL","price":1.5,"currency":"USD"}""".encodeToByteArray(), frame.payload)
    }

    @Test
    fun `error round-trips`() {
        val frame = assertIs<RpcFrame.Error>(RpcFrame.decode(goldenError.fromHex()))
        assertEquals(42, frame.correlationId)
        assertContentEquals("""{"code":"UNKNOWN_SYMBOL","message":"nope"}""".encodeToByteArray(), frame.payload)
    }

    @Test
    fun `unknown version is rejected`() {
        assertFailsWith<IllegalArgumentException> { RpcFrame.decode(byteArrayOf(2, 1)) }
    }

    @Test
    fun `unknown kind is rejected`() {
        assertFailsWith<IllegalArgumentException> { RpcFrame.decode(byteArrayOf(1, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)) }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
