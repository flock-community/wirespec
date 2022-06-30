package community.flock.wirespec.utils

import community.flock.wirespec.js.process

private val nodeArgs = process.argv.slice(2) as Array<String>

actual fun getFirst(args: Array<String>): String? = runCatching { nodeArgs[0] }.getOrNull()

actual fun getSecond(args: Array<String>): String? = runCatching { nodeArgs[1] }.getOrNull()
