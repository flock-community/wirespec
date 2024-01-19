package community.flock.wirespec.convert.avro

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Enum

object AvroConverter {

    val nullType = AvroModel.SimpleType("null")
    fun AvroModel.TypeList.isNullable() = contains(nullType)
    fun AvroModel.SimpleType.toPrimitive() = when (this.value) {
        "boolean" -> Type.Shape.Field.Reference.Primitive.Type.Boolean
        "int" -> Type.Shape.Field.Reference.Primitive.Type.Integer
        "long" -> Type.Shape.Field.Reference.Primitive.Type.Integer
        "float" -> Type.Shape.Field.Reference.Primitive.Type.Number
        "double" -> Type.Shape.Field.Reference.Primitive.Type.Number
        "bytes" -> Type.Shape.Field.Reference.Primitive.Type.String
        "string" -> Type.Shape.Field.Reference.Primitive.Type.String
        else -> TODO("primitive not mapped ${this.value}")
    }

    fun AvroModel.Type.toReference(isIterable: Boolean = false): Type.Shape.Field.Reference = when (this) {
        is AvroModel.SimpleType -> when (value) {
            "null" -> TODO("Map primitive null")
            "boolean", "int", "long", "float", "double", "bytes", "string" -> Type.Shape.Field.Reference.Primitive(toPrimitive(), isIterable, false)
            else -> Type.Shape.Field.Reference.Custom(value, isIterable, false)
        }

        is AvroModel.ArrayType -> items.toReference(true)
        is AvroModel.RecordType -> Type.Shape.Field.Reference.Custom(name, isIterable, false)
        is AvroModel.EnumType -> Type.Shape.Field.Reference.Custom(name, isIterable, false)
    }

    fun AvroModel.TypeList.toReference(): Type.Shape.Field.Reference {
        val list = minus(nullType)
        if (list.size != 1) {
            TODO("Union types are not supported")
        }
        return list.first().toReference()
    }

    fun AvroModel.RecordType.toType() = Type(
        name = name,
        shape = Type.Shape(this.fields.map {
            Type.Shape.Field(
                identifier = Type.Shape.Field.Identifier(it.name),
                reference = it.type.toReference(),
                isNullable = it.type.isNullable()
            )
        })
    )

    fun AvroModel.EnumType.toEnum() = Enum(
        name = name,
        entries = symbols.toSet()
    )

    fun AvroModel.Type.flatten(): AST = when (this) {
        is AvroModel.SimpleType -> emptyList()
        is AvroModel.RecordType -> listOf(toType()) + fields.flatMap { it.type.flatMap { it.flatten() } }
        is AvroModel.ArrayType -> items.flatten()
        is AvroModel.EnumType -> listOf(toEnum())
    }
}