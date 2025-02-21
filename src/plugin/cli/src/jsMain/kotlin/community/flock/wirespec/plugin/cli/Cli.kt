@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.lib.WsDefinition
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger

@JsExport
fun cli(args: Array<String>) {
    main(args)
}

@JsExport
fun parser(source: String): Array<WsDefinition> = object : ParseContext {
    override val logger = noLogger
}.parse(source).getOrNull()
    ?.filterIsInstance<Definition>()
    ?.map { it.produce() }
    ?.toTypedArray()
    ?: error("Cannot parse source")
