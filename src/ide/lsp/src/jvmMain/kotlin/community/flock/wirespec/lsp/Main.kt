@file:JvmName("WirespecLsp")

package community.flock.wirespec.lsp

public fun main() {
    LspServer(JvmStdioTransport()).start()
}
