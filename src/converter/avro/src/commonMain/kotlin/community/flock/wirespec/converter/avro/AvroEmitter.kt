package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

object AvroEmitter {

    fun Enum.emit(): AvroModel.EnumType =  AvroModel.EnumType(
        type = "enum",
        name = identifier.value,
        symbols = entries.toList()
    )

    fun Reference.emit(ast:AST, hasEmitted: MutableList<String>): AvroModel.Type = when (val ref = this) {
        is Reference.Primitive -> {
            when(val type = ref.type){
                is Reference.Primitive.Type.String-> AvroModel.SimpleType("string")
                is Reference.Primitive.Type.Integer -> when(type.precision){
                    Reference.Primitive.Type.Precision.P32 -> AvroModel.SimpleType("int")
                    Reference.Primitive.Type.Precision.P64 -> AvroModel.SimpleType("long")
                }
                is Reference.Primitive.Type.Number -> when(type.precision){
                    Reference.Primitive.Type.Precision.P32 -> AvroModel.SimpleType("float")
                    Reference.Primitive.Type.Precision.P64 -> AvroModel.SimpleType("double")
                }
                is Reference.Primitive.Type.Boolean -> AvroModel.SimpleType("boolean")
                is Reference.Primitive.Type.Bytes -> AvroModel.SimpleType("bytes")
            }
        }
        is Reference.Custom -> {
            when (val def = ast.findType(ref.value)) {
                is Type -> {
                    if (hasEmitted.contains(def.identifier.value)) {
                        def.identifier.value
                            .let { AvroModel.SimpleType(it) }
                    } else {
                        def.also { hasEmitted.add(def.identifier.value) }
                            .emit(ast, hasEmitted)
                    }
                }

                is Enum -> def
                    .emit()

                is Channel -> TODO()
                is Endpoint -> TODO()
                is Refined -> TODO()
                is Union -> TODO()
                null -> AvroModel.SimpleType(ref.value)
            }
        }

        is Reference.Any -> TODO()
        is Reference.Unit -> TODO()
    }

    fun Field.emit(ast: AST, hasEmitted: MutableList<String>) =
        if(reference.isIterable){
            AvroModel.ArrayType(
                type = "array",
                items = reference.emit(ast, hasEmitted)
            )
        } else{
            reference.emit(ast, hasEmitted)
        }

    fun Type.emit(ast: AST, hasEmitted: MutableList<String>): AvroModel.RecordType = AvroModel.RecordType(
        name = identifier.value,
        type = "record",
        fields = shape.value.map { field ->
            AvroModel.Field(
                name = field.identifier.value,
                type = if(field.isNullable){
                    AvroModel.TypeList(
                        AvroModel.SimpleType("null"),
                        field.emit(ast,hasEmitted)
                    )
                }else{
                    AvroModel.TypeList(field.emit(ast,hasEmitted))
                }
            )
        }
    )

    fun emit(ast: AST): List<AvroModel.Type> {
        val hasEmitted = mutableListOf<String>()
        return ast
            .filterIsInstance<Definition>()
            .mapNotNull { when(it){
                is Type -> it.emit(ast, hasEmitted)
                is Enum -> it.emit()
                else -> null
            } }
    }

    private fun AST.findType(name: String): Definition? = filterIsInstance<Definition>().find { it.identifier.value == name }
}
