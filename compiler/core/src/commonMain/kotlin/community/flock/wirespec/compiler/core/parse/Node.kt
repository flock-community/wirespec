package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node

data class Type(val name: Name, val shape: Shape) : Definition {
    data class Name(val value: String)
    data class Shape(val value: List<Field>) {
        data class Field(val key: Key, val value: Value, val nullable: Boolean) {
            data class Key(val value: String)
            sealed class Value(val iterable: Boolean) {
                class Custom(val value: String, iterable: Boolean) : Value(iterable)
                class Ws(val value: Type, iterable: Boolean) : Value(iterable) {
                    enum class Type { String, Integer, Boolean }
                }
            }
        }
    }
}
