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

object ScalaGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> {
            val emitter = ScalaEmitter(element)
            emitter.emitFile()
        }

        else -> {
            val emitter = ScalaEmitter(File(Name.of(""), listOf(element)))
            emitter.emitFile()
        }
    }
}

private class
ScalaEmitter(
    val file: File,
) {
    private val objectNames = collectObjectNames(file.elements)
    private val primaryFieldNames = collectPrimaryFieldNames(file.elements)

    private fun collectObjectNames(elements: List<Element>): Set<String> {
        val names = mutableSetOf<String>()
        for (element in elements) {
            when (element) {
                is Struct -> {
                    val isObject = (element.constructors.size == 1 && element.constructors.single().parameters.isEmpty()) ||
                        (element.fields.isEmpty() && element.constructors.isEmpty())
                    if (isObject && !element.isModelStruct()) names.add(element.name.pascalCase())
                    names.addAll(collectObjectNames(element.elements))
                }
                is Namespace -> names.addAll(collectObjectNames(element.elements))
                is Interface -> names.addAll(collectObjectNames(element.elements))
                else -> {}
            }
        }
        return names
    }

    private fun Struct.isModelStruct(): Boolean = interfaces.any { it.name.dotted() == "Wirespec.Model" }

    private fun collectPrimaryFieldNames(elements: List<Element>): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()
        for (element in elements) {
            when (element) {
                is Struct -> {
                    result[element.name.pascalCase()] = element.fields.map { it.name.value() }.toSet()
                    result.putAll(collectPrimaryFieldNames(element.elements))
                }
                is Namespace -> result.putAll(collectPrimaryFieldNames(element.elements))
                is Interface -> result.putAll(collectPrimaryFieldNames(element.elements))
                else -> {}
            }
        }
        return result
    }

    private fun ConstructorStatement.needsNew(): Boolean {
        val typeName = (type as? Type.Custom)?.name?.dotted() ?: return false
        if (namedArguments.isEmpty()) return false
        // Default to true for unknown types - `new CaseClass(args)` is always valid in Scala
        val primaryFields = primaryFieldNames[typeName] ?: return true
        val argNames = namedArguments.keys.map { it.value() }.toSet()
        return argNames != primaryFields
    }

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
            val staticContent = statics.joinToString("") { it.emit(1, false, parents) }
            val content = body.joinToString("") { it.emit(1) }
            "object ${file.name.pascalCase()} {\n$staticContent  def main(args: Array[String]): Unit = {\n$content  }\n}\n\n".indentCode(indent)
        }
        is File -> elements.joinToString("") { it.emit(indent, isStatic, parents) }
        is RawElement -> "$code\n".indentCode(indent)
    }

    private fun Package.emit(indent: Int): String = "package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path.${type.name.dotted()}\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " extends ${it.emitTypeAnnotation()}" } ?: ""
        val content = elements.joinToString("") { it.emit(indent + 1, isStatic = true, parents = parents + this) }
        return "object ${name.pascalCase()}$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val sealedStr = if (isSealed) "sealed " else ""
        val typeParamsStr =
            if (typeParameters.isNotEmpty()) "[${typeParameters.joinToString(", ") { it.emit() }}]" else ""
        val extStr = if (extends.isNotEmpty()) " extends ${extends.joinToString(" with ") { it.emitTypeAnnotation() }}" else ""
        val fieldsContent = fields.joinToString("") { field ->
            val overridePrefix = if (field.isOverride) "override " else ""
            "${overridePrefix}def ${field.name.value()}: ${field.type.emitTypeAnnotation()}\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, isStatic = false, parents = parents + this) }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "${sealedStr}trait ${name.pascalCase()}$typeParamsStr$extStr\n\n".indentCode(indent)
        } else {
            "${sealedStr}trait ${name.pascalCase()}$typeParamsStr$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int, parents: List<Element>): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) "[${typeParameters.joinToString(", ") { it.emit() }}]" else ""
        val extStr = extends?.let { " extends ${it.emitTypeAnnotation()}" } ?: ""
        return "sealed trait ${name.pascalCase()}$typeParamsStr$extStr\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val implStr = extends?.let { " extends ${it.emitGenerics()}" } ?: ""

        val hasFields = fields.isNotEmpty()

        if (hasFields) {
            val fieldsStr = fields.joinToString(", ") { "${if (it.isOverride) "override " else ""}val ${it.name.value()}: ${it.type.emitGenerics()}" }
            val entriesStr = entries.joinToString(",\n") { entry ->
                val e = if (entry.values.isEmpty()) {
                    "case ${entry.name.value()}"
                } else {
                    "case ${entry.name.value()} extends ${name.pascalCase()}(${entry.values.joinToString(", ")})"
                }
                e.indentCode(indent + 1)
            }
            val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
                val overridePrefix = if (it.isOverride || it.name.camelCase() == "toString") "override " else ""
                it.emitAsMethod(indent + 1, overridePrefix)
            }
            val content = listOf(entriesStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
            return "enum ${name.pascalCase()}($fieldsStr)$implStr {\n$content\n${"}".indentCode(indent)}\n\n".indentCode(indent)
        }

        val entriesStr = entries.joinToString("\n") { entry ->
            "case ${entry.name.value()}".indentCode(indent + 1)
        }
        val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
            val overridePrefix = if (it.isOverride || it.name.camelCase() == "toString") "override " else ""
            it.emitAsMethod(indent + 1, overridePrefix)
        }
        val content = listOf(entriesStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
        return "enum ${name.pascalCase()}$implStr {\n$content\n${"}".indentCode(indent)}\n\n".indentCode(indent)
    }

    private fun AstFunction.emitAsMethod(indent: Int, prefix: String): String {
        val rType = returnType?.takeIf { it != Type.Unit }?.emitTypeAnnotation() ?: "Unit"
        val params = parameters.joinToString(", ") { it.emit(0) }
        return if (body.isEmpty()) {
            "${prefix}def ${name.camelCase()}($params): $rType\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "${prefix}def ${name.camelCase()}($params): $rType = {\n$content${"}".indentCode(0)}\n".indentCode(indent)
        }
    }

    private fun Struct.emit(indent: Int, parents: List<Element>): String {
        val implStr = if (interfaces.isEmpty()) "" else " extends ${interfaces.map { it.emitTypeAnnotation() }.distinct().joinToString(" with ")}"

        val nestedContent = elements.joinToString("") { it.emit(indent + 1, isStatic = true, parents = parents + this) }
        val customConstructors = constructors.joinToString("") { it.emitScala(fields, indent + 1) }

        if (constructors.size == 1 && constructors.single().parameters.isEmpty()) {
            if (isModelStruct()) {
                val bodyContent = listOf(nestedContent).filter { it.isNotEmpty() }.joinToString("\n")
                return if (bodyContent.isNotEmpty()) {
                    "case class ${name.pascalCase()}()$implStr {\n$bodyContent${"}".indentCode(indent)}\n\n".indentCode(indent)
                } else {
                    "case class ${name.pascalCase()}()$implStr\n\n".indentCode(indent)
                }
            }
            val constructor = constructors.single()
            val assignments = constructor.body.filterIsInstance<Assignment>()
            val fieldProperties = fields.joinToString("\n") { field ->
                val assignment = assignments.find { it.name.camelCase() == field.name.value() }
                val valueStr = assignment?.let { " = ${it.value.emit()}" } ?: ""
                "${if (field.isOverride) "override " else ""}val ${field.name.value().sanitize()}: ${field.type.emitTypeAnnotation()}$valueStr".indentCode(indent + 1)
            }
            val bodyContent = listOf(fieldProperties, nestedContent).filter { it.isNotEmpty() }.joinToString("\n")
            return if (bodyContent.isNotEmpty()) {
                "object ${name.pascalCase()}$implStr {\n$bodyContent${"}".indentCode(indent)}\n\n".indentCode(indent)
            } else {
                "object ${name.pascalCase()}$implStr\n\n".indentCode(indent)
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
            "${if (it.isOverride) "override " else ""}val ${it.name.value().sanitize()}: ${it.type.emitTypeAnnotation()}".indentCode(indent + 1)
        }
        val paramsStr = if (fields.isEmpty()) "" else "(\n$params\n${")".indentCode(indent)}"

        val hasBody = customConstructors.isNotEmpty() || nestedContent.isNotEmpty()

        return if (hasBody) {
            "case class ${name.pascalCase()}$paramsStr$implStr {\n$customConstructors$nestedContent${"}".indentCode(indent)}\n\n".indentCode(indent)
        } else {
            "case class ${name.pascalCase()}$paramsStr$implStr\n\n".indentCode(indent)
        }
    }

    private fun Constructor.emitScala(structFields: List<Field>, indent: Int): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val isDelegating = body.any { it is ConstructorStatement }

        if (isDelegating) {
            val delegationStmt = body.filterIsInstance<ConstructorStatement>().first()
            val delegationArgs = delegationStmt.namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
            val delegationStr = "this(${delegationArgs.joinToString(", ")})"
            return "def this($params) = $delegationStr\n".indentCode(indent)
        }

        val assignments = body.filterIsInstance<Assignment>().associate {
            it.name.value() to it.value.emit()
        }
        val constructorArgs = structFields.map { field ->
            assignments[field.name.value()] ?: "null"
        }

        return "def this($params) = this(${constructorArgs.joinToString(", ")})\n".indentCode(indent)
    }

    private fun AstFunction.emit(indent: Int, parents: List<Element>): String {
        val overridePrefix = if (isOverride) "override " else ""
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "[${typeParameters.joinToString(", ") { it.emit() }}]"
        } else {
            ""
        }
        val rType = returnType?.takeIf { it != Type.Unit }?.emitTypeAnnotation() ?: "Unit"
        val returnTypeStr = ": $rType"
        val params = parameters.joinToString(", ") { it.emit(0) }

        return if (body.isEmpty()) {
            "${overridePrefix}def ${name.camelCase()}$typeParamsStr($params)$returnTypeStr\n".indentCode(indent)
        } else if (body.size == 1 && body.first() is ReturnStatement) {
            val expr = (body.first() as ReturnStatement).expression.emit()
            "${overridePrefix}def ${name.camelCase()}$typeParamsStr($params)$returnTypeStr =\n${expr.indentCode(1)}\n\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "${overridePrefix}def ${name.camelCase()}$typeParamsStr($params)$returnTypeStr = {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Parameter.emit(indent: Int): String = "${name.camelCase().sanitize()}: ${type.emitTypeAnnotation()}".indentCode(indent)

    private fun TypeParameter.emit(): String {
        val typeStr = type.emitGenerics()
        return if (extends.isEmpty()) {
            typeStr
        } else {
            "$typeStr <: ${extends.joinToString(" with ") { it.emitGenerics() }}"
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
        Type.Bytes -> "Array[Byte]"
        Type.Boolean -> "Boolean"
        Type.Unit -> "Unit"
        Type.Wildcard -> "?"
        Type.Reflect -> "scala.reflect.ClassTag[?]"
        is Type.Array -> "List"
        is Type.Dict -> "Map"
        is Type.Custom -> name.dotted()
        is Type.Nullable -> "Option[${type.emitGenerics()}]"
        is Type.IntegerLiteral -> "Int"
        is Type.StringLiteral -> "String"
    }

    private fun Type.emitGenerics(): String = when (this) {
        is Type.Array -> "${emit()}[${elementType.emitGenerics()}]"
        is Type.Dict -> "${emit()}[${keyType.emitGenerics()}, ${valueType.emitGenerics()}]"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                emit()
            } else {
                "${emit()}[${generics.joinToString(", ") { it.emitGenerics() }}]"
            }
        }

        is Type.Nullable -> "Option[${type.emitGenerics()}]"
        else -> emit()
    }

    // Emit type for use in type annotation positions (field types, parameter types, return types).
    // Adds .type suffix for Scala singleton object types and recurses into generics.
    private fun Type.emitTypeAnnotation(): String = when (this) {
        is Type.Array -> "List[${elementType.emitTypeAnnotation()}]"
        is Type.Dict -> "Map[${keyType.emitTypeAnnotation()}, ${valueType.emitTypeAnnotation()}]"
        is Type.Custom -> {
            val rendered = name.dotted()
            if (generics.isEmpty()) {
                if (rendered in objectNames) "$rendered.type" else rendered
            } else {
                "$rendered[${generics.joinToString(", ") { it.emitTypeAnnotation() }}]"
            }
        }
        is Type.Nullable -> "Option[${type.emitTypeAnnotation()}]"
        else -> emit()
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "println(${expression.emit()})\n".indentCode(indent)
        is ReturnStatement -> "${expression.emit()}\n".indentCode(indent)
        is ConstructorStatement -> {
            val allArgs = namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
            val argsStr = when {
                allArgs.isEmpty() -> ""
                allArgs.size == 1 -> "(${allArgs.first()})"
                else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
            }
            val prefix = if (needsNew()) "new " else ""
            "$prefix${type.emitGenerics()}$argsStr\n".indentCode(indent)
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
                val prefix = if (constructorStmt.needsNew()) "new " else ""
                "$prefix${constructorStmt.type.emitGenerics()}$argsStr"
            } ?: value.emit()
            if (isProperty) {
                "${name.value().sanitize()} = $expr\n".indentCode(indent)
            } else {
                "val ${name.camelCase().sanitize()} = $expr\n".indentCode(indent)
            }
        }

        is ErrorStatement -> "throw new IllegalStateException(${message.emit()})\n".indentCode(indent)
        is AssertStatement -> "assert(${expression.emit()}, \"$message\")\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Any"
                    val varName = variable?.camelCase() ?: "_"
                    "case $varName: $typeStr => {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "case _ => {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "${expression.emit()} match {\n$casesStr$defaultStr}\n".indentCode(indent)
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    "case ${case.value.emit()} => {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "case _ => {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "${expression.emit()} match {\n$casesStr$defaultStr}\n".indentCode(indent)
            }
        }

        is RawExpression -> "$code\n".indentCode(indent)
        is NullLiteral -> "null\n".indentCode(indent)
        is NullableEmpty -> "None\n".indentCode(indent)
        is ThisExpression -> "this\n".indentCode(indent)
        is VariableReference -> "${name.camelCase().sanitize()}\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}\n".indentCode(indent)
        }

        is FunctionCall -> {
            val typeArgsStr =
                if (typeArguments.isNotEmpty()) "[${typeArguments.joinToString(", ") { it.emitGenerics() }}]" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${name.value().sanitize()}$typeArgsStr(${arguments.values.joinToString(", ") { it.emit() }})\n".indentCode(indent)
        }

        is ArrayIndexCall -> "${emitArrayIndex()}\n".indentCode(indent)

        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()}\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()}.toString\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toScala()} ${right.emit()})\n".indentCode(indent)
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

    private fun BinaryOp.Operator.toScala(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "()"
            } else {
                val allArgs = namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
                val argsStr = when {
                    allArgs.isEmpty() -> ""
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                val prefix = if (needsNew()) "new " else ""
                "$prefix${type.emitGenerics()}$argsStr"
            }
        }

        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "null"
        is NullableEmpty -> "None"
        is ThisExpression -> "this"
        is VariableReference -> name.camelCase().sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}"
        }

        is FunctionCall -> {
            val typeArgsStr =
                if (typeArguments.isNotEmpty()) "[${typeArguments.joinToString(", ") { it.emitGenerics() }}]" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${name.value().sanitize()}$typeArgsStr(${arguments.values.joinToString(", ") { it.emit() }})"
        }

        is ArrayIndexCall -> emitArrayIndex()

        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()}"
        is EnumValueCall -> "${expression.emit()}.toString"
        is BinaryOp -> "(${left.emit()} ${operator.toScala()} ${right.emit()})"
        is TypeDescriptor -> emitTypeDescriptor()
        is NullCheck -> "(${expression.emit()}.map(it => ${body.emit()})${alternative?.emit()?.let { ".getOrElse($it)" } ?: ""})"
        is NullableMap -> "(${expression.emit()}.map(it => ${body.emit()}).getOrElse(${alternative.emit()}))"
        is NullableOf -> "Some(${expression.emit()})"
        is NullableGet -> "${expression.emit()}.get"
        is Constraint.RegexMatch -> "\"\"\"${pattern}\"\"\".r.findFirstIn(${value.emit()}).isDefined"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" && ").ifEmpty { "true" }
            checks
        }
        is ErrorStatement -> "throw new IllegalStateException(${message.emit()})"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Scala")
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Any"
                    val varName = variable?.camelCase() ?: "_"
                    "case $varName: $typeStr => {\n$bodyStr}\n".indentCode(1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "case _ => {\n$bodyStr}\n".indentCode(1)
                } ?: ""
                "${expression.emit()} match {\n$casesStr$defaultStr}"
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    "case ${case.value.emit()} => {\n$bodyStr}\n".indentCode(1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "case _ => {\n$bodyStr}\n".indentCode(1)
                } ?: ""
                "${expression.emit()} match {\n$casesStr$defaultStr}"
            }
        }

        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Scala")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Scala")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Scala")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "if (${condition.emit()}) ${thenExpr.emit()} else ${elseExpr.emit()}"
        is MapExpression -> "${receiver.emit()}.map(${variable.camelCase()} => ${body.emit()})"
        is FlatMapIndexed -> "${receiver.emit()}.zipWithIndex.flatMap { case (${elementVar.camelCase()}, ${indexVar.camelCase()}) => ${body.emit()} }"
        is ListConcat -> when {
            lists.isEmpty() -> "List.empty[String]"
            lists.size == 1 -> lists.single().emit()
            else -> lists.joinToString(" ++ ") { expr ->
                val emitted = expr.emit()
                if (expr is IfExpression) "($emitted)" else emitted
            }
        }
        is StringTemplate -> "s\"${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "\${${it.expression.emit()}}"
            }
        }}\""
    }

    private fun LiteralList.emit(): String {
        if (values.isEmpty()) return "List.empty[${type.emitGenerics()}]"
        val list = values.joinToString(", ") { it.emit() }
        return "List($list)"
    }

    private fun LiteralMap.emit(): String {
        if (values.isEmpty()) return "Map.empty"
        val map = values.entries.joinToString(", ") {
            "${Literal(it.key, keyType).emit()} -> ${it.value.emit()}"
        }
        return "Map($map)"
    }

    private fun Literal.emit(): String = when (type) {
        Type.String -> "\"$value\""
        is Type.Integer -> if (type.precision == Precision.P64) "${value}L" else value.toString()
        else -> value.toString()
    }

    private fun ArrayIndexCall.emitArrayIndex(): String {
        val isMapAccess = index is Literal && (index as Literal).type == Type.String
        return when {
            !caseSensitive && isMapAccess -> "${receiver.emit()}.find(_._1.equalsIgnoreCase(${index.emit()})).map(_._2)"
            isMapAccess -> "${receiver.emit()}.get(${index.emit()})"
            else -> "${receiver.emit()}(${index.emit()})"
        }
    }

    private fun TypeDescriptor.emitTypeDescriptor(): String = "scala.reflect.classTag[${type.emitGenerics()}]"
}

private fun String.sanitize(): String = if (reservedKeywords.contains(this)) "`$this`" else this

private val reservedKeywords = setOf(
    "abstract", "case", "class", "def", "do",
    "else", "extends", "false", "final", "for",
    "forSome", "if", "implicit", "import", "lazy",
    "match", "new", "null", "object", "override",
    "package", "private", "protected", "return", "sealed",
    "super", "this", "throw", "trait", "true",
    "try", "type", "val", "var", "while",
    "with", "yield", "given", "using", "enum",
    "export", "then",
)
