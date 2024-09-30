package community.flock.wirespec.convert.avro

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type

object AvroConverter {

    val nullType = AvroModel.SimpleType("null")
    fun AvroModel.TypeList.isNullable() = contains(nullType)
    fun AvroModel.SimpleType.toPrimitive() = when (this.value) {
        "boolean" -> Reference.Primitive.Type.Boolean
        "int" -> Reference.Primitive.Type.Integer
        "long" -> Reference.Primitive.Type.Integer
        "float" -> Reference.Primitive.Type.Number
        "double" -> Reference.Primitive.Type.Number
        "bytes" -> Reference.Primitive.Type.String
        "string" -> Reference.Primitive.Type.String
        else -> TODO("primitive not mapped ${this.value}")
    }

    fun AvroModel.Type.toReference(isIterable: Boolean = false): Reference = when (this) {
        is AvroModel.SimpleType -> when (value) {
            "null" -> TODO("Map primitive null")
            "boolean", "int", "long", "float", "double", "bytes", "string" -> Reference.Primitive(toPrimitive(), isIterable, false)
            else -> Reference.Custom(value, isIterable, false)
        }

        is AvroModel.ArrayType -> items.toReference(true)
        is AvroModel.RecordType -> Reference.Custom(name, isIterable, false)
        is AvroModel.EnumType -> Reference.Custom(name, isIterable, false)
    }

    fun AvroModel.TypeList.toReference(): Reference {
        val list = minus(nullType)
        if (list.size != 1) {
            TODO("Union types are not supported")
        }
        return list.first().toReference()
    }

    fun AvroModel.RecordType.toType() = Type(
        identifier = Identifier(name),
        shape = Type.Shape(this.fields.map {
            Field(
                identifier = Identifier(it.name),
                reference = it.type.toReference(),
                isNullable = it.type.isNullable()
            )
        }),
        comment = null,
        extends = emptyList()
    )

    fun AvroModel.EnumType.toEnum() = Enum(
        comment = null,
        identifier = Identifier(name),
        entries = symbols.toSet()
    )

    fun AvroModel.Type.flatten(): AST = when (this) {
        is AvroModel.SimpleType -> emptyList()
        is AvroModel.RecordType -> listOf(toType()) + fields.flatMap { it.type.flatMap { it.flatten() } }
        is AvroModel.ArrayType -> items.flatten()
        is AvroModel.EnumType -> listOf(toEnum())
    }
}
