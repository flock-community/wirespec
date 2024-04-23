package community.flock.wirespec.plugin.cli.js


external fun require(name: String): dynamic

val fs = require("node:fs")


external interface Process {
    val env: dynamic
    val argv: dynamic
}

external val process: Process
