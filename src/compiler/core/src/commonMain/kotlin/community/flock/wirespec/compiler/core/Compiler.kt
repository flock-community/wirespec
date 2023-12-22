package community.flock.wirespec.compiler.core

import arrow.core.Either
import arrow.core.Nel
import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.Reported.EMITTED
import community.flock.wirespec.compiler.core.Reported.PARSED
import community.flock.wirespec.compiler.core.Reported.TOKENIZED
import community.flock.wirespec.compiler.core.Reported.VALIDATED
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.optimize.optimize
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger


fun LanguageSpec.parse(source: String): (Logger) -> Either<NonEmptyList<WirespecException>, AST> =
    { logger ->
        tokenize(source)
            .also((TOKENIZED::report)(logger))
            .optimize()
            .also((VALIDATED::report)(logger))
            .let { Parser(logger).parse(it) }
            .also((PARSED::report)(logger))
            .map { it.validate() }
            .also((VALIDATED::report)(logger))
    }

fun LanguageSpec.compile(source: String): (Logger) -> (Emitter) -> Either<Nel<WirespecException>, List<Emitted>> =
    { logger ->
        { emitter ->
            this.parse(source)(logger)
                .map { emitter.emit(it) }
                .also((EMITTED::report)(logger))
        }
    }

private enum class Reported {
    TOKENIZED, PARSED, VALIDATED, EMITTED;

    fun report(logger: Logger): (Any) -> Unit = {
        logger.log("********** $name **********\n$it\n########## $name ##########")
    }
}
