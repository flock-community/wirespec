package community.flock.wirespec.plugin.utils

import community.flock.wirespec.plugin.cli.js.process

actual fun getEnvVar(s: String) = process.env[s].unsafeCast<String?>()
