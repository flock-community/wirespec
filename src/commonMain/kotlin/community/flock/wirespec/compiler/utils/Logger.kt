package community.flock.wirespec.compiler.utils

import kotlinx.cinterop.toKString
import platform.posix.getenv

fun log(s: String) = if (enableLogging) println(s) else Unit

fun log(s: String, block: () -> String) = run {
    log(s)
    block()
}

private val enableLogging = getenv("WIRE_SPEC_LOGGING_ENABLED")?.toKString().toBoolean()
