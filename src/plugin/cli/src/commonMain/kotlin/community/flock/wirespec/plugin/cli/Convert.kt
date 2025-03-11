package community.flock.wirespec.plugin.cli

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import community.flock.wirespec.plugin.ConverterArguments
import community.flock.wirespec.plugin.Format.Avro
import community.flock.wirespec.plugin.Format.OpenAPIV2
import community.flock.wirespec.plugin.Format.OpenAPIV3
import community.flock.wirespec.plugin.files.FileName
import community.flock.wirespec.plugin.files.changeDirectory

fun convert(arguments: ConverterArguments) {
    val packageName = arguments.packageName

    val source = arguments.input

    val ast = when (arguments.format) {
        OpenAPIV2 -> OpenAPIV2Parser::parse
        OpenAPIV3 -> OpenAPIV3Parser::parse
        Avro -> AvroParser::parse
    }(arguments.reader(source), arguments.strict)

    return source.changeDirectory(arguments.output).emitters(arguments.languages, packageName)
        .map { (emitter, file) ->
            val results = emitter.emit(ast, Logger(arguments.logLevel))
            when {
                emitter.split -> file to results
                else -> file to nonEmptyListOf(
                    Emitted(
                        source.path.fileName.value.replaceFirstChar(Char::uppercase),
                        results.first().result,
                    ),
                )
            }.let(::WirespecResult)
        }
        .flatMap { (file, results) -> results.map { (name, result) -> file.copy(FileName(name)) to result } }
        .forEach { (file, result) -> arguments.writer(file, result) }
}
