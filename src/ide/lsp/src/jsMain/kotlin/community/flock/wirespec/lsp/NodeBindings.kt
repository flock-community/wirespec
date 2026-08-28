package community.flock.wirespec.lsp

internal external interface NodeStream {
    fun on(event: String, listener: (Any) -> Unit)
    fun setEncoding(encoding: String)
    fun write(chunk: String): Boolean
}

internal external interface NodeProcess {
    val argv: Array<String>
    val stdin: NodeStream
    val stdout: NodeStream
    val stderr: NodeStream
    fun on(event: String, listener: (Any) -> Unit)
    fun send(message: Any)
}

@Suppress("ClassName")
internal external object Buffer {
    fun byteLength(value: String, encoding: String): Int
}

internal external val process: NodeProcess
