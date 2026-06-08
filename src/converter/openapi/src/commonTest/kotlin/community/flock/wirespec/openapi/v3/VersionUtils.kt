package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.Version
import kotlin.text.replace

fun Version.isOpenApiVersion() = this != Version.V20

val Version.versionString
    get() = when (this) {
        Version.V20 -> "2.0.0"
        Version.V30 -> "3.0.0"
        Version.V31 -> "3.1.0"
        Version.V32 -> "3.2.0"
    }

fun String.replaceOpenApiVersionInSpec(replacementVersion: Version): String {
    require(replacementVersion.isOpenApiVersion()) { "Expected OpenAPI version, but got $replacementVersion" }
    return replace(
        """"openapi"\s*:\s*"\d.\d.\d"""".toRegex(),
        "\"openapi\": \"${replacementVersion.versionString}\""
    )
}