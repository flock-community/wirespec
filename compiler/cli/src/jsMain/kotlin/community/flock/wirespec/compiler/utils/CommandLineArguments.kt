package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.cli.js.process

// Remove first arguments [command, program]
private val nodeArgs = process.argv.slice(2) as Array<String>

actual fun Array<String>.orNull(idx: Int): String? = runCatching { nodeArgs[idx] }.getOrNull()
