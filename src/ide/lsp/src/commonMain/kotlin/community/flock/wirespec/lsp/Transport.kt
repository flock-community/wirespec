package community.flock.wirespec.lsp

internal interface Transport {
    fun start(onMessage: (String) -> Unit)
    fun send(json: String)
}
