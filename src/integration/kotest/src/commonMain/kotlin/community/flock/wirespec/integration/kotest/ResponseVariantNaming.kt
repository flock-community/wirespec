package community.flock.wirespec.integration.kotest

/**
 * Single source of the Wirespec response-variant naming convention: a generated variant class is
 * named `Response<NNN>` for a three-digit status (e.g. `Response200`). Both the DSL emitter (which
 * builds those names) and the runtime (which reflects the status back out of them, and off a
 * user-supplied `ResponseNNN::class`) go through here, so the convention lives in exactly one place.
 */
internal object ResponseVariantNaming {
    private val regex = Regex("Response(\\d{3})")

    /** The variant class simple name for [status], e.g. `"200"` -> `"Response200"`. */
    fun className(status: String): String = "Response$status"

    /** The numeric status encoded in a variant class [simpleName], or `null` if it isn't a `ResponseNNN`. */
    fun statusOf(simpleName: String): Int? = regex.matchEntire(simpleName)?.groupValues?.get(1)?.toInt()
}
