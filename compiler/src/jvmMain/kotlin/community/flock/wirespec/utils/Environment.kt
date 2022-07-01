package community.flock.wirespec.utils

import java.lang.System

actual fun getEnvVar(s: String): String? = System.getenv(s)
