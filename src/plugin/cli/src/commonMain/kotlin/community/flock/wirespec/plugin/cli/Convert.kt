package community.flock.wirespec.plugin.cli

import arrow.core.right
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format.Avro
import community.flock.wirespec.plugin.Format.OpenAPIV2
import community.flock.wirespec.plugin.Format.OpenAPIV3
import community.flock.wirespec.plugin.cli.io.JsonFile

fun convert(arguments: ConverterArguments): WirespecResults {
    val packageName = arguments.packageName

    val fullPath = arguments.input
    val jsonString = JsonFile(fullPath).read()
    val strict = arguments.strict

    val ast = when (arguments.format) {
        OpenAPIV2 -> OpenAPIV2Parser.parse(jsonString, !strict)
        OpenAPIV3 -> OpenAPIV3Parser.parse(jsonString, !strict)
        Avro -> AvroParser.parse(jsonString, strict)
    }

    val path = fullPath.out(packageName, arguments.output)

    return arguments.languages.emitters(packageName, path, Logger(arguments.logLevel)).map { (emitter, file) ->
        val results = emitter.emit(ast)
        if (!emitter.split) {
            listOf(
                Emitted(
                    fullPath.fileName.value.replaceFirstChar(Char::uppercase),
                    results.first().result,
                ),
            ) to file
        } else {
            results to file
        }
    }.map { it.right() }
}
