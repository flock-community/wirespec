package community.flock.wirespec.ir.generator

import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.AssertStatement
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.Constraint
import community.flock.wirespec.ir.core.Constructor
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Enum
import community.flock.wirespec.ir.core.EnumReference
import community.flock.wirespec.ir.core.EnumValueCall
import community.flock.wirespec.ir.core.ErrorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FlatMapIndexed
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.IfExpression
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.ListConcat
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.LiteralMap
import community.flock.wirespec.ir.core.Main
import community.flock.wirespec.ir.core.MapExpression
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.NotExpression
import community.flock.wirespec.ir.core.NullCheck
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.NullableEmpty
import community.flock.wirespec.ir.core.NullableGet
import community.flock.wirespec.ir.core.NullableMap
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.PrintStatement
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.StringTemplate
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Function as AstFunction

object PythonGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> element.emit(0)
        else -> File(Name.of(""), listOf(element)).emit(0)
    }

    private fun String.indentCode(level: Int): String = " ".repeat(level * 4) + this

    private fun File.emit(indent: Int): String {
        val allUnions = elements.flatMap { it.findAllUnions() }
        return groupImports(elements).joinToString("") { it.emit(indent, allUnions = allUnions) }.removeEmptyLines()
    }

    private fun groupImports(elements: List<Element>): List<Element> {
        val result = mutableListOf<Element>()
        var i = 0
        while (i < elements.size) {
            val element = elements[i]
            if (element is Import && element.path != ".") {
                val path = element.path
                val types = mutableListOf(element.type.name)
                while (i + 1 < elements.size) {
                    val next = elements[i + 1]
                    if (next is Import && next.path == path) {
                        types.add(next.type.name)
                        i++
                    } else {
                        break
                    }
                }
                result.add(RawElement("from $path import ${types.joinToString(", ")}"))
            } else {
                result.add(element)
            }
            i++
        }
        return result
    }

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n").plus("\n")

    private fun Element.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList(), isStaticScope: Boolean = false, qualifier: ((String) -> String)? = null): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> emit(indent, parents, allUnions = allUnions, qualifier = qualifier)
        is AstFunction -> {
            val isInClass = parents.any { it is Struct || it is Interface || it is Namespace }
            val isInInterface = parents.any { it is Interface }
            emit(indent, isInClass = isInClass, isStaticScope = isStaticScope, isInInterface = isInInterface, qualifier = qualifier)
        }
        is Namespace -> emit(indent, parents, allUnions = allUnions)
        is Interface -> emit(indent, parents, allUnions = allUnions, qualifier = qualifier)
        is Union -> emit(indent, parents, allUnions = allUnions)
        is Enum -> emit(indent)
        is Main -> {
            val staticContent = statics.joinToString("") { it.emit(indent, parents, allUnions, isStaticScope, qualifier) }
            val content = body.joinToString("") { it.emit(indent + 1) }
            val asyncPrefix = if (isAsync) "async " else ""
            val runner = if (isAsync) "asyncio.run(main())" else "main()"
            val defBlock = "${asyncPrefix}def main():\n$content\n".indentCode(indent)
            val guard = "if __name__ == \"__main__\":\n${runner.indentCode(1)}\n".indentCode(indent)
            "$staticContent$defBlock$guard"
        }
        is File -> elements.joinToString("") { it.emit(indent, parents, allUnions, isStaticScope, qualifier) }
        is RawElement -> "$code\n".indentCode(indent)
    }

    private fun Element.findAllUnions(): List<Union> {
        val result = mutableListOf<Union>()
        if (this is Union) result.add(this)
        when (this) {
            is Struct -> result.addAll(elements.flatMap { it.findAllUnions() })
            is Namespace -> result.addAll(elements.flatMap { it.findAllUnions() })
            is Interface -> result.addAll(elements.flatMap { it.findAllUnions() })
            is Main -> {}
            else -> {}
        }
        return result
    }

    private fun Package.emit(indent: Int): String = "# package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "from $path import ${type.name}\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }

        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val siblingNames = elements.mapNotNull { elementName(it) }.toSet()
        val nameStr = name.pascalCase()
        val nsQualifier: ((String) -> String)? = if (siblingNames.isNotEmpty()) {
            { typeName -> if (typeName in siblingNames) "$nameStr.$typeName" else typeName }
        } else {
            null
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = true, qualifier = nsQualifier) }
        val content = elementsContent.ifEmpty { "pass\n".indentCode(indent + 1) }
        return "class $nameStr$ext:\n$content\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList(), qualifier: ((String) -> String)? = null): String {
        val p = extends.map { it.emit() }.toMutableList()
        p.add("ABC")
        if (typeParameters.isNotEmpty()) {
            p.add("Generic[${typeParameters.joinToString(", ") { it.type.emit() }}]")
        }
        val ext = if (p.isEmpty()) "" else "(${p.joinToString(", ")})"
        val nestedNames = elements.mapNotNull { elementName(it) }.toSet()
        val adjustedQualifier = if (qualifier != null && nestedNames.isNotEmpty()) {
            { name: String -> if (name in nestedNames) name else qualifier(name) }
        } else {
            qualifier
        }
        val fieldsContent = fields.joinToString("") { field ->
            "${field.name.value()}: ${field.type.emit(adjustedQualifier)}\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = false, qualifier = adjustedQualifier) }
        val content = (fieldsContent + elementsContent).ifEmpty { "pass\n".indentCode(indent + 1) }
        return "class ${name.pascalCase()}$ext:\n$content\n".indentCode(indent)
    }

    private fun Union.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val p = mutableListOf<String>()
        extends?.let { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name.pascalCase()) }
        if (typeParameters.isNotEmpty()) {
            p.add("Generic[${typeParameters.joinToString(", ") { it.type.emit() }}]")
        }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        return "class ${name.pascalCase()}$ext:\n${"pass".indentCode(indent + 1)}\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val ext = if (extends != null) "(${extends!!.emit()}, enum.Enum)" else "(enum.Enum)"
        val entriesStr = if (entries.isEmpty()) {
            "pass".indentCode(indent + 1)
        } else {
            entries.joinToString("\n") { entry ->
                val value = entry.values.firstOrNull() ?: "\"${entry.name.value()}\""
                "${entry.name.value()} = $value".indentCode(indent + 1)
            }
        }
        return "class ${name.pascalCase()}$ext:\n$entriesStr\n\n".indentCode(indent)
    }

    private fun Struct.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList(), qualifier: ((String) -> String)? = null): String {
        val p = mutableListOf<String>()
        interfaces.forEach { p.add(it.emit()) }
        parents.filterIsInstance<Union>().forEach { p.add(it.name.pascalCase()) }
        allUnions.filter { it.members.any { m -> m.name == this.name.pascalCase() } }.forEach { p.add(it.name.pascalCase()) }

        val ext = if (p.isEmpty()) "" else "(${p.distinct().joinToString(", ")})"
        val nestedContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = false, qualifier = qualifier) }
        val content = if (fields.isEmpty() && constructors.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            val fieldDecls = fields.joinToString("") { it.emit(indent + 1, qualifier) }
            val customConstructors = constructors.joinToString("") { it.emit(indent + 1, qualifier) }
            "$fieldDecls$customConstructors"
        }

        val decorator = "@dataclass\n".indentCode(indent)
        return decorator + "class ${name.pascalCase()}$ext:\n$content$nestedContent\n".indentCode(indent)
    }

    private fun Constructor.emit(indent: Int, qualifier: ((String) -> String)? = null): String {
        val content = if (body.isEmpty()) {
            "pass\n".indentCode(indent + 1)
        } else {
            body.joinToString("") { stmt ->
                when (stmt) {
                    is Assignment -> "self.${stmt.name.value()} = ${stmt.value.emit()}\n".indentCode(indent + 1)
                    else -> stmt.emit(indent + 1).replace("this.", "self.")
                }
            }
        }
        if (parameters.isEmpty()) {
            return "def __init__(self):\n$content\n".indentCode(indent)
        }
        val selfParam = "self,\n".indentCode(indent + 1)
        val paramLines = parameters.joinToString(",\n") {
            "${it.name.camelCase()}: ${it.type.emit(qualifier)}".indentCode(indent + 1)
        }
        val closeParen = "):\n".indentCode(indent)
        return "def __init__(\n$selfParam$paramLines,\n$closeParen$content\n".indentCode(indent)
    }

    private fun Field.emit(indent: Int, qualifier: ((String) -> String)? = null): String = "${name.value()}: ${type.emit(qualifier)}\n".indentCode(indent)

    private fun AstFunction.emit(indent: Int, isInClass: Boolean = false, isStaticScope: Boolean = false, isInInterface: Boolean = false, qualifier: ((String) -> String)? = null): String {
        val params = parameters.joinToString(", ") {
            if (it.name.camelCase() == "self") it.name.camelCase() else "${it.name.camelCase()}: ${it.type.emit(qualifier)}"
        }
        val effectivelyStatic = isStatic || isStaticScope
        val selfPrefix = if (isInClass && !effectivelyStatic && parameters.none { it.name.camelCase() == "self" }) {
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
        val returnAnnotation = returnType?.let { " -> ${it.emit(qualifier)}" } ?: ""
        return staticDecorator + abstractDecorator + "${prefix}def ${name.value()}($selfPrefix$params)$returnAnnotation:\n$content\n".indentCode(indent)
    }

    private fun Type.emit(qualifier: ((String) -> String)? = null): String = when (this) {
        is Type.Integer -> "int"
        is Type.Number -> "float"
        Type.Any -> "Any"
        Type.String -> "str"
        Type.Boolean -> "bool"
        Type.Bytes -> "bytes"
        Type.Unit -> "None"
        Type.Wildcard -> "Any"
        Type.Reflect -> "type[T]"
        is Type.Array -> "list[${elementType.emit(qualifier)}]"
        is Type.Dict -> "dict[${keyType.emit(qualifier)}, ${valueType.emit(qualifier)}]"
        is Type.Custom -> {
            val qualifiedName = qualifier?.invoke(name) ?: name
            if (generics.isEmpty()) {
                qualifiedName
            } else {
                "$qualifiedName[${generics.joinToString(", ") { it.emit(qualifier) }}]"
            }
        }
        is Type.Nullable -> "Optional[${type.emit(qualifier)}]"
        is Type.IntegerLiteral -> "int"
        is Type.StringLiteral -> "str"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "print(${expression.emit()})\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()}\n".indentCode(indent)
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "None\n".indentCode(indent)
            } else {
                val allArgs = namedArguments.map { "${it.key.value()}=${it.value.emit()}" }
                "${type.emit()}(${allArgs.joinToString(", ")})\n".indentCode(indent)
            }
        }
        is Literal -> "${emit()}\n".indentCode(indent)
        is LiteralList -> "${emit()}\n".indentCode(indent)
        is LiteralMap -> "${emit()}\n".indentCode(indent)
        is Assignment -> "${name.camelCase()} = ${value.emit()}\n".indentCode(indent)
        is ErrorStatement -> "raise Exception(${message.emit()})\n".indentCode(indent)
        is AssertStatement -> "assert ${expression.emit()}, '${message.replace("'", "\\'")}'\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                    val typeStr = case.type?.emit() ?: "object"
                    val varBinding = variable?.let { " as ${it.camelCase()}" } ?: ""
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
        is RawExpression -> "$code\n".indentCode(indent)
        is NullLiteral -> "None\n".indentCode(indent)
        is NullableEmpty -> "None\n".indentCode(indent)
        is VariableReference -> "${name.camelCase()}\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value()}\n".indentCode(indent)
        }
        is FunctionCall -> {
            val awaitPrefix = if (isAwait) "await " else ""
            val recv = receiver
            if (recv != null) {
                "$awaitPrefix${recv.emit()}.${name.value()}(${arguments.values.joinToString(", ") { it.emit() }})\n".indentCode(indent)
            } else {
                "$awaitPrefix${name.value()}(${arguments.map { "${it.key.value()}=${it.value.emit()}" }.joinToString(", ")})\n".indentCode(indent)
            }
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}]\n".indentCode(indent)
        } else {
            "next((v for k, v in ${receiver.emit()}.items() if k.lower() == ${index.emit()}.lower()), None)\n".indentCode(indent)
        }
        is EnumReference -> "${enumType.emit()}.${entry.value()}\n".indentCode(indent)
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
        is NullableGet -> "${emit()}\n".indentCode(indent)
        is Constraint.RegexMatch -> "${emit()}\n".indentCode(indent)
        is Constraint.BoundCheck -> "${emit()}\n".indentCode(indent)
        is NotExpression -> "not ${expression.emit()}\n".indentCode(indent)
        is IfExpression -> "${emit()}\n".indentCode(indent)
        is MapExpression -> "${emit()}\n".indentCode(indent)
        is FlatMapIndexed -> "${emit()}\n".indentCode(indent)
        is ListConcat -> "${emit()}\n".indentCode(indent)
        is StringTemplate -> "${emit()}\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toPython(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> if (type == Type.Unit) "None" else "${type.emit()}(${namedArguments.map { "${it.key.value()}=${it.value.emit()}" }.joinToString(", ")})"
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "None"
        is NullableEmpty -> "None"
        is VariableReference -> name.camelCase()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value()}"
        }
        is FunctionCall -> {
            val awaitPrefix = if (isAwait) "await " else ""
            val recv = receiver
            if (recv != null) {
                "$awaitPrefix${recv.emit()}.${name.value()}(${arguments.values.joinToString(", ") { it.emit() }})"
            } else {
                "$awaitPrefix${name.value()}(${arguments.map { "${it.key.value()}=${it.value.emit()}" }.joinToString(", ")})"
            }
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}]"
        } else {
            "next((v for k, v in ${receiver.emit()}.items() if k.lower() == ${index.emit()}.lower()), None)"
        }
        is EnumReference -> "${enumType.emit()}.${entry.value()}"
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
        is NullableGet -> expression.emit()
        is Constraint.RegexMatch -> "bool(re.match(r\"${rawValue}\", ${value.emit()}))"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" and ").ifEmpty { "True" }
            checks
        }
        is ErrorStatement -> "_raise(${message.emit()})"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Python")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Python")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Python")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Python")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Python")
        is NotExpression -> "not ${expression.emit()}"
        is IfExpression -> "(${thenExpr.emit()} if ${condition.emit()} else ${elseExpr.emit()})"
        is MapExpression -> "[${body.emit()} for ${variable.camelCase()} in ${receiver.emit()}]"
        is FlatMapIndexed -> "[item for ${indexVar.camelCase()}, ${elementVar.camelCase()} in enumerate(${receiver.emit()}) for item in ${body.emit()}]"
        is ListConcat -> when {
            lists.isEmpty() -> "[]"
            lists.size == 1 -> lists.single().emit()
            else -> lists.joinToString(" + ") { it.emit() }
        }
        is StringTemplate -> "f\"${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "{${it.expression.emit()}}"
            }
        }}\""
    }

    private fun Expression.emitWithInlinedIt(replacement: String): String = when (this) {
        is VariableReference -> if (name.value() == "it") replacement else emit()
        is FunctionCall -> {
            val recv = receiver
            val inlinedArgs = arguments.mapValues { it.value.emitWithInlinedIt(replacement) }
            if (recv != null) {
                "${recv.emitWithInlinedIt(replacement)}.${name.value()}(${inlinedArgs.values.joinToString(", ")})"
            } else {
                "${name.value()}(${inlinedArgs.map { "${it.key.value()}=${it.value}" }.joinToString(", ")})"
            }
        }
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emitWithInlinedIt(replacement)}." } ?: ""
            "$receiverStr${field.value()}"
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emitWithInlinedIt(replacement)}[${index.emitWithInlinedIt(replacement)}]"
        } else {
            "next((v for k, v in ${receiver.emitWithInlinedIt(replacement)}.items() if k.lower() == ${index.emitWithInlinedIt(replacement)}.lower()), None)"
        }
        is EnumValueCall -> "${expression.emitWithInlinedIt(replacement)}.value"
        is NotExpression -> "not ${expression.emitWithInlinedIt(replacement)}"
        is IfExpression -> "(${thenExpr.emitWithInlinedIt(replacement)} if ${condition.emitWithInlinedIt(replacement)} else ${elseExpr.emitWithInlinedIt(replacement)})"
        is MapExpression -> "[${body.emitWithInlinedIt(replacement)} for ${variable.camelCase()} in ${receiver.emitWithInlinedIt(replacement)}]"
        is FlatMapIndexed -> "[item for ${indexVar.camelCase()}, ${elementVar.camelCase()} in enumerate(${receiver.emitWithInlinedIt(replacement)}) for item in ${body.emitWithInlinedIt(replacement)}]"
        is ListConcat -> lists.joinToString(" + ") { it.emitWithInlinedIt(replacement) }
        is StringTemplate -> "f\"${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "{${it.expression.emitWithInlinedIt(replacement)}}"
            }
        }}\""
        is LiteralList -> emit()
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

    private fun elementName(element: Element): String? = when (element) {
        is Interface -> element.name.pascalCase()
        is Struct -> element.name.pascalCase()
        is Enum -> element.name.pascalCase()
        is Union -> element.name.pascalCase()
        is Namespace -> element.name.pascalCase()
        else -> null
    }
}
