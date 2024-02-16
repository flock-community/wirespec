package community.flock.wirespec.plugin.utils

actual fun Array<String>.orNull(idx: Int) = runCatching { get(idx) }.getOrNull()
