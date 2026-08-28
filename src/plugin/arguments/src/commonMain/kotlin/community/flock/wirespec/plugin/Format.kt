package community.flock.wirespec.plugin

public enum class Format {
    OpenAPIV2,
    OpenAPIV3,
    Avro,
    ;

    public companion object {
        override fun toString(): String = entries.joinToString()
    }
}
