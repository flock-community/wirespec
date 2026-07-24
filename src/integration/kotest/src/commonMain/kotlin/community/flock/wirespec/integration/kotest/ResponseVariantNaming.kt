package community.flock.wirespec.integration.kotest

/** Single source of the Wirespec response-variant naming convention (`Response<NNN>`, e.g. `Response200`). */
internal object ResponseVariantNaming {
    private val regex = Regex("Response(\\d{3})")

    /** The variant class simple name for [status], e.g. `"200"` -> `"Response200"`. */
    fun className(status: String): String = "Response$status"

    /** The numeric status encoded in a variant class [simpleName], or `null` if it isn't a `ResponseNNN`. */
    fun statusOf(simpleName: String): Int? = regex.matchEntire(simpleName)?.groupValues?.get(1)?.toInt()
}
