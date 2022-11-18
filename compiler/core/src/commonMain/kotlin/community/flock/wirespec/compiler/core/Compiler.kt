package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.Reported.EMITTED
import community.flock.wirespec.compiler.core.Reported.PARSED
import community.flock.wirespec.compiler.core.Reported.TOKENIZED
import community.flock.wirespec.compiler.core.Reported.VALIDATED
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WireSpecException.CompilerException
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.core.validate.validate
import community.flock.wirespec.compiler.utils.Logger

fun LanguageSpec.compile(source: String): (Logger) -> (Emitter) -> Either<CompilerException, List<Pair<String, String>>> = {
    { emitter ->
        tokenize(source)
            .also((TOKENIZED::report)(it))
            .let(Parser(it)::parse)
            .also((PARSED::report)(it))
            .flatMap { it.validate() }
            .also((VALIDATED::report)(it))
            .flatMap(emitter::emit)
            .also((EMITTED::report)(it))
    }
}

private enum class Reported {
    TOKENIZED, PARSED, VALIDATED, EMITTED;

    fun report(logger: Logger): (Any) -> Unit = {
        logger.log("********** $name **********\n$it\n########## $name ##########")
    }
}
