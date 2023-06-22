package community.flock.wirespec.openapi

expect object IO {
    fun readOpenApi(file: String): String
}