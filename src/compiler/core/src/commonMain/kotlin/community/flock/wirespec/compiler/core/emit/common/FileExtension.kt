package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.Value

enum class FileExtension(override val value: String) : Value<String> {
    Java("java"),
    Kotlin("kt"),
    Scala("scala"),
    TypeScript("ts"),
    Python("py"),
    Wirespec("ws"),
    JSON("json"),
    Avro("avsc"),
}
