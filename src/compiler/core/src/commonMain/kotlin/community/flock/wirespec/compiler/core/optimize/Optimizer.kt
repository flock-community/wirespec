package community.flock.wirespec.compiler.core.optimize

import community.flock.wirespec.compiler.core.tokenize.CustomType
import community.flock.wirespec.compiler.core.tokenize.SpecificType
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.Tokens

fun Tokens.optimize(specificTypes: Map<String, SpecificType>): Tokens = map { it.specify(specificTypes) }

private fun Token.specify(entries: Map<String, SpecificType>) = when (type) {
    is CustomType -> entries[value]
        ?.let { copy(type = it) }
        ?: this

    else -> this
}
