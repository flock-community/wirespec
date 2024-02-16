package community.flock.wirespec.plugin.utils

actual fun Array<String>.orNull(idx: Int): String? = runCatching { get(idx) }.getOrNull()
