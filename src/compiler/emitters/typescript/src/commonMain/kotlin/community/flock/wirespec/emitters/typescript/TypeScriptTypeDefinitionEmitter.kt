package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

interface TypeScriptTypeDefinitionEmitter: TypeDefinitionEmitter, ImportEmitter, TypeScriptIdentifierEmitter {

    fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    override fun emit(type: Type, module: Module):String =
        """
            |${type.importReferences().distinctBy { it.value }.map { "import {${it.value}} from './${it.value}'" }.joinToString("\n") { it.trimStart() }}
            |export type ${type.identifier.sanitizeSymbol()} = {
            |${type.shape.emit()}
            |}
            |
        """.trimMargin()

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Record<string, ${reference.emit()}>"
        is Reference.Iterable -> "${reference.emit()}[]"
        is Reference.Unit -> "void"
        is Reference.Any -> "any"
        is Reference.Custom -> value.sanitizeSymbol()
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "string"
            is Reference.Primitive.Type.Integer -> "number"
            is Reference.Primitive.Type.Number -> "number"
            is Reference.Primitive.Type.Boolean -> "boolean"
            is Reference.Primitive.Type.Bytes -> "ArrayBuffer"
        }
    }.let { "$it${if (isNullable) " | undefined" else ""}" }

    override fun Refined.emitValidator(): String {
        val defaultReturn = "true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

    override fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> """$value.test(value)"""
        is Reference.Primitive.Type.Constraint.Bound -> """$min < value && value < $max;"""
    }

    override fun Type.Shape.emit() = value.joinToString(",\n") { it.emit() }

    override fun Field.emit() = "$Spacer${emit(identifier)}: ${reference.emit()}"
}