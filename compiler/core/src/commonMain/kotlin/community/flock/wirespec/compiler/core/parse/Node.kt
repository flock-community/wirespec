package community.flock.wirespec.compiler.core.parse

sealed interface Node

sealed interface Definition : Node
sealed interface Segment : Node

data class Type(val name: Name, val shape: Shape) : Definition {
    data class Name(val value: String)

}

data class Endpoint(val verb: Verb, val path: Iterable<Segment>, val query: Shape?, val lambda: Lambda) : Definition {
    data class Verb(val value: String)
    data class PathSegment(val value: String): Segment
    data class Lambda(val output: String, val input: String?)
}

data class Shape(val value: List<Field>): Definition, Segment {
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