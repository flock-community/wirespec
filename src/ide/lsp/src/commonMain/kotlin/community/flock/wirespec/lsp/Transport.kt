package community.flock.wirespec.lsp

interface Transport {
    fun start(onMessage: (String) -> Unit)
    fun send(json: String)
}
