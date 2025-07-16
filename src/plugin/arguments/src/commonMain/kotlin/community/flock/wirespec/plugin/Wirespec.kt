package community.flock.wirespec.plugin

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.converter.avro.AvroParser
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser

fun compile(arguments: CompilerArguments) {
    val ctx = object : CompilationContext {
        override val logger = arguments.logger
        override val emitters = arguments.emitters
    }

    ctx
        .compile(arguments.input.map { ModuleContent(it.name.value, it.content) })
        .fold(arguments)
}

fun convert(arguments: ConverterArguments) {
    val parser: Parser = when (arguments.format) {
        Format.OpenAPIV2 -> OpenAPIV2Parser
        Format.OpenAPIV3 -> OpenAPIV3Parser
        Format.Avro -> AvroParser
    }
    val options = ParseOptions(
        strict = arguments.strict,
    )
    arguments.input
        .map { ModuleContent(it.name.value, it.content) }
        .map { moduleContent -> parser.parse(moduleContent, arguments.strict) }
        .map { Validator.validate(options, it) }
        .let { either { it.bindAll() } }
        .map { list ->
            list.flatMap { ast ->
                arguments.emitters.flatMap {
                    it.emit(ast, arguments.logger)
                }
            }
        }
        .fold(arguments)
}

private fun EitherNel<WirespecException, NonEmptyList<Emitted>>.fold(arguments: WirespecArguments) = this
    .mapLeft { it.map(WirespecException::message) }
    .mapLeft { it.joinToString() }
    .fold(arguments.error, arguments.writer)
