package community.flock.wirespec.plugin.cli

import arrow.core.Either.Companion.catch

internal actual fun Array<String>.orNull(idx: Int) = catch { get(idx) }.getOrNull()
