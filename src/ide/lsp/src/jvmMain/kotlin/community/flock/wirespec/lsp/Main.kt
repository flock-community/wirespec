@file:JvmName("WirespecLsp")

package community.flock.wirespec.lsp

fun main() {
    LspServer(JvmStdioTransport()).start()
}
