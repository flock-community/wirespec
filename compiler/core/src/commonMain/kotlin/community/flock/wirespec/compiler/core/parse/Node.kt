package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

data class Type(val name: Name, val shape: Shape) : Definition {
    data class Name(val value: String)
    data class Shape(val value: List<Field>) {
        data class Field(val key: Key, val value: Value, val isNullable: Boolean) {
            data class Key(val value: String)
            sealed class Value(val isIterable: Boolean) {
                class Custom(val value: String, isIterable: Boolean) : Value(isIterable)
                class Ws(val value: Type, isIterable: Boolean) : Value(isIterable) {
                    enum class Type { String, Integer, Boolean }
                }
            }
        }
    }
}

data class Refined(val name: Name, val validator: Validator) : Definition {
    data class Name(val value: String)
    data class Validator(val value: String)
}
