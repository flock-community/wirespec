package community.flock.wirespec.compiler.core.parse.nodes

sealed interface Node

sealed interface Definition : Node

data class Enum(val name: String, val entries: Set<String>) : Definition

data class Refined(val name: String, val validator: Validator) : Definition {
    data class Validator(val value: String)
}
