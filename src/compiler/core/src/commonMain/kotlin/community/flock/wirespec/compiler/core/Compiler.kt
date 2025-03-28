package community.flock.wirespec.compiler.core

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Stage.EMITTED
import community.flock.wirespec.compiler.core.Stage.PARSED
import community.flock.wirespec.compiler.core.Stage.TOKENIZED
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.HasEmitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.tokenize.TokenizedModule
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.HasLogger

interface TokenizeContext :
    HasLanguageSpec,
    HasLogger

interface ParseContext :
    TokenizeContext,
    HasLogger

interface EmitContext :
    ParseContext,
    HasEmitter,
    HasLogger

interface CompilationContext :
    TokenizeContext,
    ParseContext,
    EmitContext

fun TokenizeContext.tokenize(source: String): TokenizedModule = spec
    .tokenize(source)
    .also(TOKENIZED::log)

fun ParseContext.parse(source: NonEmptyList<String>): EitherNel<WirespecException, AST> = parse(source.map { tokenize(it) }).also(PARSED::log)

fun EmitContext.emit(source: NonEmptyList<String>): EitherNel<WirespecException, NonEmptyList<Emitted>> = parse(source)
    .map { emitter.emit(it, logger) }
    .also(EMITTED::log)

fun CompilationContext.compile(source: NonEmptyList<String>): EitherNel<WirespecException, NonEmptyList<Emitted>> = emit(source)

private enum class Stage {
    TOKENIZED,
    PARSED,
    EMITTED,
    ;

    fun log(it: Any): HasLogger.() -> Unit = {
        logger.debug("********** $name **********\n$it\n########## $name ##########")
    }
}
