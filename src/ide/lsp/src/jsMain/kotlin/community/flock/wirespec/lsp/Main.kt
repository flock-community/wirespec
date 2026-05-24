package community.flock.wirespec.lsp

fun main() {
    val argv = process.argv
    val useNodeIpc = (0 until argv.size).any { argv[it] == "--node-ipc" }
    val transport: Transport = if (useNodeIpc) NodeIpcTransport() else NodeStdioTransport()
    LspServer(transport).start()
}
