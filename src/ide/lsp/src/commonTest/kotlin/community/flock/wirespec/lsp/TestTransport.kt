package community.flock.wirespec.lsp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * In-memory [Transport] used by tests. Drives the [LspServer] synchronously:
 * calling [request] or [notify] feeds a single JSON-RPC message into the server
 * and lets it run to completion before returning. Every response and
 * server-initiated notification is captured for later inspection.
 */
class TestTransport : Transport {

    private var handler: ((String) -> Unit)? = null
    private val outbox = mutableListOf<JsonElement>()
    private var nextId = 1
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    override fun start(onMessage: (String) -> Unit) {
        handler = onMessage
    }

    override fun send(json: String) {
        outbox += this.json.parseToJsonElement(json)
    }

    /** Send a JSON-RPC notification (no response expected). */
    fun notify(method: String, params: JsonElement? = null) {
        val msg = buildJsonObject {
            put("jsonrpc", "2.0")
            put("method", method)
            if (params != null) put("params", params)
        }
        handler!!.invoke(json.encodeToString(JsonElement.serializer(), msg))
    }

    /**
     * Send a JSON-RPC request and return the matching response, which the server
     * is expected to produce synchronously while the call is on the stack.
     */
    fun request(method: String, params: JsonElement? = null): JsonObject {
        val id = nextId++
        val msg = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            if (params != null) put("params", params)
        }
        val before = outbox.size
        handler!!.invoke(json.encodeToString(JsonElement.serializer(), msg))
        for (i in before until outbox.size) {
            val entry = outbox[i] as? JsonObject ?: continue
            val entryId = entry["id"] as? JsonPrimitive ?: continue
            if (entryId.content == id.toString()) return entry
        }
        throw AssertionError("No response captured for request id=$id method=$method")
    }

    /** Return all server-pushed notifications matching [method], without consuming them. */
    fun notificationsOf(method: String): List<JsonObject> = outbox
        .mapNotNull { it as? JsonObject }
        .filter { it["method"]?.let { v -> v is JsonPrimitive && v.content == method } == true }

    /** Clear the captured outbox. */
    fun reset() {
        outbox.clear()
    }

    @Suppress("unused")
    fun lastNotificationOf(method: String): JsonObject? = notificationsOf(method).lastOrNull()

    @Suppress("unused")
    fun debugDump(): String = outbox.joinToString("\n") { it.toString() }

    companion object {
        fun nullElement(): JsonElement = JsonNull
    }
}
