package community.flock.wirespec.compiler.core.parse.nodes

data class Type(val name: String, val shape: Shape) : Definition {
    data class Shape(val value: List<Field>) {
        data class Field(val identifier: Identifier, val reference: Reference, val isNullable: Boolean) {
            data class Identifier(val value: String)
            sealed interface Reference {
                val isIterable: Boolean
                val isMap: Boolean

                data class Any(override val isIterable: Boolean, override val isMap: Boolean = false) : Reference
                data class Custom(
                    val value: String,
                    override val isIterable: Boolean,
                    override val isMap: Boolean = false
                ) : Reference

                data class Primitive(
                    val type: Type,
                    override val isIterable: Boolean,
                    override val isMap: Boolean = false
                ) : Reference {
                    enum class Type { String, Integer, Boolean }
                }
            }
        }
    }
}
