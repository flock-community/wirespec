package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type


object AvroConverter {

    private val nullType = AvroModel.SimpleType("null")
    private fun AvroModel.TypeList.isNullable() = contains(nullType)
    private fun AvroModel.SimpleType.toPrimitive() = when (this.value) {
        "boolean" -> Reference.Primitive.Type.Boolean
        "int" -> Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32)
        "long" -> Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64)
        "float" -> Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P32)
        "double" -> Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64)
        "bytes" -> Reference.Primitive.Type.Bytes
        "string" -> Reference.Primitive.Type.String
        else -> TODO("primitive not mapped ${this.value}")
    }

    private fun AvroModel.Type.toReference(isIterable: Boolean = false): Reference = when (this) {
        is AvroModel.SimpleType -> when (value) {
            "null" -> Reference.Unit(
                isIterable = isIterable,
                isDictionary = false
            )

            "boolean", "int", "long", "float", "double", "bytes", "string" -> Reference.Primitive(
                type = toPrimitive(),
                isIterable = isIterable,
                isDictionary = false
            )

            else -> Reference.Custom(value, isIterable, false)
        }

        is AvroModel.ArrayType -> items.toReference(true)
        is AvroModel.RecordType -> Reference.Custom(name, isIterable, false)
        is AvroModel.EnumType -> Reference.Custom(name, isIterable, false)
        is AvroModel.LogicalType -> AvroModel.SimpleType(this.type).toReference()
    }

    private fun AvroModel.TypeList.toReference(): Reference {
        val list = this - nullType
        return when {
            list.size == 1 -> list.first().toReference()
            list.count { it is AvroModel.SimpleType } > 1 -> error("Cannot have multiple SimpleTypes in Union")
            else -> list.first().toReference()
        }
    }

    private fun AvroModel.RecordType.toType() = Type(
        comment = null,
        identifier = DefinitionIdentifier(name),
        extends = emptyList(),
        shape = Type.Shape(this.fields.map {
            Field(
                identifier = FieldIdentifier(it.name),
                reference = it.type.toReference(),
                isNullable = it.type.isNullable()
            )
        })
    )

    private fun AvroModel.EnumType.toEnum() = Enum(
        comment = null,
        identifier = DefinitionIdentifier(name),
        entries = symbols.toSet()
    )

    fun AvroModel.Type.flatten(): AST = when (this) {
        is AvroModel.SimpleType -> emptyList()
        is AvroModel.RecordType -> listOf(toType()) + fields.flatMap { field -> field.type.flatMap { it.flatten() } }
        is AvroModel.ArrayType -> items.flatten()
        is AvroModel.EnumType -> listOf(toEnum())
        is AvroModel.LogicalType -> emptyList()
    }
}
