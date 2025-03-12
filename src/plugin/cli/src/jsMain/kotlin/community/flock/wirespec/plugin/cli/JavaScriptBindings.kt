package community.flock.wirespec.plugin.cli

external interface Process {
    val env: dynamic
    val argv: dynamic
}

external val process: Process
