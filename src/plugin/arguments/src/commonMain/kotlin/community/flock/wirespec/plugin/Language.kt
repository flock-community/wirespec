package community.flock.wirespec.plugin

enum class Language {
    Java,
    Kotlin,
    Scala,
    TypeScript,
    Python,
    Wirespec,
    OpenAPIV2,
    OpenAPIV3,
    ;

    companion object {
        fun toMap() = entries.associateBy { it.name }
        override fun toString() = entries.joinToString()
    }
}
