package community.flock.wirespec.compiler.core.optimize

import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.WhiteSpace

fun List<Token>.optimize() = filterNot { it.type is WhiteSpace }
