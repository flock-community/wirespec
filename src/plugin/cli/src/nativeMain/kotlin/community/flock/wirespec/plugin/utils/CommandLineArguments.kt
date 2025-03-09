package community.flock.wirespec.plugin.utils

import arrow.core.Either.Companion.catch

actual fun Array<String>.orNull(idx: Int): String? = catch { get(idx) }.getOrNull()
