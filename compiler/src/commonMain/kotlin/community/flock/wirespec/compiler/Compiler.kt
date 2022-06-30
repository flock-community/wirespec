package community.flock.wirespec.compiler

import community.flock.wirespec.compiler.Reported.EMITTED
import community.flock.wirespec.compiler.Reported.PARSED
import community.flock.wirespec.compiler.Reported.TOKENIZED
import community.flock.wirespec.compiler.emit.common.Emitter
import community.flock.wirespec.compiler.parse.parse
import community.flock.wirespec.compiler.tokenize.tokenize
import community.flock.wirespec.utils.log

fun LanguageSpec.compile(source: String): (Emitter) -> String = { emitter ->
    tokenize(source)
        .also(TOKENIZED::report)
        .parse()
        .also(PARSED::report)
        .let(emitter::emit)
        .also(EMITTED::report)
}


private enum class Reported {
    TOKENIZED, PARSED, EMITTED;

    fun report(any: Any) = run {
        log("********** $name **********\n$any\n########## $name ##########")
    }
}
