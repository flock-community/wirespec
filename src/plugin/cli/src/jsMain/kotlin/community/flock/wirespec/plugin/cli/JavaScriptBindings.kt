package community.flock.wirespec.plugin.cli

internal external interface Process {
    val env: dynamic
    val argv: dynamic
}

internal external val process: Process
