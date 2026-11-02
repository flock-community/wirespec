package community.flock.wirespec.converter.common

/**
 * The name and version of the specification a converter emits. Each emitter writes them where its format has room
 * for them, and names its file after [name]. Left out, an emitter uses its own default.
 */
public data class ConverterContext(
    val name: String? = null,
    val version: String? = null,
) {
    public fun fileName(default: String, extension: String): String = "${name ?: default}.$extension"
}
