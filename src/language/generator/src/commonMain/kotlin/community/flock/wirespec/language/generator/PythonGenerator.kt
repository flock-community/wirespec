package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.AnonymousClass
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.BinaryOp
import community.flock.wirespec.language.core.Call
import community.flock.wirespec.language.core.ClassLiteral
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.EnumReference
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.MethodCall
import community.flock.wirespec.language.core.NullLiteral
import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.PrintStatement
import community.flock.wirespec.language.core.PropertyAccess
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Statement
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.StaticCall
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.Function as AstFunction

object PythonGenerator : CodeGenerator {
    override fun generate(element: Element): String = when (element) {
        is File -> element.emit(0)
        else -> File("", listOf(element)).emit(0)
    }

    private fun String.indentCode(level: Int): String = " ".repeat(level * 4) + this

    private fun File.emit(indent: Int): String {
        val allUnions = elements.flatMap { it.findAllUnions() }
        return elements.joinToString("") { it.emit(indent, allUnions = allUnions) }.removeEmptyLines()
    }

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n")

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
        is RawElement -> code.indentCode(indent)
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

    private fun Package.emit(indent: Int): String = "# package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path\n".indentCode(indent)

    private fun Static.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }

        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val content = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        return "class $name$ext:\n$content\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }

        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val content = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        return "class $name$ext:\n$content\n".indentCode(indent)
    }

    private fun Union.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name) }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        return "class $name$ext:\n${"pass".indentCode(indent + 1)}\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val entriesStr = if (entries.isEmpty()) {
            "pass".indentCode(indent + 1)
        } else {
            entries.joinToString("\n") { "${it.name} = \"${it.name}\"".indentCode(indent + 1) }
        }
        return "class $name:\n$entriesStr\n\n".indentCode(indent)
    }

    private fun Struct.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        interfaces.forEach { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name) }
        allUnions.filter { it.members.contains(this.name) }.forEach { p.add(it.name) }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        val nestedContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions) }
        val init = if (fields.isEmpty() && constructors.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            val params = fields.joinToString("") { ", ${it.name}" }
            val body = if (fields.isEmpty()) "pass\n".indentCode(indent + 2) else fields.joinToString("") { it.emit(indent + 2) }
            val defaultInit = "def __init__(self$params):\n$body".indentCode(indent + 1)
            val customConstructors = constructors.joinToString("") { it.emit(indent + 1) }
            "$defaultInit$customConstructors"
        }

        return "class $name$ext:\n$init$nestedContent\n".indentCode(indent)
    }

    private fun Constructor.emit(indent: Int): String {
        val params = parameters.joinToString("") { ", ${it.name}" }
        val content = if (body.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            body.joinToString("") { it.emit(indent + 1).replace("this.", "self.") }
        }
        return "def __init__(self$params):\n$content\n".indentCode(indent)
    }

    private fun Field.emit(indent: Int): String = "self.$name = $name\n".indentCode(indent)

    private fun AstFunction.emit(indent: Int): String {
        val params = parameters.joinToString(", ") { it.name }
        val content = if (body.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            body.joinToString("") { it.emit(indent + 1) }
        }
        val prefix = if (isAsync) "async " else ""
        return "${prefix}def $name($params):\n$content\n".indentCode(indent)
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> "int"
        is Type.Number -> "float"
        Type.String -> "str"
        Type.Boolean -> "bool"
        Type.Bytes -> "bytes"
        Type.Unit -> "None"
        is Type.Array -> "list"
        is Type.Dict -> "dict[${keyType.emit()}, ${valueType.emit()}]"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                name
            } else {
                "$name[${generics.joinToString(", ") { it.emit() }}]"
            }
        }
        is Type.Nullable -> "Optional[${type.emit()}]"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "print(${expression.emit()})\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()}\n".indentCode(indent)
        is ConstructorStatement -> {
            val allArgs = namedArguments.map { "${it.key}=${it.value.emit()}" }
            "${type.emit()}(${allArgs.joinToString(", ")})\n".indentCode(indent)
        }
        is Call -> "$name(${arguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})\n".indentCode(indent)
        is Literal -> "${emit()}\n".indentCode(indent)
        is LiteralList -> "${emit()}\n".indentCode(indent)
        is LiteralMap -> "${emit()}\n".indentCode(indent)
        is Assignment -> "$name = ${value.emit()}\n".indentCode(indent)
        is ErrorStatement -> "raise Exception(${message.emit()})\n".indentCode(indent)
        is Switch -> {
            val casesStr = cases.joinToString("") { case ->
                val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                "case ${case.value.emit()}:\n$bodyStr".indentCode(indent + 1)
            }
            val defaultStr = default?.let {
                val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                "case _:\n$bodyStr".indentCode(indent + 1)
            } ?: ""
            "match ${expression.emit()}:\n$casesStr$defaultStr"
        }
        is NullLiteral -> "None\n".indentCode(indent)
        is VariableReference -> "$name\n".indentCode(indent)
        is PropertyAccess -> "${receiver.emit()}.$property\n".indentCode(indent)
        is MethodCall -> "${receiver.emit()}.$method(${arguments.joinToString(", ") { it.emit() }})\n".indentCode(indent)
        is EnumReference -> "${enumType.emit()}.$entry\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toPython()} ${right.emit()})\n".indentCode(indent)
        is StaticCall -> "$qualifiedName(${arguments.joinToString(", ") { it.emit() }})\n".indentCode(indent)
        is ClassLiteral -> "${type.emit()}\n".indentCode(indent)
        is AnonymousClass -> throw IllegalArgumentException("AnonymousClass is not supported in Python")
    }

    private fun BinaryOp.Operator.toPython(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun Expression.emit(): String = when (this) {
        is Call -> "$name(${arguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})"
        is ConstructorStatement -> "${type.emit()}(${namedArguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})"
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "None"
        is VariableReference -> name
        is PropertyAccess -> "${receiver.emit()}.$property"
        is MethodCall -> "${receiver.emit()}.$method(${arguments.joinToString(", ") { it.emit() }})"
        is EnumReference -> "${enumType.emit()}.$entry"
        is BinaryOp -> "(${left.emit()} ${operator.toPython()} ${right.emit()})"
        is StaticCall -> "$qualifiedName(${arguments.joinToString(", ") { it.emit() }})"
        is ClassLiteral -> type.emit()
        is AnonymousClass -> throw IllegalArgumentException("AnonymousClass is not supported in Python")
        is ErrorStatement -> throw IllegalArgumentException("ErrorStatement cannot be an expression in Python")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Python")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Python")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Python")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Python")
    }

    private fun LiteralList.emit(): String {
        val list = values.joinToString(", ") { it.emit() }
        return "[$list]"
    }

    private fun LiteralMap.emit(): String {
        val map = values.entries.joinToString(", ") {
            "${Literal(it.key, keyType).emit()}: ${it.value.emit()}"
        }
        return "{$map}"
    }

    private fun Literal.emit(): String = when (type) {
        Type.String -> "'$value'"
        Type.Boolean -> value.toString().replaceFirstChar { it.uppercase() }
        else -> value.toString()
    }
}
