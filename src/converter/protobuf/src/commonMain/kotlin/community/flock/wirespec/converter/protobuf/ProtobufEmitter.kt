package community.flock.wirespec.converter.protobuf

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger

/**
 * Emits a self-contained, dependency-free proto3 schema from a Wirespec AST.
 *
 * Mapping:
 * - `type`   → `message`
 * - `enum`   → `enum` (a zero-valued `*_UNSPECIFIED` entry is injected as proto3 requires)
 * - `union`  → `message` with a `oneof`
 * - `rpc`    → a synthesized `<Rpc>Request` message wrapping the parameters plus a
 *              `service <Rpc>` exposing `rpc <Rpc> (<Rpc>Request) returns (<Response>)`
 * - `refined`→ resolved to its underlying scalar wherever it is referenced
 */
class ProtobufEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
) : Emitter {

    override val extension = FileExtension.Proto

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = ast.modules
        .map { module ->
            logger.info("Emitting proto from ${module.fileUri.value}")
            Emitted(module.protoFileName(), module.emit())
        }
        .toList()
        .toNonEmptyListOrNull()!!

    private fun Module.emit(): String {
        val blocks = buildList {
            statements.filterIsInstance<Type>().forEach { add(it.emit(this@emit)) }
            statements.filterIsInstance<Enum>().forEach { add(it.emit()) }
            statements.filterIsInstance<Union>().forEach { add(it.emit()) }
            statements.filterIsInstance<Rpc>().forEach { rpc ->
                add(rpc.emitRequestMessage(this@emit))
                rpc.emitResponseMessage(this@emit)?.let { add(it) }
                add(rpc.emitService(this@emit))
            }
        }
        return buildString {
            appendLine("syntax = \"proto3\";")
            appendLine()
            appendLine("package ${packageName.value};")
            appendLine()
            appendLine("option java_multiple_files = true;")
            appendLine("option java_package = \"${packageName.value}\";")
            blocks.forEach {
                appendLine()
                append(it)
            }
        }
    }

    private fun Type.emit(module: Module): String = buildString {
        appendLine("message ${identifier.value} {")
        shape.value.forEachIndexed { index, field ->
            appendLine("  ${field.emit(module, index + 1)}")
        }
        appendLine("}")
    }

    private fun Enum.emit(): String = buildString {
        val prefix = identifier.value.toScreamingSnakeCase()
        appendLine("enum ${identifier.value} {")
        appendLine("  ${prefix}_UNSPECIFIED = 0;")
        entries.forEachIndexed { index, entry ->
            appendLine("  ${entry.toScreamingSnakeCase()} = ${index + 1};")
        }
        appendLine("}")
    }

    private fun Union.emit(): String = buildString {
        appendLine("message ${identifier.value} {")
        appendLine("  oneof value {")
        entries.forEachIndexed { index, ref ->
            appendLine("    ${ref.value} ${ref.value.toSnakeCase()} = ${index + 1};")
        }
        appendLine("  }")
        appendLine("}")
    }

    private fun Rpc.emitRequestMessage(module: Module): String = buildString {
        appendLine("message ${requestMessageName()} {")
        requestParameters.forEachIndexed { index, field ->
            appendLine("  ${field.emit(module, index + 1)}")
        }
        appendLine("}")
    }

    /** Only synthesized when the response is not already a custom message type. */
    private fun Rpc.emitResponseMessage(module: Module): String? = when (val resolved = response.resolved(module)) {
        is ResolvedType.Message -> null
        is ResolvedType.Scalar -> buildString {
            appendLine("message ${responseMessageName()} {")
            appendLine("  ${response.emitField("value", 1, module)}")
            appendLine("}")
        }
        is ResolvedType.Empty -> "message ${responseMessageName()} {}\n"
    }

    private fun Rpc.emitService(module: Module): String = buildString {
        val responseType = when (response.resolved(module)) {
            is ResolvedType.Message -> response.value
            else -> responseMessageName()
        }
        appendLine("service ${identifier.value} {")
        appendLine("  rpc ${identifier.value} (${requestMessageName()}) returns ($responseType);")
        appendLine("}")
    }

    private fun Rpc.requestMessageName() = "${identifier.value}Request"
    private fun Rpc.responseMessageName() = "${identifier.value}Response"

    private fun Field.emit(module: Module, number: Int): String = reference.emitField(identifier.value.toSnakeCase(), number, module)

    private fun Reference.emitField(name: String, number: Int, module: Module): String = when (this) {
        is Reference.Iterable -> "repeated ${reference.protoType(module)} $name = $number;"
        is Reference.Dict -> "map<string, ${reference.protoType(module)}> $name = $number;"
        else -> {
            val optional = if (isNullable) "optional " else ""
            "$optional${protoType(module)} $name = $number;"
        }
    }

    private fun Reference.protoType(module: Module): String = when (this) {
        is Reference.Primitive -> type.protoScalar()
        is Reference.Custom -> when (val def = module.find(value)) {
            is Refined -> def.reference.type.protoScalar()
            else -> value
        }
        is Reference.Iterable -> reference.protoType(module)
        is Reference.Dict -> reference.protoType(module)
        is Reference.Unit -> "google.protobuf.Empty"
        is Reference.Any -> "google.protobuf.Any"
    }

    private fun Reference.Primitive.Type.protoScalar(): String = when (this) {
        is Reference.Primitive.Type.String -> "string"
        is Reference.Primitive.Type.Boolean -> "bool"
        is Reference.Primitive.Type.Bytes -> "bytes"
        is Reference.Primitive.Type.Integer -> when (precision) {
            Reference.Primitive.Type.Precision.P32 -> "int32"
            Reference.Primitive.Type.Precision.P64 -> "int64"
        }
        is Reference.Primitive.Type.Number -> when (precision) {
            Reference.Primitive.Type.Precision.P32 -> "float"
            Reference.Primitive.Type.Precision.P64 -> "double"
        }
    }

    private sealed interface ResolvedType {
        data object Message : ResolvedType
        data object Scalar : ResolvedType
        data object Empty : ResolvedType
    }

    // A response may be used directly as the rpc return type only when it is a bare message
    // (custom type or union). Collections, scalars and Unit must be wrapped, since proto rpc
    // methods can only return a single message type.
    private fun Reference.resolved(module: Module): ResolvedType = when (this) {
        is Reference.Unit -> ResolvedType.Empty
        is Reference.Custom -> when (module.find(value)) {
            is Type, is Union -> ResolvedType.Message
            else -> ResolvedType.Scalar
        }
        else -> ResolvedType.Scalar
    }

    private fun Module.find(name: String): Definition? = statements.find { it.identifier.value == name }

    private fun Module.protoFileName(): String = fileUri.value
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .ifEmpty { "wirespec" }
        .let { "$it.proto" }

    private fun String.toSnakeCase(): String = replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace('-', '_')
        .lowercase()

    private fun String.toScreamingSnakeCase(): String = toSnakeCase().uppercase()
}
