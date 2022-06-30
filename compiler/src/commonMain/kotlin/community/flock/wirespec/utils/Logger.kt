package community.flock.wirespec.utils

fun log(s: String) = if (enableLogging) println(s) else Unit

fun log(s: String, block: () -> String) = run {
    log(s)
    block()
}

private val enableLogging = getEnvVar("WIRE_SPEC_LOGGING_ENABLED").toBoolean()
