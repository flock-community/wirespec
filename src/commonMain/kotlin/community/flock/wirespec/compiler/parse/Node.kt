package community.flock.wirespec.compiler.parse

interface Node

sealed class Definition : Node {

    data class TypeDefinition(val name: Name, val shape: Shape) : Definition() {
        data class Name(val value: String)
        data class Shape(val value: Map<Key, Value>) {
            data class Key(val value: String)
            sealed class Value {
                data class Custom(val value: String) : Value()
                data class Ws(val value: Type) : Value() {
                    enum class Type { String, Integer }
                }
            }
        }
    }

}
