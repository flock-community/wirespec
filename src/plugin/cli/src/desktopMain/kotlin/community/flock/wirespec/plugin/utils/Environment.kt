@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package community.flock.wirespec.plugin.utils

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getEnvVar(s: String) = getenv(s)?.toKString()
