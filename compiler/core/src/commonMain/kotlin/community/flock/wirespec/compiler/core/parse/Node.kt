package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node
sealed interface Segment

data class Type(val name: Name, val shape: Shape) : Definition {
    data class Name(val value: String)

}

data class Endpoint(
    val name: Name,
    val verb: Verb,
    val path: Iterable<Segment>,
    val query: Shape?,
    val lambda: Lambda
) : Definition {
    data class Name(val value: String)
    data class Verb(val value: String)
    data class PathSegment(val value: String) : Segment
    data class Lambda(val output: Type, val input: Type?) {
        data class Type(val name: String, val isIterable: Boolean, val isNullable: Boolean)
    }
}

data class Shape(val value: List<Field>) : Segment {
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