package community.flock.wirespec.lsp

external interface NodeStream {
    fun on(event: String, listener: (Any) -> Unit)
    fun setEncoding(encoding: String)
    fun write(chunk: String): Boolean
}

external interface NodeProcess {
    val argv: Array<String>
    val stdin: NodeStream
    val stdout: NodeStream
    val stderr: NodeStream
    fun on(event: String, listener: (Any) -> Unit)
    fun send(message: Any)
}

@Suppress("ClassName")
external object Buffer {
    fun byteLength(value: String, encoding: String): Int
}

external val process: NodeProcess
