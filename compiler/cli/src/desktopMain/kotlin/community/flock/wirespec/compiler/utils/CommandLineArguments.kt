package community.flock.wirespec.compiler.utils

actual fun getFirst(args: Array<String>): String? = runCatching { args[0] }.getOrNull()

actual fun getSecond(args: Array<String>): String? = runCatching { args[1] }.getOrNull()
