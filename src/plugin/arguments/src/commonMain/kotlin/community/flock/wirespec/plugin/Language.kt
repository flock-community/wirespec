package community.flock.wirespec.plugin

import community.flock.wirespec.compiler.core.Value

enum class Language {
    Java,
    Kotlin,
    Scala,
    TypeScript,
    Wirespec,
    OpenAPIV2,
    OpenAPIV3,
    ;

    companion object {
        fun toMap() = entries.associateBy { it.name }
        override fun toString() = entries.joinToString()
    }
}

enum class FileExtension(override val value: String) : Value<String> {
    Java("java"),
    Kotlin("kt"),
    Scala("scala"),
    TypeScript("ts"),
    Wirespec("ws"),
    Json("json"),
}
