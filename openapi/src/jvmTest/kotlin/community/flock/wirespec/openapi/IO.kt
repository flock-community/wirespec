package community.flock.wirespec.openapi

actual object IO {
    actual fun readOpenApi(file: String): String {
        val resource = IO::class.java.classLoader.getResource(file)
        return resource.readText()
    }
}