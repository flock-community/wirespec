@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.cli

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.lib.WsNode
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.NoLogger

@JsExport
fun cli(args: Array<String>) {
    main(args)
}

@JsExport
fun parser(source: String): Array<WsNode> = object : ParseContext, NoLogger {}
    .parse(nonEmptyListOf(source))
    .getOrElse { error("Cannot parse source: ${it.joinToString { e -> e.message }}") }
    .modules.flatMap(Module::statements).toList()
    .map { it.produce() }
    .toTypedArray()
