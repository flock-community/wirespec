package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.tokenize.types.WsType

sealed interface Node

sealed interface Definition : Node

data class Type(val name: Name, val shape: Shape) : Definition {
    data class Name(val value: String)
    data class Shape(val value: Map<Key, Value>) {
        data class Key(val value: String, val iterable: Boolean = false, val nullable: Boolean = false)
        sealed class Value(val iterable: Boolean, val nullable: Boolean) {
            class Custom(val value: String, type: WsType) : Value(iterable = type.iterable, nullable = type.nullable)
            class Ws(val value: Type, type: WsType) : Value(iterable = type.iterable, nullable = type.nullable) {
                enum class Type { String, Integer, Boolean }
            }
        }
    }
}
