package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

object AvroEmitter : Emitter() {

    override val extension = FileExtension.JSON

    override val shared = null

    override fun emit(type: Type, module: Module): String {
        TODO("Not yet implemented")
    }

    fun Enum.emit(): AvroModel.EnumType = AvroModel.EnumType(
        type = "enum",
        name = identifier.value,
        symbols = entries.toList()
    )

    fun Union.emit(): AvroModel.UnionType = AvroModel.UnionType(
        name = identifier.value,
        type = AvroModel.TypeList(entries.map { AvroModel.SimpleType(it.value) })
    )

    fun Reference.emit(module: Module, hasEmitted: MutableList<String>): AvroModel.Type = when (this) {
        is Reference.Dict -> AvroModel.MapType(type = "map", values = reference.emit(module, hasEmitted))
        is Reference.Iterable -> AvroModel.ArrayType(type = "array", items = reference.emit(module, hasEmitted))
        is Reference.Primitive -> {
            when (val type = type) {
                is Reference.Primitive.Type.String -> AvroModel.SimpleType("string")
                is Reference.Primitive.Type.Integer -> when (type.precision) {
                    Reference.Primitive.Type.Precision.P32 -> AvroModel.SimpleType("int")
                    Reference.Primitive.Type.Precision.P64 -> AvroModel.SimpleType("long")
                }

                is Reference.Primitive.Type.Number -> when (type.precision) {
                    Reference.Primitive.Type.Precision.P32 -> AvroModel.SimpleType("float")
                    Reference.Primitive.Type.Precision.P64 -> AvroModel.SimpleType("double")
                }

                is Reference.Primitive.Type.Boolean -> AvroModel.SimpleType("boolean")
                is Reference.Primitive.Type.Bytes -> AvroModel.SimpleType("bytes")
            }
        }

        is Reference.Custom -> {
            when (val def = module.findType(value)) {
                is Type -> if (hasEmitted.contains(def.identifier.value)) {
                    def.identifier.value.let(AvroModel::SimpleType)
                } else {
                    def.also { hasEmitted.add(def.identifier.value) }.emit(module, hasEmitted)
                }

                is Enum -> AvroModel.SimpleType(def.identifier.value)
                is Refined -> AvroModel.SimpleType("string")
                else -> AvroModel.SimpleType(value)
            }
        }

        is Reference.Any -> TODO()
        is Reference.Unit -> TODO()
    }

    fun Field.emit(module: Module, hasEmitted: MutableList<String>) =
        when (val ref = reference) {
            is Reference.Iterable -> AvroModel.ArrayType(
                type = "array",
                items = ref.reference.emit(module, hasEmitted)
            )
            else -> ref.emit(module, hasEmitted)
        }

    fun Type.emit(module: Module, hasEmitted: MutableList<String>): AvroModel.RecordType = AvroModel.RecordType(
        name = identifier.value,
        type = "record",
        fields = shape.value.map { field ->
            AvroModel.Field(
                name = field.identifier.value,
                type = if (field.reference.isNullable) {
                    AvroModel.TypeList(
                        AvroModel.SimpleType("null"),
                        field.emit(module, hasEmitted)
                    )
                } else {
                    AvroModel.TypeList(field.emit(module, hasEmitted))
                }
            )
        }
    )

    fun emit(module: Module): List<AvroModel.Type> {
        val hasEmitted = mutableListOf<String>()
        return module.statements.toList()
            .mapNotNull {
                when (it) {
                    is Type -> it.emit(module, hasEmitted)
                    is Enum -> it.emit()
                    is Union -> it.emit()
                    else -> null
                }
            }
    }

    private fun Module.findType(name: String): Definition? = statements.toList().find { it.identifier.value == name }

    override fun Type.Shape.emit(): String {
        TODO("Not yet implemented")
    }

    override fun Field.emit(): String {
        TODO("Not yet implemented")
    }

    override fun Reference.emit(): String {
        TODO("Not yet implemented")
    }

    override fun Refined.Validator.emit(): String {
        TODO("Not yet implemented")
    }

    override fun emit(enum: Enum, module: Module): String {
        TODO("Not yet implemented")
    }

    override fun emit(refined: Refined): String {
        TODO("Not yet implemented")
    }

    override fun emit(endpoint: Endpoint): String {
        TODO("Not yet implemented")
    }

    override fun emit(union: Union): String {
        TODO("Not yet implemented")
    }

    override fun emit(identifier: Identifier): String {
        TODO("Not yet implemented")
    }

    override fun emit(channel: Channel): String {
        TODO("Not yet implemented")
    }

    override val singleLineComment: String
        get() = TODO("Not yet implemented")
}
