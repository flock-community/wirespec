package community.flock.wirespec.ir.generator

import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.AssertStatement
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ClassReference
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
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Function as AstFunction

object ScalaGenerator : Generator {
    private var objectNames: Set<String> = emptySet()
    private var primaryFieldNames: Map<String, Set<String>> = emptyMap()

    override fun generate(element: Element): String {
        val file = if (element is File) element else File(Name.of(""), listOf(element))
        objectNames = collectObjectNames(file.elements)
        primaryFieldNames = collectPrimaryFieldNames(file.elements)
        return emitFile(file)
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

    private fun Struct.isModelStruct(): Boolean = interfaces.any { it.name == "Wirespec.Model" }

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
        val typeName = (type as? Type.Custom)?.name ?: return false
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
        is AstFunction -> emit(indent)
        is Namespace -> emit(indent, parents)
        is Interface -> emit(indent, parents)
        is Union -> emit(indent)
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

    private fun Import.emit(indent: Int): String = "import $path.${type.name}\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.emitTypeAnnotation()?.let { " extends $it" }.orEmpty()
        val content = elements.joinToString("") { it.emit(indent + 1, isStatic = true, parents = parents + this) }
        return "object ${name.pascalCase()}$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val sealedStr = "sealed ".takeIf { isSealed }.orEmpty()
        val typeParamsStr = typeParameters.joinNonEmpty(", ", "[", "]") { it.emit() }
        val extStr = extends.joinNonEmpty(" with ", " extends ") { it.emitTypeAnnotation() }
        val fieldsContent = fields.joinToString("") { field ->
            val overridePrefix = "override ".takeIf { field.isOverride }.orEmpty()
            "${overridePrefix}def ${field.name.value()}: ${field.type.emitTypeAnnotation()}\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, isStatic = false, parents = parents + this) }
        val content = fieldsContent + elementsContent
        val signature = "${sealedStr}trait ${name.pascalCase()}$typeParamsStr$extStr"
        return if (content.isEmpty()) {
            "$signature\n\n".indentCode(indent)
        } else {
            "$signature {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int): String {
        val typeParamsStr = typeParameters.joinNonEmpty(", ", "[", "]") { it.emit() }
        val extStr = extends?.emitTypeAnnotation()?.let { " extends $it" }.orEmpty()
        return "sealed trait ${name.pascalCase()}$typeParamsStr$extStr\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val implStr = extends?.emitGenerics()?.let { " extends $it" }.orEmpty()
        val closingBrace = "}".indentCode(indent)
        val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
            val overridePrefix = "override ".takeIf { _ -> it.isOverride || it.name.camelCase() == "toString" }.orEmpty()
            it.emitAsMethod(indent + 1, overridePrefix)
        }

        return if (fields.isNotEmpty()) {
            val fieldsStr = fields.joinToString(", ") {
                val overridePrefix = "override ".takeIf { _ -> it.isOverride }.orEmpty()
                "${overridePrefix}val ${it.name.value()}: ${it.type.emitGenerics()}"
            }
            val entriesStr = entries.joinToString(",\n") { entry ->
                val ext = entry.values.joinNonEmpty(", ", " extends ${name.pascalCase()}(", ")")
                "case ${entry.name.value()}$ext".indentCode(indent + 1)
            }
            val content = listOf(entriesStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
            "enum ${name.pascalCase()}($fieldsStr)$implStr {\n$content\n$closingBrace\n\n".indentCode(indent)
        } else {
            val entriesStr = entries.joinToString("\n") { "case ${it.name.value()}".indentCode(indent + 1) }
            val content = listOf(entriesStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
            "enum ${name.pascalCase()}$implStr {\n$content\n$closingBrace\n\n".indentCode(indent)
        }
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
        val closingBrace = "}".indentCode(indent)

        // Empty-parameter primary constructor
        val primary = constructors.singleOrNull()?.takeIf { it.parameters.isEmpty() }
        if (primary != null) {
            if (isModelStruct()) {
                return if (nestedContent.isEmpty()) {
                    "case class $pascal()$implStr\n\n".indentCode(indent)
                } else {
                    "case class $pascal()$implStr {\n$nestedContent$closingBrace\n\n".indentCode(indent)
                }
            }
            val assignments = primary.body.filterIsInstance<Assignment>().associateBy { it.name.camelCase() }
            val fieldProperties = fields.joinToString("\n") { field ->
                val overridePrefix = "override ".takeIf { _ -> field.isOverride }.orEmpty()
                val valueStr = assignments[field.name.value()]?.let { " = ${it.value.emit()}" }.orEmpty()
                "${overridePrefix}val ${field.name.value().sanitize()}: ${field.type.emitTypeAnnotation()}$valueStr".indentCode(indent + 1)
            }
            val bodyContent = listOf(fieldProperties, nestedContent).filter { it.isNotEmpty() }.joinToString("\n")
            return if (bodyContent.isEmpty()) {
                "object $pascal$implStr\n\n".indentCode(indent)
            } else {
                "object $pascal$implStr {\n$bodyContent$closingBrace\n\n".indentCode(indent)
            }
        }

        if (fields.isEmpty() && constructors.isEmpty()) {
            return if (nestedContent.isEmpty()) {
                "object $pascal$implStr\n\n".indentCode(indent)
            } else {
                "object $pascal$implStr {\n$nestedContent$closingBrace\n\n".indentCode(indent)
            }
        }

        val paramsStr = if (fields.isEmpty()) {
            ""
        } else {
            fields.joinToString(",\n", "(\n", "\n${")".indentCode(indent)}") {
                val overridePrefix = "override ".takeIf { _ -> it.isOverride }.orEmpty()
                "${overridePrefix}val ${it.name.value().sanitize()}: ${it.type.emitTypeAnnotation()}".indentCode(indent + 1)
            }
        }
        val hasBody = customConstructors.isNotEmpty() || nestedContent.isNotEmpty()
        return if (hasBody) {
            "case class $pascal$paramsStr$implStr {\n$customConstructors$nestedContent$closingBrace\n\n".indentCode(indent)
        } else {
            "case class $pascal$paramsStr$implStr\n\n".indentCode(indent)
        }
    }

    private fun Constructor.emitScala(structFields: List<Field>, indent: Int): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val delegation = body.filterIsInstance<ConstructorStatement>().firstOrNull()

        val rhs = if (delegation != null) {
            val args = delegation.namedArguments.entries.joinToString(", ") { (k, v) -> "${k.value()} = ${v.emit()}" }
            "this($args)"
        } else {
            val assignments = body.filterIsInstance<Assignment>().associate { it.name.value() to it.value.emit() }
            val constructorArgs = structFields.joinToString(", ") { assignments[it.name.value()] ?: "null" }
            "this($constructorArgs)"
        }

        return "def this($params) = $rhs\n".indentCode(indent)
    }

    private fun AstFunction.emit(indent: Int, parents: List<Element>): String {
        val overridePrefix = if (isOverride) "override " else ""
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "[${typeParameters.joinToString(", ") { it.emit() }}]"
        } else {
            ""
        }
        val rType = returnType?.takeIf { it != Type.Unit }?.emitTypeAnnotation() ?: "Unit"
        val params = parameters.joinToString(", ") { it.emit(0) }
        val signature = "${overridePrefix}def ${name.camelCase()}$typeParamsStr($params): $rType"

        return when {
            body.isEmpty() -> "$signature\n".indentCode(indent)
            body.size == 1 && body.first() is ReturnStatement -> {
                val expr = (body.first() as ReturnStatement).expression.emit()
                "$signature =\n${expr.indentCode(1)}\n\n".indentCode(indent)
            }
            else -> {
                val content = body.joinToString("") { it.emit(1) }
                "$signature = {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
            }
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
        is Type.Custom -> name
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
            if (generics.isEmpty()) {
                if (name in objectNames) "$name.type" else name
            } else {
                "$name[${generics.joinToString(", ") { it.emitTypeAnnotation() }}]"
            }
        }
        is Type.Nullable -> "Option[${type.emitTypeAnnotation()}]"
        else -> emit()
    }

    private fun ConstructorStatement.formatArgs(): String {
        val allArgs = namedArguments.map { "${it.key.value()} = ${it.value.emit()}" }
        return when {
            allArgs.isEmpty() -> ""
            allArgs.size == 1 -> "(${allArgs.first()})"
            else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
        }
    }

    private fun ConstructorStatement.emitConstructorExpression(): String {
        val prefix = "new ".takeIf { needsNew() }.orEmpty()
        return "$prefix${type.emitGenerics()}${formatArgs()}"
    }

    private fun Switch.emitMatch(caseIndent: Int): String {
        val isPatternSwitch = cases.any { it.type != null }
        val casesStr = cases.joinToString("") { case ->
            val bodyStr = case.body.joinToString("") { it.emit(1) }
            val pattern = if (isPatternSwitch) {
                val varName = variable?.camelCase() ?: "_"
                "case $varName: ${case.type?.emitGenerics() ?: "Any"}"
            } else {
                "case ${case.value.emit()}"
            }
            "$pattern => {\n$bodyStr}\n".indentCode(caseIndent)
        }
        val defaultStr = default?.let {
            val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
            "case _ => {\n$bodyStr}\n".indentCode(caseIndent)
        }.orEmpty()
        return "${expression.emit()} match {\n$casesStr$defaultStr}"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "println(${expression.emit()})\n".indentCode(indent)
        is ReturnStatement -> "${expression.emit()}\n".indentCode(indent)
        is ConstructorStatement -> "${emitConstructorExpression()}\n".indentCode(indent)

        is Literal -> "${emit()}\n".indentCode(indent)
        is LiteralList -> "${emit()}\n".indentCode(indent)
        is LiteralMap -> "${emit()}\n".indentCode(indent)
        is Assignment -> {
            val expr = (value as? ConstructorStatement)?.emitConstructorExpression() ?: value.emit()
            val lhs = if (isProperty) name.value().sanitize() else "val ${name.camelCase().sanitize()}"
            "$lhs = $expr\n".indentCode(indent)
        }

        is ErrorStatement -> "throw new IllegalStateException(${message.emit()})\n".indentCode(indent)
        is AssertStatement -> "assert(${expression.emit()}, \"$message\")\n".indentCode(indent)
        is Switch -> "${emitMatch(indent + 1)}\n".indentCode(indent)

        is RawExpression -> "$code\n".indentCode(indent)
        is NullLiteral -> "null\n".indentCode(indent)
        is NullableEmpty -> "None\n".indentCode(indent)
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
        is EnumReference -> "${emit()}\n".indentCode(indent)
        is EnumValueCall -> "${emit()}\n".indentCode(indent)
        is BinaryOp -> "${emit()}\n".indentCode(indent)
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
            if (type == Type.Unit) "()" else emitConstructorExpression()
        }

        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is ClassReference -> TODO("ClassReference emission not yet implemented")
        is RawExpression -> code
        is NullLiteral -> "null"
        is NullableEmpty -> "None"
        is VariableReference -> name.camelCase().sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.emit()?.plus(".").orEmpty()
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
        is Switch -> emitMatch(1)

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
