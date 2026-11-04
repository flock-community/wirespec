package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Shape
import community.flock.wirespec.compiler.core.parse.ast.ShapeEntry
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type

internal interface WirespecTypeDefinitionEmitter : TypeDefinitionEmitter, WirespecIdentifierEmitter, WirespecMetaDataEmitter {

    override fun emit(type: Type, module: Module) = """
        |type ${emit(type.identifier)} {
        |${type.shape.emit()}
        |}
        |""".trimMargin()

    override fun Shape.emit() = value.joinToString(",\n") { entry ->
        when (entry) {
            is Field -> entry.annotations.joinToString("") { "$Spacer${it.emit()}\n" } + "$Spacer${entry.emitDeclaration()}"
            is Spread -> "$Spacer${entry.emit()}"
        }
    }

    override fun Field.emit() = annotations.joinToString("") { "${it.emit()} " } + emitDeclaration()

    private fun Field.emitDeclaration() = "${emit(identifier)}: ${reference.emit()}${defaultValue?.let { " = ${it.emit()}" }.orEmpty()}"

    private fun DefaultValue.emit(): String = when (this) {
        is DefaultValue.StringValue -> "\"${value.escape()}\""
        is DefaultValue.IntegerValue -> value
        is DefaultValue.NumberValue -> value
        is DefaultValue.BooleanValue -> value.toString()
        is DefaultValue.EnumValue -> value.emitEnumEntry()
        is DefaultValue.NullValue -> "null"
    }

    private fun String.escape(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    fun ShapeEntry.emit(): String = when (this) {
        is Field -> emit()
        is Spread -> "...${reference.emit()}"
    }

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "{ ${reference.emit()} }${emitNullable()}"
        is Reference.Iterable -> "${reference.emit()}[]${emitNullable()}"
        is Reference.Unit -> "Unit${emitNullable()}"
        is Reference.Any -> "Any${emitNullable()}"
        is Reference.Custom -> "$value${emitNullable()}"
        // The parser reads the `?` of a String before its constraint, and that of a number after it.
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String${emitNullable()}${t.constraint?.emit().orEmpty()}"
            is Reference.Primitive.Type.Boolean -> "Boolean${emitNullable()}"
            is Reference.Primitive.Type.Bytes -> "Bytes${emitNullable()}"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Integer32"
                Reference.Primitive.Type.Precision.P64 -> "Integer"
            } + t.constraint?.emit().orEmpty() + emitNullable()

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Number32"
                Reference.Primitive.Type.Precision.P64 -> "Number"
            } + t.constraint?.emitDecimal().orEmpty() + emitNullable()
        }
    }

    private fun Reference.emitNullable(): String = if (isNullable) "?" else ""

    private fun Reference.Primitive.Type.Constraint.Bound.emitDecimal(): String =
        "(${min.toBoundDecimal()}, ${max.toBoundDecimal()})"

    private fun String?.toBoundDecimal(): String = when {
        this == null -> "_"
        contains(".") -> this
        else -> "$this.0"
    }

}
