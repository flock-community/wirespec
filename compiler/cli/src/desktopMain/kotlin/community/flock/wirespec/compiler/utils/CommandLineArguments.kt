package community.flock.wirespec.compiler.utils

actual fun Array<String>.orNull(idx: Int): String? = runCatching { this[idx] }.getOrNull()
