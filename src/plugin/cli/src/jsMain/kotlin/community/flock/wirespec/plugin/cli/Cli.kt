@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.nodes.Definition
import community.flock.wirespec.compiler.lib.WsNode
import community.flock.wirespec.compiler.lib.produce

import community.flock.wirespec.compiler.utils.noLogger

@JsExport
fun cli(args: Array<String>) {
    main(args)
}

@JsExport
fun parser(source: String): Array<WsNode> {
    return Wirespec.parse(source)(noLogger).getOrNull()
        ?.filterIsInstance<Definition>()
        ?.map { it.produce() }
        ?.toTypedArray()
        ?: error("Cannot parse source")
}

