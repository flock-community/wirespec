package community.flock.wirespec.openapi

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper

object Common {
    fun className(vararg arg: String) = arg
        .flatMap { it.split("-", "/") }
        .joinToString("") { it.firstToUpper() }
}
