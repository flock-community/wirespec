package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Value

enum class FileExtension(override val value: String) : Value<String> {
    Java("java"),
    Kotlin("kt"),
    TypeScript("ts"),
    Python("py"),
    Wirespec("ws"),
    JSON("json"),
    Avro("avsc"),
}
