package community.flock.wirespec.lsp

class NodeIpcTransport : Transport {

    override fun start(onMessage: (String) -> Unit) {
        process.on("message") { message ->
            onMessage(jsJsonStringify(message))
        }
    }

    override fun send(json: String) {
        process.send(jsJsonParse(json))
    }
}

private fun jsJsonStringify(value: Any): String = js("JSON.stringify(value)") as String

private fun jsJsonParse(value: String): Any = js("JSON.parse(value)") as Any
