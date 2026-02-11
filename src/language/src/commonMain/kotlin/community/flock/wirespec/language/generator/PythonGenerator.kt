package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.ArrayIndexCall
import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.BinaryOp
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.EnumReference
import community.flock.wirespec.language.core.EnumValueCall
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.FieldCall
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.FunctionCall
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.NullCheck
import community.flock.wirespec.language.core.NullLiteral
import community.flock.wirespec.language.core.NullableEmpty
import community.flock.wirespec.language.core.NullableMap
import community.flock.wirespec.language.core.NullableOf
import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.PrintStatement
import community.flock.wirespec.language.core.RawElement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Statement
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.TypeDescriptor
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

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n").plus("\n")

    private fun Element.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList(), isStaticScope: Boolean = false): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> emit(indent, parents, allUnions = allUnions)
        is AstFunction -> {
            val isInClass = parents.any { it is Struct || it is Interface || it is Static }
            val isInInterface = parents.any { it is Interface }
            emit(indent, isInClass = isInClass, isStaticScope = isStaticScope, isInInterface = isInInterface)
        }
        is Static -> emit(indent, parents, allUnions = allUnions)
        is Interface -> emit(indent, parents, allUnions = allUnions)
        is Union -> emit(indent, parents, allUnions = allUnions)
        is Enum -> emit(indent)
        is File -> elements.joinToString("") { it.emit(indent, parents, allUnions, isStaticScope) }
        is RawElement -> "$code\n".indentCode(indent)
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

    private fun Import.emit(indent: Int): String = "from $path import ${type.name}\n".indentCode(indent)

    private fun Static.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }

        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = true) }
        val content = elementsContent.ifEmpty { "pass\n".indentCode(indent + 1) }
        return "class $name$ext:\n$content\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = extends.map { it.emit() }.toMutableList()
        p.add("ABC")
        if (typeParameters.isNotEmpty()) {
            p.add("Generic[${typeParameters.joinToString(", ") { it.type.emit() }}]")
        }
        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val fieldsContent = fields.joinToString("") { field ->
            "${field.name}: ${field.type.emit()}\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = false) }
        val content = (fieldsContent + elementsContent).ifEmpty { "pass\n".indentCode(indent + 1) }
        return "class $name$ext:\n$content\n".indentCode(indent)
    }

    private fun Union.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name) }
        if (typeParameters.isNotEmpty()) {
            p.add("Generic[${typeParameters.joinToString(", ") { it.type.emit() }}]")
        }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        return "class $name$ext:\n${"pass".indentCode(indent + 1)}\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val ext = if (extends != null) "(${extends!!.emit()}, enum.Enum)" else "(enum.Enum)"
        val entriesStr = if (entries.isEmpty()) {
            "pass".indentCode(indent + 1)
        } else {
            entries.joinToString("\n") { entry ->
                val value = entry.values.firstOrNull() ?: "\"${entry.name}\""
                "${entry.name} = $value".indentCode(indent + 1)
            }
        }
        return "class $name$ext:\n$entriesStr\n\n".indentCode(indent)
    }

    private fun Struct.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        interfaces.forEach { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name) }
        allUnions.filter { it.members.any { m -> m.name == this.name } }.forEach { p.add(it.name) }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        val nestedContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = false) }
        val content = if (fields.isEmpty() && constructors.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            val fieldDecls = fields.joinToString("") { it.emit(indent + 1) }
            val customConstructors = constructors.joinToString("") { it.emit(indent + 1) }
            "$fieldDecls$customConstructors"
        }

        val decorator = "@dataclass\n".indentCode(indent)
        return decorator + "class $name$ext:\n$content$nestedContent\n".indentCode(indent)
    }

    private fun Constructor.emit(indent: Int): String {
        val content = if (body.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            body.joinToString("") { stmt ->
                when (stmt) {
                    is Assignment -> "self.${stmt.name} = ${stmt.value.emit()}\n".indentCode(indent + 1)
                    else -> stmt.emit(indent + 1).replace("this.", "self.")
                }
            }
        }
        if (parameters.isEmpty()) {
            return "def __init__(self):\n$content\n".indentCode(indent)
        }
        val selfParam = "self,\n".indentCode(indent + 1)
        val paramLines = parameters.joinToString(",\n") {
            "${it.name}: ${it.type.emit()}".indentCode(indent + 1)
        }
        val closeParen = "):\n".indentCode(indent)
        return "def __init__(\n$selfParam$paramLines,\n$closeParen$content\n".indentCode(indent)
    }

    private fun Field.emit(indent: Int): String = "$name: ${type.emit()}\n".indentCode(indent)

    private fun AstFunction.emit(indent: Int, isInClass: Boolean = false, isStaticScope: Boolean = false, isInInterface: Boolean = false): String {
        val params = parameters.joinToString(", ") {
            if (it.name == "self") it.name else "${it.name}: ${it.type.emit()}"
        }
        val effectivelyStatic = isStatic || isStaticScope
        val selfPrefix = if (isInClass && !effectivelyStatic && parameters.none { it.name == "self" }) {
            if (params.isEmpty()) "self" else "self, "
        } else {
            ""
        }
        val staticDecorator = if (isInClass && effectivelyStatic) "@staticmethod\n".indentCode(indent) else ""
        val abstractDecorator = if (isInInterface && body.isEmpty()) "@abstractmethod\n".indentCode(indent) else ""
        val content = if (body.isEmpty()) {
            "...\n".indentCode(indent + 1)
        } else {
            body.joinToString("") { it.emit(indent + 1) }
        }
        val prefix = if (isAsync) "async " else ""
        val returnAnnotation = returnType?.let { " -> ${it.emit()}" } ?: ""
        return staticDecorator + abstractDecorator + "${prefix}def $name($selfPrefix$params)$returnAnnotation:\n$content\n".indentCode(indent)
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> "int"
        is Type.Number -> "float"
        Type.Any -> "Any"
        Type.String -> "str"
        Type.Boolean -> "bool"
        Type.Bytes -> "bytes"
        Type.Unit -> "None"
        Type.Wildcard -> "Any"
        is Type.Array -> "list[${elementType.emit()}]"
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
            if (type == Type.Unit) {
                "None\n".indentCode(indent)
            } else {
                val allArgs = namedArguments.map { "${it.key}=${it.value.emit()}" }
                "${type.emit()}(${allArgs.joinToString(", ")})\n".indentCode(indent)
            }
        }
        is Literal -> "${emit()}\n".indentCode(indent)
        is LiteralList -> "${emit()}\n".indentCode(indent)
        is LiteralMap -> "${emit()}\n".indentCode(indent)
        is Assignment -> "$name = ${value.emit()}\n".indentCode(indent)
        is ErrorStatement -> "raise Exception(${message.emit()})\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                    val typeStr = case.type?.emit() ?: "object"
                    val varBinding = variable?.let { " as $it" } ?: ""
                    "case $typeStr()$varBinding:\n$bodyStr".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                    "case _:\n$bodyStr".indentCode(indent + 1)
                } ?: ""
                "match ${expression.emit()}:\n$casesStr$defaultStr".indentCode(indent)
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                    "case ${case.value.emit()}:\n$bodyStr".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                    "case _:\n$bodyStr".indentCode(indent + 1)
                } ?: ""
                "match ${expression.emit()}:\n$casesStr$defaultStr".indentCode(indent)
            }
        }
        is NullLiteral -> "None\n".indentCode(indent)
        is NullableEmpty -> "None\n".indentCode(indent)
        is VariableReference -> "$name\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr$field\n".indentCode(indent)
        }
        is FunctionCall -> {
            val recv = receiver
            if (recv != null) {
                "${recv.emit()}.$name(${arguments.values.joinToString(", ") { it.emit() }})\n".indentCode(indent)
            } else {
                "$name(${arguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})\n".indentCode(indent)
            }
        }
        is ArrayIndexCall -> "${receiver.emit()}[${index.emit()}]\n".indentCode(indent)
        is EnumReference -> "${enumType.emit()}.$entry\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()}.value\n".indentCode(indent)
        is BinaryOp -> {
            if (operator == BinaryOp.Operator.PLUS && (left is Literal && (left as Literal).type == Type.String || right is Literal && (right as Literal).type == Type.String)) {
                val leftStr = if (left is Literal && (left as Literal).type == Type.String) left.emit() else "str(${left.emit()})"
                val rightStr = if (right is Literal && (right as Literal).type == Type.String) right.emit() else "str(${right.emit()})"
                "($leftStr + $rightStr)\n".indentCode(indent)
            } else {
                "(${left.emit()} ${operator.toPython()} ${right.emit()})\n".indentCode(indent)
            }
        }
        is TypeDescriptor -> "${type.emit()}\n".indentCode(indent)
        is NullCheck -> "${emit()}\n".indentCode(indent)
        is NullableMap -> "${emit()}\n".indentCode(indent)
        is NullableOf -> "${emit()}\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toPython(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> if (type == Type.Unit) "None" else "${type.emit()}(${namedArguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})"
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "None"
        is NullableEmpty -> "None"
        is VariableReference -> name
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr$field"
        }
        is FunctionCall -> {
            val recv = receiver
            if (recv != null) {
                "${recv.emit()}.$name(${arguments.values.joinToString(", ") { it.emit() }})"
            } else {
                "$name(${arguments.map { "${it.key}=${it.value.emit()}" }.joinToString(", ")})"
            }
        }
        is ArrayIndexCall -> "${receiver.emit()}[${index.emit()}]"
        is EnumReference -> "${enumType.emit()}.$entry"
        is EnumValueCall -> "${expression.emit()}.value"
        is BinaryOp -> {
            if (operator == BinaryOp.Operator.PLUS && (left is Literal && (left as Literal).type == Type.String || right is Literal && (right as Literal).type == Type.String)) {
                val leftStr = if (left is Literal && (left as Literal).type == Type.String) left.emit() else "str(${left.emit()})"
                val rightStr = if (right is Literal && (right as Literal).type == Type.String) right.emit() else "str(${right.emit()})"
                "($leftStr + $rightStr)"
            } else {
                "(${left.emit()} ${operator.toPython()} ${right.emit()})"
            }
        }
        is TypeDescriptor -> type.emit()
        is NullCheck -> {
            val exprStr = expression.emit()
            val bodyStr = body.emitWithInlinedIt(exprStr)
            val altStr = alternative?.emit() ?: "None"
            "$bodyStr if $exprStr is not None else $altStr"
        }
        is NullableMap -> {
            val exprStr = expression.emit()
            val bodyStr = body.emitWithInlinedIt(exprStr)
            val altStr = alternative.emit()
            "$bodyStr if $exprStr is not None else $altStr"
        }
        is NullableOf -> expression.emit()
        is ErrorStatement -> "_raise(${message.emit()})"
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Python")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Python")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Python")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Python")
    }

    private fun Expression.emitWithInlinedIt(replacement: String): String = when (this) {
        is VariableReference -> if (name == "it") replacement else emit()
        is FunctionCall -> {
            val recv = receiver
            val inlinedArgs = arguments.mapValues { it.value.emitWithInlinedIt(replacement) }
            if (recv != null) {
                "${recv.emitWithInlinedIt(replacement)}.$name(${inlinedArgs.values.joinToString(", ")})"
            } else {
                "$name(${inlinedArgs.map { "${it.key}=${it.value}" }.joinToString(", ")})"
            }
        }
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emitWithInlinedIt(replacement)}." } ?: ""
            "$receiverStr$field"
        }
        is ArrayIndexCall -> "${receiver.emitWithInlinedIt(replacement)}[${index.emitWithInlinedIt(replacement)}]"
        is EnumValueCall -> "${expression.emitWithInlinedIt(replacement)}.value"
        else -> emit()
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
