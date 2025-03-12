package community.flock.wirespec.plugin.cli

import arrow.core.Either.Companion.catch

// Remove first arguments [command, program]
private val nodeArgs = process.argv.slice(2) as Array<String>

actual fun Array<String>.orNull(idx: Int): String? = catch { nodeArgs[idx] }.getOrNull()
