package community.flock.wirespec.plugin

enum class Format {
    OpenAPIV2,
    OpenAPIV3,
    Avro,
    GraphQL,
    ;

    companion object {
        override fun toString() = entries.joinToString()
    }
}
