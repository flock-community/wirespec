package community.flock.wirespec.plugin.utils

actual fun getEnvVar(s: String): String? = System.getenv(s)
