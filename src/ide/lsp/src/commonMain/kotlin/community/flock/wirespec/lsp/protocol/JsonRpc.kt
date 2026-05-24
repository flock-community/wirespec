package community.flock.wirespec.lsp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class JsonRpcMessage(
    val jsonrpc: String = "2.0",
    val id: JsonPrimitive? = null,
    val method: String? = null,
    val params: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
) {
    val isRequest: Boolean get() = id != null && method != null
    val isNotification: Boolean get() = id == null && method != null
    val isResponse: Boolean get() = id != null && method == null
}

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }
}
