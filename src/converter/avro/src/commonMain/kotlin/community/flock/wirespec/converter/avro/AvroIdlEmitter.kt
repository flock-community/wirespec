package community.flock.wirespec.converter.avro

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger

object AvroIdlEmitter : Emitter {

    override val extension = FileExtension.AvroIdl

    private const val INDENT = "    "

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .map {
            logger.info("Emitting Avro IDL from ${it.fileUri.value}")
            Emitted("schema.avdl", emit(it, deriveProtocolName(it)))
        }

    private fun deriveProtocolName(module: Module): String = module.fileUri.value
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .takeIf { it.isNotBlank() }
        ?.let { it.replaceFirstChar { c -> c.uppercaseChar() } + "Protocol" }
        ?: "WirespecProtocol"

    fun emit(module: Module, protocolName: String = "WirespecProtocol"): String {
        val body = module.statements.toList().mapNotNull { definition ->
            when (definition) {
                is Type -> renderRecord(definition)
                is Enum -> renderEnum(definition)
                is Union -> renderUnion(definition)
                is Refined -> renderRefined(definition)
                else -> null
            }
        }.joinToString("\n")
        return "protocol $protocolName {\n$body}\n"
    }

    private fun renderRecord(type: Type): String {
        val doc = type.comment?.value?.let { renderDoc(it, INDENT) }.orEmpty()
        val fields = type.shape.value.joinToString("") { field ->
            "$INDENT$INDENT${renderReference(field.reference)} ${field.identifier.value};\n"
        }
        return "$doc${INDENT}record ${type.identifier.value} {\n$fields$INDENT}\n"
    }

    private fun renderEnum(enum: Enum): String {
        val doc = enum.comment?.value?.let { renderDoc(it, INDENT) }.orEmpty()
        val symbols = enum.entries.joinToString(", ")
        return "$doc${INDENT}enum ${enum.identifier.value} {\n$INDENT$INDENT$symbols\n$INDENT}\n"
    }

    private fun renderUnion(union: Union): String {
        val doc = union.comment?.value?.let { renderDoc(it, INDENT) }.orEmpty()
        val members = union.entries.joinToString(", ") { renderReference(it) }
        return "$doc${INDENT}record ${union.identifier.value} {\n$INDENT${INDENT}union { $members } value;\n$INDENT}\n"
    }

    private fun renderRefined(refined: Refined): String {
        val doc = refined.comment?.value?.let { renderDoc(it, INDENT) }.orEmpty()
        return "$doc$INDENT// refined ${refined.identifier.value} = ${renderPrimitive(refined.reference.type)}\n"
    }

    private fun renderReference(reference: Reference): String = when (reference) {
        is Reference.Primitive -> wrapNullable(renderPrimitive(reference.type), reference.isNullable)
        is Reference.Custom -> wrapNullable(reference.value, reference.isNullable)
        is Reference.Iterable -> wrapNullable("array<${renderReference(reference.reference.copy(isNullable = false))}>", reference.isNullable)
        is Reference.Dict -> wrapNullable("map<${renderReference(reference.reference.copy(isNullable = false))}>", reference.isNullable)
        is Reference.Any -> wrapNullable("bytes", reference.isNullable)
        is Reference.Unit -> "null"
    }

    private fun renderPrimitive(type: Reference.Primitive.Type): String = when (type) {
        is Reference.Primitive.Type.String -> "string"
        is Reference.Primitive.Type.Boolean -> "boolean"
        is Reference.Primitive.Type.Bytes -> "bytes"
        is Reference.Primitive.Type.Integer -> when (type.precision) {
            Reference.Primitive.Type.Precision.P32 -> "int"
            Reference.Primitive.Type.Precision.P64 -> "long"
        }
        is Reference.Primitive.Type.Number -> when (type.precision) {
            Reference.Primitive.Type.Precision.P32 -> "float"
            Reference.Primitive.Type.Precision.P64 -> "double"
        }
    }

    private fun wrapNullable(rendered: String, isNullable: Boolean): String = if (isNullable) "union { null, $rendered }" else rendered

    private fun renderDoc(value: String, indent: String): String {
        val lines = value.lines()
        return if (lines.size == 1) {
            "$indent/** ${lines.first()} */\n"
        } else {
            val body = lines.joinToString("") { "$indent * $it\n" }
            "$indent/**\n$body$indent */\n"
        }
    }
}
