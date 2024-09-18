package community.flock.wirespec.plugin

enum class Format {
    OpenApiV2, OpenApiV3;

    companion object {
        override fun toString() = entries.joinToString()
    }
}
