package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type

interface TypeScriptTypeDefinitionEmitter : TypeDefinitionEmitter, TypeScriptIdentifierEmitter {

    fun Identifier.sanitizeSymbol() = value.sanitizeSymbol()

    fun String.sanitizeSymbol() = asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")

    override fun emit(type: Type, module: Module): String =
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
        is Reference.Primitive -> emitPrimitive()
    }.let { "$it${if (isNullable) " | undefined" else ""}" }

    fun Reference.Primitive.emitPrimitive(): String = when (type) {
        is Reference.Primitive.Type.String -> "string"
        is Reference.Primitive.Type.Integer -> "number"
        is Reference.Primitive.Type.Number -> "number"
        is Reference.Primitive.Type.Boolean -> "boolean"
        is Reference.Primitive.Type.Bytes -> "ArrayBuffer"
    }

    override fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> """${Spacer}return $value.test(value);"""
        is Reference.Primitive.Type.Constraint.Bound -> {
            val minCheck = min?.let { "$it < num" }
            val maxCheck = max?.let { "num < $it" }

            "${Spacer}return ${listOfNotNull(minCheck, maxCheck).joinToString(" && ").let { it.ifEmpty { "true" } }};"
        }
    }

    override fun Type.Shape.emit() = value.joinToString(",\n") { it.emit() }

    override fun Field.emit() = "$Spacer${emit(identifier)}: ${reference.emit()}"
}
