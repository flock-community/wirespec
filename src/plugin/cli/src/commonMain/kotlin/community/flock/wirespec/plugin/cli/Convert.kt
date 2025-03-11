package community.flock.wirespec.plugin.cli

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.FileName
import community.flock.wirespec.plugin.Format.Avro
import community.flock.wirespec.plugin.Format.OpenAPIV2
import community.flock.wirespec.plugin.Format.OpenAPIV3

fun convert(arguments: ConverterArguments) {
    val packageName = arguments.packageName

    val jsonFile = arguments.input

    val ast = when (arguments.format) {
        OpenAPIV2 -> OpenAPIV2Parser::parse
        OpenAPIV3 -> OpenAPIV3Parser::parse
        Avro -> AvroParser::parse
    }(arguments.reader(jsonFile), arguments.strict)

    val path = jsonFile.out(packageName, arguments.output)

    return arguments.languages.emitters(packageName, path)
        .map { (emitter, file) ->
            val results = emitter.emit(ast, Logger(arguments.logLevel))
            when {
                emitter.split -> file to results
                else -> file to nonEmptyListOf(
                    Emitted(
                        jsonFile.path.fileName.value.replaceFirstChar(Char::uppercase),
                        results.first().result,
                    ),
                )
            }.let(::WirespecResult)
        }
        .flatMap { (file, results) -> results.map { (name, result) -> file.copy(FileName(name)) to result } }
        .forEach { (file, result) -> arguments.writer(file, result) }
}
