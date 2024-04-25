package community.flock.wirespec.plugin.cli.io

actual class Server actual constructor(handle: (req:Request) -> Response) {
    actual fun start(port: Int) {
            error("Not implemented yet")
    }
}