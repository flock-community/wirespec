package community.flock.wirespec.compiler.utils

import community.flock.wirespec.compiler.cli.js.process

actual fun getEnvVar(s: String) = process.env[s].unsafeCast<String?>()
