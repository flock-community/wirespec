package community.flock.wirespec.utils

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getEnvVar(s: String) = getenv(s)?.toKString()
