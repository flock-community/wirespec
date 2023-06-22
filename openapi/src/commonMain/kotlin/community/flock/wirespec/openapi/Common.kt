package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.v3.Type

object Common {
    fun className(vararg arg: String) = arg.joinToString("") { it.replaceFirstChar { it.uppercase() } }
}

