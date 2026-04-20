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
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.PrintStatement
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.StringTemplate
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.ThisExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Function as AstFunction

object KotlinGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> {
            val emitter = KotlinEmitter(element)
            emitter.emitFile()
        }

        else -> {
            val emitter = KotlinEmitter(File(Name.of(""), listOf(element)))
            emitter.emitFile()
        }
    }
}

private class KotlinEmitter(val file: File) {

    fun emitFile(): String {
        val packages = file.elements.filterIsInstance<Package>()
        val imports = file.elements.filterIsInstance<Import>()
        val otherElements = file.elements.filter { it !is Package && it !is Import }

        val packagesStr = packages.joinToString("") { it.emit(0) }
        val importsStr = imports.joinToString("") { it.emit(0) }
        val elementsStr = otherElements.joinToString("") { it.emit(0, parents = emptyList()) }

        return "$packagesStr\n$importsStr\n$elementsStr".removeEmptyLines()
    }

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n").plus("\n")

    private fun String.indentCode(level: Int): String {
        if (level <= 0) return this
        val prefix = " ".repeat(level * 2)
        return this.lines().joinToString("\n") { line ->
            if (line.isEmpty()) line else prefix + line
        }
    }

    private fun Element.emit(indent: Int, isStatic: Boolean = false, parents: List<Element>): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> emit(indent, parents)
        is AstFunction -> emit(indent, parents)
        is Namespace -> emit(indent, parents)
        is Interface -> emit(indent, parents)
        is Union -> emit(indent, parents)
        is Enum -> emit(indent)
        is Main -> {
            val staticContent = statics.joinToString("") { it.emit(indent, false, parents) }
            val content = body.joinToString("") { it.emit(1) }
            val modifier = if (isAsync) "suspend " else ""
            "$staticContent${"${modifier}fun main() {\n$content}\n\n".indentCode(indent)}"
        }
        is File -> elements.joinToString("") { it.emit(indent, isStatic, parents) }
        is RawElement -> "$code\n".indentCode(indent)
    }

    private fun Package.emit(indent: Int): String = "package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path.${type.name.dotted()}\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " : ${it.emitGenerics()}" } ?: ""
        val content = elements.joinToString("") { it.emit(indent + 1, isStatic = true, parents = parents + this) }
        return "object ${name.pascalCase()}$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val sealedStr = if (isSealed) "sealed " else ""
        val typeParamsStr =
            if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extStr = if (extends.isNotEmpty()) " : ${extends.joinToString(", ") { it.emitGenerics() }}" else ""
        val fieldsContent = fields.joinToString("") { field ->
            val overridePrefix = if (field.isOverride) "override " else ""
            "${overridePrefix}val ${field.name.value()}: ${field.type.emitGenerics()}\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, isStatic = false, parents = parents + this) }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "${sealedStr}interface ${name.pascalCase()}$typeParamsStr$extStr\n\n".indentCode(indent)
        } else {
            "${sealedStr}interface ${name.pascalCase()}$typeParamsStr$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int, parents: List<Element>): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extStr = extends?.let { " : ${it.emitGenerics()}" } ?: ""
        return "sealed interface ${name.pascalCase()}$typeParamsStr$extStr\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val entriesStr = entries.joinToString(",\n") { entry ->
            val e = if (entry.values.isEmpty()) {
                entry.name.value()
            } else {
                "${entry.name.value()}(${entry.values.joinToString(", ")})"
            }
            e.indentCode(indent + 1)
        }
        val hasContent = fields.isNotEmpty() || constructors.isNotEmpty() || elements.isNotEmpty()
        val terminator = if (hasContent) ";\n" else ""

        val constructorParamsStr = if (fields.isNotEmpty()) {
            " (${fields.joinToString(", ") { "${if (it.isOverride) "override " else ""}val ${it.name.value()}: ${it.type.emitGenerics()}" }})"
        } else {
            ""
        }

        val implStr =
            extends?.let { "${if (constructorParamsStr.isNotEmpty()) "" else " "}: ${it.emitGenerics()}" } ?: ""

        val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
            val overridePrefix = if (it.isOverride || it.name.camelCase() == "toString") "override " else ""
            it.emitAsMethod(indent + 1, overridePrefix)
        }

        val content = listOf(functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
        val sep = if (content.isNotEmpty()) "\n" else ""

        return (
            "enum class ${name.pascalCase()}$constructorParamsStr$implStr {\n$entriesStr$terminator$sep$content\n${
                "}".indentCode(
                    indent,
                )
            }".indentCode(indent)
            ).trimEnd()
    }

    private fun AstFunction.emitAsMethod(indent: Int, prefix: String): String {
        val rType = returnType?.takeIf { it != Type.Unit }?.emitGenerics() ?: "Unit"
        val params = parameters.joinToString(", ") { it.emit(0) }
        return if (body.isEmpty()) {
            "${prefix}fun ${name.camelCase()}($params): $rType".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "${prefix}fun ${name.camelCase()}($params): $rType {\n$content${"}".indentCode(0)}\n".indentCode(indent)
        }
    }

    private fun Struct.emit(indent: Int, parents: List<Element>): String {
        val implStr = if (interfaces.isEmpty()) "" else " : ${interfaces.map { it.emitGenerics() }.distinct().joinToString(", ")}"

        val nestedContent = elements.joinToString("") { it.emit(indent + 1, isStatic = true, parents = parents + this) }
        val customConstructors = constructors.joinToString("") { it.emitKotlin(fields, indent + 1) }

        if (constructors.size == 1 && constructors.single().parameters.isEmpty()) {
            val constructor = constructors.single()
            val assignments = constructor.body.filterIsInstance<Assignment>()
            val fieldProperties = fields.joinToString("\n") { field ->
                val assignment = assignments.find { it.name.camelCase() == field.name.value() }
                val valueStr = assignment?.let { " = ${it.value.emit()}" } ?: ""
                "${if (field.isOverride) "override " else ""}val ${field.name.value().sanitize()}: ${field.type.emitGenerics()}$valueStr".indentCode(indent + 1)
            }
            val bodyContent = listOf(fieldProperties, nestedContent).filter { it.isNotEmpty() }.joinToString("\n")
            return if (bodyContent.isNotEmpty()) {
                "data object ${name.pascalCase()}$implStr {\n$bodyContent${"}".indentCode(indent)}\n\n".indentCode(indent)
            } else {
                "data object ${name.pascalCase()}$implStr\n\n".indentCode(indent)
            }
        }

        if (fields.isEmpty() && constructors.isEmpty()) {
            return if (nestedContent.isNotEmpty()) {
                "object ${name.pascalCase()}$implStr {\n$nestedContent${"}".indentCode(indent)}\n\n".indentCode(indent)
            } else {
                "object ${name.pascalCase()}$implStr\n\n".indentCode(indent)
            }
        }

        val params = fields.joinToString(",\n") {
            "${if (it.isOverride) "override " else ""}val ${it.name.value().sanitize()}: ${it.type.emitGenerics()}".indentCode(
                indent + 1,
            )
        }
        val paramsStr = if (fields.isEmpty()) "" else "(\n$params\n${")".indentCode(indent)}"

        val hasBody = customConstructors.isNotEmpty() || nestedContent.isNotEmpty()

        return if (hasBody) {
            "data class ${name.pascalCase()}$paramsStr$implStr {\n$customConstructors$nestedContent${"}".indentCode(indent)}\n\n".indentCode(
                indent,
            )
        } else {
            "data class ${name.pascalCase()}$paramsStr$implStr\n\n".indentCode(indent)
        }
    }

    private fun Constructor.emitKotlin(structFields: List<Field>, indent: Int): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val isDelegating = body.any { it is ConstructorStatement }

        if (isDelegating) {
            val delegationStmt = body.filterIsInstance<ConstructorStatement>().first()
            val delegationArgs = delegationStmt.namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
            val delegationStr = "this(${delegationArgs.joinToString(", ")})"
            val otherStatements = body.filter { it !is ConstructorStatement }
            return if (otherStatements.isEmpty()) {
                "constructor($params) : $delegationStr\n".indentCode(indent)
            } else {
                val bodyContent = otherStatements.joinToString("") { it.emit(1) }
                "constructor($params) : $delegationStr {\n$bodyContent${"}".indentCode(0)}\n".indentCode(indent)
            }
        }

        val assignments = body.filterIsInstance<Assignment>().associate {
            it.name.camelCase() to it.value.emit()
        }
        val constructorArgs = structFields.map { field ->
            assignments[field.name.value()] ?: "null"
        }
        val otherStatements = body.filter {
            it !is Assignment || it.name.camelCase() !in structFields.map { f -> f.name.value() }
        }

        return if (otherStatements.isEmpty()) {
            "constructor($params) : this(${constructorArgs.joinToString(", ")})\n".indentCode(indent)
        } else {
            val bodyContent = otherStatements.joinToString("") { it.emit(1) }
            "constructor($params) : this(${constructorArgs.joinToString(", ")}) {\n$bodyContent${"}".indentCode(0)}\n".indentCode(
                indent,
            )
        }
    }

    private fun AstFunction.emit(indent: Int, parents: List<Element>): String {
        val lastParent = parents.lastOrNull()
        val isInterface = lastParent is Interface

        val overridePrefix = if (isOverride) "override " else ""
        val suspendPrefix = if (isAsync) "suspend " else ""
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { it.emit() }}> "
        } else {
            ""
        }
        val rType = if (isAsync) {
            returnType?.emitGenerics() ?: "Unit"
        } else {
            returnType?.takeIf { it != Type.Unit }?.emitGenerics()
        }
        val returnTypeStr = if (rType != null) ": $rType" else ""
        val params = parameters.joinToString(", ") { it.emit(0) }

        return if (body.isEmpty()) {
            "$overridePrefix${suspendPrefix}fun $typeParamsStr${name.camelCase()}($params)$returnTypeStr\n".indentCode(indent)
        } else if (body.size == 1 && body.first() is ReturnStatement) {
            val expr = (body.first() as ReturnStatement).expression.emit()
            "$overridePrefix${suspendPrefix}fun $typeParamsStr${name.camelCase()}($params)$returnTypeStr =\n${expr.indentCode(1)}\n\n".indentCode(
                indent,
            )
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "$overridePrefix${suspendPrefix}fun $typeParamsStr${name.camelCase()}($params)$returnTypeStr {\n$content${"}".indentCode(0)}\n\n".indentCode(
                indent,
            )
        }
    }

    private fun Parameter.emit(indent: Int): String = "${name.camelCase().sanitize()}: ${type.emitGenerics()}".indentCode(indent)

    private fun TypeParameter.emit(): String {
        val typeStr = type.emitGenerics()
        return if (extends.isEmpty()) {
            "$typeStr: Any"
        } else {
            "$typeStr: ${extends.joinToString(" & ") { it.emitGenerics() }}"
        }
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "Int"
            Precision.P64 -> "Long"
        }

        is Type.Number -> when (precision) {
            Precision.P32 -> "Float"
            Precision.P64 -> "Double"
        }

        Type.Any -> "Any"
        Type.String -> "String"
        Type.Bytes -> "ByteArray"
        Type.Boolean -> "Boolean"
        Type.Unit -> "Unit"
        Type.Wildcard -> "*"
        Type.Reflect -> "KType"
        is Type.Array -> "List"
        is Type.Dict -> "Map"
        is Type.Custom -> name.dotted()
        is Type.Nullable -> "${type.emitGenerics()}?"
        is Type.IntegerLiteral -> "Int"
        is Type.StringLiteral -> "String"
    }

    private fun Type.emitGenerics(): String = when (this) {
        is Type.Array -> "${emit()}<${elementType.emitGenerics()}>"
        is Type.Dict -> "${emit()}<${keyType.emitGenerics()}, ${valueType.emitGenerics()}>"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                emit()
            } else {
                "${emit()}<${generics.joinToString(", ") { it.emitGenerics() }}>"
            }
        }

        is Type.Nullable -> "${type.emitGenerics()}?"
        else -> emit()
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "println(${expression.emit()})\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()}\n".indentCode(indent)
        is ConstructorStatement -> {
            val allArgs = namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
            val argsStr = when {
                allArgs.isEmpty() -> ""
                allArgs.size == 1 -> "(${allArgs.first()})"
                else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
            }
            "${type.emitGenerics()}$argsStr\n".indentCode(indent)
        }

        is Literal -> "${emit()}\n".indentCode(indent)
        is LiteralList -> "${emit()}\n".indentCode(indent)
        is LiteralMap -> "${emit()}\n".indentCode(indent)
        is Assignment -> {
            val expr = (value as? ConstructorStatement)?.let { constructorStmt ->
                val allArgs = constructorStmt.namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
                val argsStr = when {
                    allArgs.isEmpty() -> ""
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                "${constructorStmt.type.emitGenerics()}$argsStr"
            } ?: value.emit()
            if (isProperty) {
                "${name.value().sanitize()} = $expr\n".indentCode(indent)
            } else {
                "val ${name.camelCase().sanitize()} = $expr\n".indentCode(indent)
            }
        }

        is ErrorStatement -> "error(${message.emit()})\n".indentCode(indent)
        is AssertStatement -> "assert(${expression.emit()}) { \"$message\" }\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Any"
                    "is $typeStr -> {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "else -> {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                val exprStr = variable?.let { "val ${it.camelCase()} = ${expression.emit()}" } ?: expression.emit()
                "when($exprStr) {\n$casesStr$defaultStr}\n".indentCode(indent)
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    "${case.value.emit()} -> {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "else -> {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "when (${expression.emit()}) {\n$casesStr$defaultStr}\n".indentCode(indent)
            }
        }

        is RawExpression -> "$code\n".indentCode(indent)
        is NullLiteral -> "null\n".indentCode(indent)
        is NullableEmpty -> "null\n".indentCode(indent)
        is ThisExpression -> "this\n".indentCode(indent)
        is VariableReference -> "${name.camelCase().sanitize()}\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}\n".indentCode(indent)
        }

        is FunctionCall -> {
            val typeArgsStr =
                if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${
                name.value().toKotlinStaticCall().sanitize()
            }$typeArgsStr(${arguments.values.joinToString(", ") { it.emit() }})\n".indentCode(indent)
        }

        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}]\n".indentCode(indent)
        } else {
            "${receiver.emit()}.entries.find { it.key.equals(${index.emit()}, ignoreCase = true) }?.value\n".indentCode(indent)
        }

        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()}\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()}.name\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toKotlin()} ${right.emit()})\n".indentCode(indent)
        is TypeDescriptor -> "${emitTypeDescriptor()}\n".indentCode(indent)
        is NullCheck -> "${emit()}\n".indentCode(indent)
        is NullableMap -> "${emit()}\n".indentCode(indent)
        is NullableOf -> "${emit()}\n".indentCode(indent)
        is NullableGet -> "${emit()}\n".indentCode(indent)
        is Constraint.RegexMatch -> "${emit()}\n".indentCode(indent)
        is Constraint.BoundCheck -> "${emit()}\n".indentCode(indent)
        is NotExpression -> "!${expression.emit()}\n".indentCode(indent)
        is IfExpression -> "${emit()}\n".indentCode(indent)
        is MapExpression -> "${emit()}\n".indentCode(indent)
        is FlatMapIndexed -> "${emit()}\n".indentCode(indent)
        is ListConcat -> "${emit()}\n".indentCode(indent)
        is StringTemplate -> "${emit()}\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toKotlin(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun String.toKotlinStaticCall(): String = when (this) {
        "java.util.Collections.emptyList" -> "emptyList"
        "java.util.Collections.emptyMap" -> "emptyMap"
        else -> this
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                type.emitGenerics()
            } else {
                val allArgs = namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
                val argsStr = when {
                    allArgs.isEmpty() -> ""
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                "${type.emitGenerics()}$argsStr"
            }
        }

        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "null"
        is NullableEmpty -> "null"
        is ThisExpression -> "this"
        is VariableReference -> name.camelCase().sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}"
        }

        is FunctionCall -> {
            val typeArgsStr =
                if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${
                name.value().toKotlinStaticCall().sanitize()
            }$typeArgsStr(${arguments.values.joinToString(", ") { it.emit() }})"
        }

        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}]"
        } else {
            "${receiver.emit()}.entries.find { it.key.equals(${index.emit()}, ignoreCase = true) }?.value"
        }

        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()}"
        is EnumValueCall -> "${expression.emit()}.name"
        is BinaryOp -> "(${left.emit()} ${operator.toKotlin()} ${right.emit()})"
        is TypeDescriptor -> emitTypeDescriptor()
        is NullCheck -> "(${expression.emit()}?.let { ${body.emit()} }${alternative?.emit()?.let { " ?: $it" } ?: ""})"
        is NullableMap -> "(${expression.emit()}?.let { ${body.emit()} } ?: ${alternative.emit()})"
        is NullableOf -> expression.emit()
        is NullableGet -> "${expression.emit()}!!"
        is Constraint.RegexMatch -> "Regex(\"\"\"${pattern}\"\"\").matches(${value.emit()})"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" && ").ifEmpty { "true" }
            checks
        }
        is ErrorStatement -> "error(${message.emit()})"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Kotlin")
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Any"
                    "is $typeStr -> {\n$bodyStr}\n".indentCode(1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "else -> {\n$bodyStr}\n".indentCode(1)
                } ?: ""
                val exprStr = variable?.let { "val ${it.camelCase()} = ${expression.emit()}" } ?: expression.emit()
                "when($exprStr) {\n$casesStr$defaultStr}"
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    "${case.value.emit()} -> {\n$bodyStr}\n".indentCode(1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "else -> {\n$bodyStr}\n".indentCode(1)
                } ?: ""
                "when (${expression.emit()}) {\n$casesStr$defaultStr}"
            }
        }

        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Kotlin")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Kotlin")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Kotlin")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "if (${condition.emit()}) ${thenExpr.emit()} else ${elseExpr.emit()}"
        is MapExpression -> "${receiver.emit()}.map { ${variable.camelCase()} -> ${body.emit()} }"
        is FlatMapIndexed -> "${receiver.emit()}.flatMapIndexed { ${indexVar.camelCase()}, ${elementVar.camelCase()} -> ${body.emit()} }"
        is ListConcat -> when {
            lists.isEmpty() -> "emptyList<String>()"
            lists.size == 1 -> lists.single().emit()
            else -> lists.joinToString(" + ") { expr ->
                val emitted = expr.emit()
                if (expr is IfExpression) "($emitted)" else emitted
            }
        }
        is StringTemplate -> "\"${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "\${${it.expression.emit()}}"
            }
        }}\""
    }

    private fun LiteralList.emit(): String {
        if (values.isEmpty()) return "emptyList<${type.emitGenerics()}>()"
        val list = values.joinToString(", ") { it.emit() }
        return "listOf($list)"
    }

    private fun LiteralMap.emit(): String {
        if (values.isEmpty()) return "emptyMap()"
        val map = values.entries.joinToString(", ") {
            "${Literal(it.key, keyType).emit()} to ${it.value.emit()}"
        }
        return "mapOf($map)"
    }

    private fun Literal.emit(): String = when (type) {
        Type.String -> "\"$value\""
        else -> value.toString()
    }

    private fun TypeDescriptor.emitTypeDescriptor(): String = "typeOf<${type.emitGenerics()}>()"
}

private fun String.sanitize(): String = if (reservedKeywords.contains(this)) "`$this`" else this

private val reservedKeywords = setOf(
    "as", "break", "class", "continue", "do",
    "else", "false", "for", "fun", "if",
    "in", "interface", "internal", "is", "null",
    "object", "open", "package", "return", "super",
    "this", "throw", "true", "try", "typealias",
    "typeof", "val", "var", "when", "while",
)
