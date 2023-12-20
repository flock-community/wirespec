@file:OptIn(ExperimentalJsExport::class)

import community.flock.wirespec.compiler.cli.main

@JsExport
fun cli(args: Array<String>) {
    main(args)
}
