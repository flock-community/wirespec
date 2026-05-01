package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.Value

enum class FileExtension(override val value: String) : Value<String> {
    Java("java"),
    Kotlin("kt"),
    TypeScript("ts"),
    Python("py"),
    Rust("rs"),
    Scala("scala"),
    Wirespec("ws"),
    JSON("json"),
    YAML("yaml"),
    AvroJson("avsc"),
    AvroIdl("avdl"),
}
