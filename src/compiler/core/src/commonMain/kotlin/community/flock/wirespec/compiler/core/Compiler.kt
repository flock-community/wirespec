package community.flock.wirespec.compiler.core

import arrow.core.Either
import arrow.core.Nel
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Stage.EMITTED
import community.flock.wirespec.compiler.core.Stage.PARSED
import community.flock.wirespec.compiler.core.Stage.TOKENIZED
import community.flock.wirespec.compiler.core.Stage.VALIDATED
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.HasEmitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.HasLogger

interface CompilationContext : ParseContext, HasEmitter

fun CompilationContext.compile(source: String): Either<Nel<WirespecException>, List<Emitted>> =
    parse(source)
        .map(emitter::emit)
        .also(EMITTED::log)

interface ParseContext : HasLanguageSpec, HasLogger

fun ParseContext.parse(source: String): Either<NonEmptyList<WirespecException>, AST> =
    spec.tokenize(source)
        .also(TOKENIZED::log)
        .let(Parser(logger)::parse)
        .also(PARSED::log)
        .map { it.validate() }
        .also(VALIDATED::log)

private enum class Stage {
    TOKENIZED, PARSED, VALIDATED, EMITTED;

    fun log(it: Any): HasLogger.() -> Unit = {
        logger.info("********** $name **********\n$it\n########## $name ##########")
    }
}
