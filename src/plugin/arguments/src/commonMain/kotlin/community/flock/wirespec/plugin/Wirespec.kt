package community.flock.wirespec.plugin

import arrow.core.getOrElse
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.parse.Parser.validate
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser

fun ConverterArguments.parseOptions() = ParseOptions(
    strict = strict,
    allowUnions = true,
)

fun compile(arguments: CompilerArguments) {
    val ctx = object : CompilationContext {
        override val logger = arguments.logger
        override val emitters = arguments.emitters
    }

    ctx.compile(arguments.input.map { ModuleContent(it.name.value, it.content) })
        .mapLeft { it.map(WirespecException::message) }
        .fold({ arguments.error(it.joinToString()) }) { arguments.writer(it) }
}

fun convert(arguments: ConverterArguments) {
    val parser: Parser = when (arguments.format) {
        Format.OpenAPIV2 -> OpenAPIV2Parser
        Format.OpenAPIV3 -> OpenAPIV3Parser
        Format.Avro -> AvroParser
    }
    val options = arguments.parseOptions()
    arguments.input
        .map { ModuleContent(it.name.value, it.content) }
        .map { moduleContent -> parser.parse(moduleContent, arguments.strict) }
        .map { ast ->
            ast.copy(
                modules = ast.modules.map { module ->
                    module.copy(
                        statements = validate(options)(module.statements).getOrElse { error("Invalid statements") },
                    )
                },
            )
        }
        .flatMap { ast ->
            arguments.emitters.flatMap {
                it.emit(ast, arguments.logger)
            }
        }
        .let(arguments.writer)
}
