package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.cli.js.process

private val nodeArgs = process.argv.slice(2) as Array<String>

actual fun Array<String>.orNull(idx: Int): String? = kotlin.runCatching { nodeArgs[idx] }.getOrNull()
