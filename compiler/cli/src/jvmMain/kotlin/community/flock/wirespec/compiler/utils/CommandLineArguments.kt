package community.flock.wirespec.compiler.utils

actual fun Array<String>.orNull(idx: Int) = runCatching { get(idx) }.getOrNull()
