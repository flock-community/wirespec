package community.flock.wirespec.plugin

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.either
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.withoutDefaults
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.converter.avro.AvroJsonParser
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser

public fun compile(arguments: CompilerArguments) {
    val ctx = object : CompilationContext {
        override val logger = arguments.logger
        override val emitters = arguments.emitters
    }

    ctx
        .compile(
            source = arguments.input.map { ModuleContent(FileUri(it.name.value), it.content) },
            ignoreDefaults = arguments.ignoreDefaults,
        )
        .fold(arguments)
}

public fun convert(arguments: ConverterArguments) {
    val parser: Parser = when (arguments.format) {
        Format.OpenAPIV2 -> OpenAPIV2Parser
        Format.OpenAPIV3 -> OpenAPIV3Parser
        Format.Avro -> AvroJsonParser
    }
    val options = ParseOptions(
        strict = arguments.strict,
    )
    arguments.input
        .map { ModuleContent(FileUri(it.name.value), it.content) }
        .map { moduleContent -> parser.parse(moduleContent, arguments.strict) }
        .map { Validator.validate(options, it) }
        .let { either { it.bindAll() } }
        // Unlike compile, convert does not fail on defaults a language cannot generate: the defaults come from the
        // converted spec, and those languages leave them out.
        .map { list -> list.map { if (arguments.ignoreDefaults) it.withoutDefaults() else it } }
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
