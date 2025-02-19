package community.flock.wirespec.plugin

enum class Format {
    OpenApiV2,
    OpenApiV3,
    Avro,
    ;

    companion object {
        override fun toString() = entries.joinToString()
    }
}
