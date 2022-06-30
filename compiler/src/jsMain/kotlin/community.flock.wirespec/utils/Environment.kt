package community.flock.wirespec.utils

import community.flock.wirespec.js.process

actual fun getEnvVar(s: String) = process.env[s].unsafeCast<String?>()
