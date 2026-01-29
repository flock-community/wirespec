package community.flock.wirespec.language.core.generator

import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.Call
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.PrintStatement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Statement
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.language.core.Function as AstFunction

object TypeScriptGenerator : CodeGenerator {
    override fun generate(element: Element): String = when (element) {
        is File -> element.emit(0)
        else -> File("", listOf(element)).emit(0)
    }

    private fun String.indentCode(level: Int): String = " ".repeat(level * 4) + this

    private fun File.emit(indent: Int): String {
        val allUnions = elements.flatMap { it.findAllUnions() }
        return elements.joinToString("") { it.emit(indent, allUnions = allUnions) }.removeEmptyLines()
    }

    private fun String.removeEmptyLines(): String =
        lines().filter { it.isNotEmpty() }.joinToString("\n")

    private fun Element.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> emit(indent, parents, allUnions = allUnions)
        is AstFunction -> emit(indent)
        is Static -> emit(indent, parents, allUnions = allUnions)
        is Interface -> emit(indent, parents, allUnions = allUnions)
        is Union -> emit(indent, parents, allUnions = allUnions)
        is Enum -> emit(indent)
        is File -> elements.joinToString("") { it.emit(indent, parents, allUnions) }
    }

    private fun Element.findAllUnions(): List<Union> {
        val result = mutableListOf<Union>()
        if (this is Union) result.add(this)
        when (this) {
            is Struct -> result.addAll(elements.flatMap { it.findAllUnions() })
            is Static -> result.addAll(elements.flatMap { it.findAllUnions() })
            is Interface -> result.addAll(elements.flatMap { it.findAllUnions() })
            else -> {}
        }
        return result
    }

    private fun Package.emit(indent: Int): String = "// package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path;\n".indentCode(indent)

    private fun Static.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val ext = mutableListOf<String>()
        extends?.let { ext.add(it.emit()) }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.joinToString(", ")}"
        val content = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        return "interface $name$extStr {\n$content}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val ext = mutableListOf<String>()
        extends?.let { ext.add(it.emit()) }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.joinToString(", ")}"
        val content = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        return "interface $name$extStr {\n$content}\n\n".indentCode(indent)
    }

    private fun Union.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val ext = mutableListOf<String>()
        extends?.let { ext.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { ext.add(it.name) }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.distinct().joinToString(", ")}"
        return "interface $name$extStr {}\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val entriesStr = entries.joinToString(",\n") { it.indentCode(indent + 1) }
        return "enum $name {\n$entriesStr\n${"}".indentCode(indent)}\n\n".indentCode(indent)
    }

    private fun Struct.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val ext = mutableListOf<String>()
        val impl = mutableListOf<String>()

        interfaces.forEach { ext.add(it.emit()) }

        parents.filterIsInstance<Union>().forEach { impl.add(it.name) }
        allUnions.filter { it.members.contains(this.name) }.forEach { impl.add(it.name) }

        val extendsStr = if (ext.isEmpty()) "" else " extends ${ext.distinct().joinToString(", ")}"
        val implementsStr = if (impl.isEmpty()) "" else " implements ${impl.distinct().joinToString(", ")}"
        val fieldsContent = fields.joinToString("") { it.emit(indent + 1) }
        val constructorParams = fields.joinToString(", ") { "${it.name}: ${it.type.emit()}" }
        val constructorBody = fields.joinToString("") { "this.${it.name} = ${it.name};\n".indentCode(indent + 2) }
        val defaultConstructor = """
            |constructor($constructorParams) {
            |$constructorBody${"}".indentCode(indent + 1)}
        """.trimMargin().indentCode(indent + 1)
        val customConstructors = constructors.joinToString("") { it.emit(indent + 1) }
        val nestedContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        return "class $name$extendsStr$implementsStr {\n$fieldsContent\n$defaultConstructor\n$customConstructors$nestedContent${"}".indentCode(indent)}\n\n"
    }

    private fun Constructor.emit(indent: Int): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val bodyContent = body.joinToString("") { it.emit(indent + 1) }
        return """
            |constructor($params) {
            |$bodyContent${"}".indentCode(indent)}
        """.trimMargin().indentCode(indent)
    }

    private fun Field.emit(indent: Int): String = "public $name: ${type.emit()};\n".indentCode(indent)

    private fun AstFunction.emit(indent: Int): String {
        val rType = returnType?.let { ": ${it.emit()}" } ?: ""
        val params = parameters.joinToString(", ") { it.emit(0) }
        val prefix = if (isAsync) "async " else ""
        return if (body.isEmpty()) {
            val tsRType = if (isAsync) {
                if (returnType == null || returnType == Type.Unit) {
                    ": Promise<void>"
                } else {
                    ": Promise<${returnType.emit()}>"
                }
            } else {
                rType
            }
            // Interfaces in TS don't use 'async' keyword on methods
            "$name($params)$tsRType;\n\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(indent + 1) }
            "${prefix}function $name($params)$rType {\n$content}\n\n".indentCode(indent)
        }
    }

    private fun Parameter.emit(indent: Int): String = "$name: ${type.emit()}"

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> "number"
        is Type.Number -> "number"
        Type.String -> "string"
        Type.Boolean -> "boolean"
        Type.Bytes -> "Uint8Array"
        Type.Unit -> "void"
        is Type.Array -> {
            val element = elementType.emit()
            if (elementType is Type.Nullable) "($element)[]" else "$element[]"
        }
        is Type.Dict -> "Record<${keyType.emit()}, ${valueType.emit()}>"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                name
            } else {
                "$name<${generics.joinToString(", ") { it.emit() }}>"
            }
        }
        is Type.Nullable -> "${type.emit()} | null"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "console.log(${expression.emit()});\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()};\n".indentCode(indent)
        is ConstructorStatement -> {
            val named = namedArguments.map { "${it.key}: ${it.value.emit()}" }.joinToString(", ")
            val allArgs = if (named.isEmpty()) "" else "{ $named }"
            "new ${type.emit()}($allArgs);\n".indentCode(indent)
        }
        is Call -> {
            val named = arguments.map { "${it.key}: ${it.value.emit()}" }.joinToString(", ")
            val allArgs = if (named.isEmpty()) "" else "{ $named }"
            "$name($allArgs);\n".indentCode(indent)
        }
        is Literal -> "${emit()};\n".indentCode(indent)
        is LiteralList -> "${emit()};\n".indentCode(indent)
        is LiteralMap -> "${emit()};\n".indentCode(indent)
        is Assignment -> {
            if (isProperty) {
                "$name = ${value.emit()};\n".indentCode(indent)
            } else {
                "const $name = ${value.emit()};\n".indentCode(indent)
            }
        }
        is ErrorStatement -> "throw new Error(${message.emit()});\n".indentCode(indent)
        is Switch -> {
            val casesStr = cases.joinToString("") { case ->
                val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                "case ${case.value.emit()}:\n$bodyStr${"break;\n".indentCode(indent + 2)}".indentCode(indent + 1)
            }
            val defaultStr = default?.let {
                val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                "default:\n$bodyStr".indentCode(indent + 1)
            } ?: ""
            "switch (${expression.emit()}) {\n$casesStr$defaultStr${"}".indentCode(indent)}\n"
        }
    }

    private fun Expression.emit(): String = when (this) {
        is Call -> {
            val named = arguments.map { "${it.key}: ${it.value.emit()}" }.joinToString(", ")
            val allArgs = if (named.isEmpty()) "" else "{ $named }"
            "$name($allArgs)"
        }
        is ConstructorStatement -> {
            val named = namedArguments.map { "${it.key}: ${it.value.emit()}" }.joinToString(", ")
            val allArgs = if (named.isEmpty()) "" else "{ $named }"
            "new ${type.emit()}($allArgs)"
        }
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is ErrorStatement -> throw IllegalArgumentException("ErrorStatement cannot be an expression in TypeScript")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in TypeScript")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in TypeScript")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in TypeScript")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in TypeScript")
    }

    private fun LiteralList.emit(): String {
        val list = values.joinToString(", ") { it.emit() }
        return "[$list]"
    }

    private fun LiteralMap.emit(): String {
        val map = values.entries.joinToString(", ") {
            "${Literal(it.key, keyType).emit()}: ${it.value.emit()}"
        }
        return "{ $map }"
    }

    private fun Literal.emit(): String = when (type) {
        Type.String -> "'$value'"
        else -> value.toString()
    }
}
