@file:OptIn(ExperimentalJsExport::class)

package community.flock.wirespec.plugin.npm

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.lib.produce
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.plugin.cli.main

@JsExport
fun cli(args: Array<String>) = main(args)

@JsExport
fun parse(source: String) = WirespecSpec
    .tokenize(source)
    .let { Parser(noLogger).parse(it).produce() }
