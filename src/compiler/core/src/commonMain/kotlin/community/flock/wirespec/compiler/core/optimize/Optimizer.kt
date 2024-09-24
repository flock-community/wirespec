package community.flock.wirespec.compiler.core.optimize

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.tokenize.Token

fun NonEmptyList<Token>.optimize(): NonEmptyList<Token> = this
