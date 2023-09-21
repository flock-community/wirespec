package community.flock.wirespec.compiler.core.optimize

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex

fun NonEmptyList<Token>.optimize() = map {
    when (it.type) {
        is CustomRegex -> it.copy(value = """"${it.value.drop(1).dropLast(2)}"""")
        else -> it
    }
}
