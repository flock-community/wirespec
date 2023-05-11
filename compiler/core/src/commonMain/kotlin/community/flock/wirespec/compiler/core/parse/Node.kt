package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

data class Type(val name: String, val shape: Shape) : Definition {
    data class Shape(val value: List<Field>) {
        data class Field(val identifier: Identifier, val reference: Reference, val isNullable: Boolean) {
            data class Identifier(val value: String)
            sealed class Reference(val isIterable: Boolean) {
                class Custom(val value: String, isIterable: Boolean) : Reference(isIterable)
                class Primitive(val type: Type, isIterable: Boolean) : Reference(isIterable) {
                    enum class Type { String, Integer, Boolean }
                }
            }
        }
    }
}

data class Refined(val name: String, val validator: Validator) : Definition {
    data class Validator(val value: String)
}
