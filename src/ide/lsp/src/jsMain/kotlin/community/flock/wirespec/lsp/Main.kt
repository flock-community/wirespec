@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.lsp

@JsExport
fun startLsp(useNodeIpc: Boolean) {
    val transport: Transport = if (useNodeIpc) NodeIpcTransport() else NodeStdioTransport()
    LspServer(transport).start()
}
