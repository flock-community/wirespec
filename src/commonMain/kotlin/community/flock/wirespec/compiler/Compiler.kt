package community.flock.wirespec.compiler

import community.flock.wirespec.compiler.Reported.EMITTED
import community.flock.wirespec.compiler.Reported.PARSED
import community.flock.wirespec.compiler.Reported.TOKENIZED
import community.flock.wirespec.compiler.emit.common.Emitter
import community.flock.wirespec.compiler.parse.parse
import community.flock.wirespec.compiler.tokenize.tokenize
import community.flock.wirespec.compiler.utils.log

fun LanguageSpec.compile(source: String): (Emitter) -> String = { emitter ->
    tokenize(source)
        .also { report(it, `as` = TOKENIZED) }
        .parse()
        .also { report(it, `as` = PARSED) }
        .let { emitter.emit(it) }
        .also { report(it, `as` = EMITTED) }
}

private fun report(any: Any, `as`: Reported) = `as`.run {
    log("********** $name **********\n$any\n########## $name ##########")
}

private enum class Reported { TOKENIZED, PARSED, EMITTED }
