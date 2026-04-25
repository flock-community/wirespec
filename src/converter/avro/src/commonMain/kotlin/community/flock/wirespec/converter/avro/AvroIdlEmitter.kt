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
        val sb = StringBuilder()
        sb.append("protocol ").append(protocolName).append(" {\n")
        val statements = module.statements.toList()
        statements.forEachIndexed { index, definition ->
            val rendered = when (definition) {
                is Type -> renderRecord(definition)
                is Enum -> renderEnum(definition)
                is Union -> renderUnion(definition)
                is Refined -> renderRefined(definition)
                else -> null
            }
            if (rendered != null) {
                sb.append(rendered)
                if (index != statements.lastIndex) sb.append("\n")
            }
        }
        sb.append("}\n")
        return sb.toString()
    }

    private fun renderRecord(type: Type): String {
        val sb = StringBuilder()
        type.comment?.value?.let { sb.append(renderDoc(it, INDENT)) }
        sb.append(INDENT).append("record ").append(type.identifier.value).append(" {\n")
        type.shape.value.forEach { field ->
            sb.append(INDENT).append(INDENT)
            sb.append(renderReference(field.reference))
            sb.append(' ')
            sb.append(field.identifier.value)
            sb.append(";\n")
        }
        sb.append(INDENT).append("}\n")
        return sb.toString()
    }

    private fun renderEnum(enum: Enum): String {
        val sb = StringBuilder()
        enum.comment?.value?.let { sb.append(renderDoc(it, INDENT)) }
        sb.append(INDENT).append("enum ").append(enum.identifier.value).append(" {\n")
        sb.append(INDENT).append(INDENT)
        sb.append(enum.entries.joinToString(", "))
        sb.append("\n")
        sb.append(INDENT).append("}\n")
        return sb.toString()
    }

    private fun renderUnion(union: Union): String {
        val sb = StringBuilder()
        union.comment?.value?.let { sb.append(renderDoc(it, INDENT)) }
        sb.append(INDENT).append("record ").append(union.identifier.value).append(" {\n")
        sb.append(INDENT).append(INDENT)
        sb.append("union { ")
        sb.append(union.entries.joinToString(", ") { renderReference(it) })
        sb.append(" } value;\n")
        sb.append(INDENT).append("}\n")
        return sb.toString()
    }

    private fun renderRefined(refined: Refined): String {
        val sb = StringBuilder()
        refined.comment?.value?.let { sb.append(renderDoc(it, INDENT)) }
        sb.append(INDENT).append("// refined ").append(refined.identifier.value)
        sb.append(" = ").append(renderPrimitive(refined.reference.type)).append("\n")
        return sb.toString()
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
        val sb = StringBuilder()
        sb.append(indent).append("/**")
        if (lines.size == 1) {
            sb.append(' ').append(lines.first()).append(" */\n")
        } else {
            sb.append('\n')
            lines.forEach { sb.append(indent).append(" * ").append(it).append('\n') }
            sb.append(indent).append(" */\n")
        }
        return sb.toString()
    }
}
