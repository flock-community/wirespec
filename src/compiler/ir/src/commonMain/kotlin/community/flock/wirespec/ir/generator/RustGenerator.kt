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
import community.flock.wirespec.ir.core.NullableMap
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.Package
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

object RustGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> element.emit(0)
        else -> File(Name.of(""), listOf(element)).emit(0)
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
            val isInClass = parents.any { it is Struct || it is Interface || it is Namespace }
            val isInInterface = parents.any { it is Interface }
            emit(indent, isInClass = isInClass, isStaticScope = isStaticScope, isInInterface = isInInterface)
        }
        is Namespace -> emit(indent, parents, allUnions = allUnions)
        is Interface -> emit(indent, parents, allUnions = allUnions)
        is Union -> emit(indent, parents, allUnions = allUnions)
        is Enum -> emit(indent)
        is Main -> {
            val content = body.joinToString("") { it.emit(indent + 1) }
            "fn main() {\n$content}\n".indentCode(indent)
        }
        is File -> elements.joinToString("") { it.emit(indent, parents, allUnions, isStaticScope) }
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

    private fun Package.emit(indent: Int): String = "// package $path\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "use super::${type.name.toSnakeCase()}::${type.name};\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val hasComplexElements = elements.any { it is Interface || it is Union || it is Enum || it is Struct }
        val content = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = !hasComplexElements) }
        val rustName = name.pascalCase()
        return if (content.isBlank()) {
            "pub struct $rustName;\n\n".indentCode(indent)
        } else if (hasComplexElements) {
            val useSuper = "use super::*;\n".indentCode(indent + 1)
            "pub mod $rustName {\n$useSuper$content}\n\n".indentCode(indent)
        } else {
            "pub struct $rustName;\n\nimpl $rustName {\n$content}\n\n".indentCode(indent)
        }
    }

    private fun Interface.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val rustName = name.pascalCase()
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extStr = if (extends.isNotEmpty()) " : ${extends.joinToString(" + ") { it.emit() }}" else ""
        val fieldsContent = fields.joinToString("") { field ->
            "fn ${field.name.snakeCase().sanitize()}(&self) -> ${field.type.emit()};\n".indentCode(indent + 1)
        }
        val elementsContent = elements.joinToString("") { it.emit(indent + 1, parents = parents + this, allUnions = allUnions, isStaticScope = false) }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "pub trait $rustName$typeParamsStr$extStr {}\n\n".indentCode(indent)
        } else {
            "pub trait $rustName$typeParamsStr$extStr {\n$content}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val rustName = name.pascalCase()
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val enumDef = if (members.isNotEmpty()) {
            val variants = members.joinToString("\n") { "${it.name}(${it.name}),".indentCode(indent + 1) }
            "#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]\npub enum $rustName$typeParamsStr {\n$variants\n}\n\n".indentCode(indent)
        } else {
            "#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]\npub enum $rustName$typeParamsStr {}\n\n".indentCode(indent)
        }

        val fromImpls = if (rustName == "Response" && members.isNotEmpty()) {
            members.joinToString("\n") { member ->
                "impl From<${member.name}> for Response { fn from(value: ${member.name}) -> Self { Response::${member.name}(value) } }"
            } + "\n"
        } else {
            ""
        }

        return enumDef + fromImpls
    }

    private fun Enum.emit(indent: Int): String {
        val rustName = name.pascalCase()
        val entriesStr = if (entries.isEmpty()) {
            ""
        } else {
            entries.joinToString("\n") { entry ->
                if (entry.values.isNotEmpty()) {
                    "#[serde(rename = ${entry.values.first()})]".indentCode(indent + 1) + "\n" +
                        "${entry.name.pascalCase()},".indentCode(indent + 1)
                } else {
                    "${entry.name.pascalCase()},".indentCode(indent + 1)
                }
            }
        }
        return "#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]\npub enum $rustName {\n$entriesStr\n}\n\n".indentCode(indent)
    }

    private fun Struct.emit(indent: Int, parents: List<Element> = emptyList(), allUnions: List<Union> = emptyList()): String {
        val rustName = name.pascalCase()
        val functions = elements.filterIsInstance<AstFunction>()
        val nonFunctions = elements.filter { it !is AstFunction }
        val nestedContent = nonFunctions.joinToString("") { it.emit(indent, parents = parents + this, allUnions = allUnions, isStaticScope = false) }

        if (fields.isEmpty() && constructors.isEmpty()) {
            val structDef = "#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]\npub struct $rustName;\n\n".indentCode(indent)
            val implBlock = if (functions.isNotEmpty()) {
                val fnsContent = functions.joinToString("") { it.emit(indent + 1, isInClass = true, isStaticScope = false, isInInterface = false) }
                "impl $rustName {\n$fnsContent}\n\n".indentCode(indent)
            } else {
                ""
            }
            return "$structDef$implBlock$nestedContent"
        }

        val fieldsStr = fields.joinToString("\n") {
            "pub ${it.name.snakeCase().sanitize()}: ${it.type.emit()},".indentCode(indent + 1)
        }
        val structDef = "#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]\n#[serde(default)]\npub struct $rustName {\n$fieldsStr\n}\n\n".indentCode(indent)

        val customConstructors = constructors.joinToString("") { it.emit(rustName, fields, indent) }

        val implBlock = if (functions.isNotEmpty()) {
            val fnsContent = functions.joinToString("") { it.emit(indent + 1, isInClass = true, isStaticScope = false, isInInterface = false) }
            "impl $rustName {\n$fnsContent}\n\n".indentCode(indent)
        } else {
            ""
        }

        return "$structDef$customConstructors$implBlock$nestedContent"
    }

    private fun Constructor.emit(structName: String, structFields: List<Field>, indent: Int): String {
        val params = parameters.joinToString(", ") { "${it.name.snakeCase().sanitize()}: ${it.type.emit()}" }
        val assignments = body.filterIsInstance<Assignment>().associate {
            it.name.camelCase() to it.value.emit()
        }
        val fieldInits = structFields.joinToString(",\n") { field ->
            val value = assignments[field.name.camelCase()] ?: "Default::default()"
            "${field.name.snakeCase().sanitize()}: $value".indentCode(indent + 2)
        }
        return "impl $structName {\n${"pub fn new($params) -> Self {\n".indentCode(indent + 1)}${"$structName {\n".indentCode(indent + 2)}$fieldInits\n${"}\n".indentCode(indent + 2)}${"}\n".indentCode(indent + 1)}}\n\n".indentCode(indent)
    }

    private fun AstFunction.emit(indent: Int, isInClass: Boolean = false, isStaticScope: Boolean = false, isInInterface: Boolean = false): String {
        val params = parameters.joinToString(", ") {
            val paramName = it.name.camelCase()
            if (paramName == "self" || paramName == "&self") paramName else "${it.name.snakeCase().sanitize()}: ${it.type.emit()}"
        }
        val rType = returnType?.takeIf { it != Type.Unit }?.emit()
        val returnTypeStr = if (rType != null) " -> $rType" else ""
        val prefix = if (isInInterface && body.isEmpty()) "" else "pub "
        val content = if (body.isEmpty()) {
            return "${prefix}fn ${name.snakeCase().sanitize()}($params)$returnTypeStr;\n".indentCode(indent)
        } else {
            body.joinToString("") { it.emit(indent + 1) }
        }
        return "${prefix}fn ${name.snakeCase().sanitize()}($params)$returnTypeStr {\n$content}\n\n".indentCode(indent)
    }

    private fun TypeParameter.emit(): String {
        val typeStr = type.emit()
        return if (extends.isEmpty()) {
            typeStr
        } else {
            "$typeStr: ${extends.joinToString(" + ") { it.emit() }}"
        }
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "i32"
            Precision.P64 -> "i64"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "f32"
            Precision.P64 -> "f64"
        }
        Type.Any -> "Box<dyn std::any::Any>"
        Type.String -> "String"
        Type.Boolean -> "bool"
        Type.Bytes -> "Vec<u8>"
        Type.Unit -> "()"
        Type.Wildcard -> "_"
        is Type.Array -> "Vec<${elementType.emit()}>"
        is Type.Dict -> "std::collections::HashMap<${keyType.emit()}, ${valueType.emit()}>"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                name
            } else {
                "$name<${generics.joinToString(", ") { it.emit() }}>"
            }
        }
        is Type.Nullable -> "Option<${type.emit()}>"
    }

    private val serializationMethodNames = setOf(
        "serialize_path",
        "serialize_param",
        "serialize_body",
        "deserialize_path",
        "deserialize_param",
        "deserialize_body",
    )

    private fun emitFunctionCallArgs(arguments: Map<Name, Expression>, name: Name): String {
        val isSerialization = name.snakeCase() in serializationMethodNames
        return arguments.values.mapIndexed { idx, arg ->
            val emitted = arg.emit()
            if (isSerialization && idx == 0) "&$emitted" else emitted
        }.joinToString(", ")
    }

    private fun emitFunctionCallArgsInlined(arguments: Map<Name, Expression>, name: Name, replacement: String): String {
        val isSerialization = name.snakeCase() in serializationMethodNames
        return arguments.values.mapIndexed { idx, arg ->
            val emitted = arg.emitWithInlinedIt(replacement)
            if (isSerialization && idx == 0) "&$emitted" else emitted
        }.joinToString(", ")
    }

    private fun emitArrayIndex(receiver: Expression, index: Expression): String = if (index is Literal && index.type is Type.String) {
        "${receiver.emit()}.get(\"${index.value}\")"
    } else {
        val idxStr = if (index is Literal && (index.type is Type.Integer || index.type is Type.Number)) {
            "${index.value}"
        } else {
            index.emit()
        }
        "${receiver.emit()}[$idxStr]"
    }

    private fun emitErrorMessage(message: Expression): String = when (message) {
        is BinaryOp -> {
            fun flattenPlus(expr: Expression): List<Expression> = when {
                expr is BinaryOp && expr.operator == BinaryOp.Operator.PLUS -> flattenPlus(expr.left) + flattenPlus(expr.right)
                else -> listOf(expr)
            }
            val parts = flattenPlus(message)
            val formatStr = parts.joinToString("") {
                if (it is Literal && it.type is Type.String) it.value.toString() else "{}"
            }
            val args = parts.filter { !(it is Literal && it.type is Type.String) }.map { it.emit() }
            if (args.isEmpty()) "\"$formatStr\"" else "\"$formatStr\", ${args.joinToString(", ")}"
        }
        is Literal -> when {
            message.type is Type.String -> "\"${message.value}\""
            else -> "\"{}\", ${message.emit()}"
        }
        else -> "\"{}\", ${message.emit()}"
    }

    private fun emitUnwrap(alternative: Expression?): String = when {
        alternative is ErrorStatement && alternative.message is Literal && (alternative.message as Literal).type is Type.String ->
            ".expect(\"${(alternative.message as Literal).value}\")"
        alternative is ErrorStatement ->
            ".unwrap_or_else(|| ${alternative.emit()})"
        alternative != null ->
            ".unwrap_or(${alternative.emit()})"
        else -> ""
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "println!(\"{}\", ${expression.emit()});\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()};\n".indentCode(indent)
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "()\n".indentCode(indent)
            } else {
                val allArgs = namedArguments.map { "${it.key.snakeCase().sanitize()}: ${it.value.emit()}" }
                val argsStr = when {
                    allArgs.isEmpty() -> " {}"
                    else -> " {\n${allArgs.joinToString(",\n") { it.indentCode(1) }},\n}"
                }
                "${type.emit()}$argsStr\n".indentCode(indent)
            }
        }
        is Literal -> "${emit()};\n".indentCode(indent)
        is LiteralList -> "${emit()};\n".indentCode(indent)
        is LiteralMap -> "${emit()};\n".indentCode(indent)
        is Assignment -> {
            val expr = value.emit()
            "let ${name.snakeCase().sanitize()} = $expr;\n".indentCode(indent)
        }
        is ErrorStatement -> "panic!(${emitErrorMessage(message)});\n".indentCode(indent)
        is AssertStatement -> "assert!(${expression.emit()}, \"$message\");\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                    val typeStr = case.type?.emit() ?: "_"
                    val varBinding = variable?.let { it.snakeCase() } ?: "_"
                    "$typeStr($varBinding) => {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                    "_ => {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "match ${expression.emit()} {\n$casesStr$defaultStr}\n".indentCode(indent)
            } else {
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                    "${case.value.emit()} => {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                    "_ => {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "match ${expression.emit()} {\n$casesStr$defaultStr}\n".indentCode(indent)
            }
        }
        is RawExpression -> "$code;\n".indentCode(indent)
        is NullLiteral -> "None;\n".indentCode(indent)
        is NullableEmpty -> "None;\n".indentCode(indent)
        is VariableReference -> "${name.snakeCase().sanitize()};\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.snakeCase().sanitize()};\n".indentCode(indent)
        }
        is FunctionCall -> {
            val recv = receiver
            if (recv != null) {
                "${recv.emit()}.${name.snakeCase().sanitize()}(${emitFunctionCallArgs(arguments, name)});\n".indentCode(indent)
            } else {
                "${name.snakeCase().sanitize()}(${emitFunctionCallArgs(arguments, name)});\n".indentCode(indent)
            }
        }
        is ArrayIndexCall -> "${emitArrayIndex(receiver, index)};\n".indentCode(indent)
        is EnumReference -> "${enumType.emit()}::${entry.pascalCase()};\n".indentCode(indent)
        is EnumValueCall -> "format!(\"{:?}\", ${expression.emit()});\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toRust()} ${right.emit()});\n".indentCode(indent)
        is TypeDescriptor -> "std::any::type_name::<${type.emit()}>();\n".indentCode(indent)
        is NullCheck -> "${emit()};\n".indentCode(indent)
        is NullableMap -> "${emit()};\n".indentCode(indent)
        is NullableOf -> "${emit()};\n".indentCode(indent)
        is Constraint.RegexMatch -> "${emit()};\n".indentCode(indent)
        is Constraint.BoundCheck -> "${emit()};\n".indentCode(indent)
        is NotExpression -> "!${expression.emit()};\n".indentCode(indent)
        is IfExpression -> "${emit()};\n".indentCode(indent)
        is MapExpression -> "${emit()};\n".indentCode(indent)
        is FlatMapIndexed -> "${emit()};\n".indentCode(indent)
        is ListConcat -> "${emit()};\n".indentCode(indent)
        is StringTemplate -> "${emit()};\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toRust(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "()"
            } else {
                val allArgs = namedArguments.map { "${it.key.snakeCase().sanitize()}: ${it.value.emit()}" }
                val argsStr = when {
                    allArgs.isEmpty() -> " {}"
                    else -> " { ${allArgs.joinToString(", ")} }"
                }
                "${type.emit()}$argsStr"
            }
        }
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "None"
        is NullableEmpty -> "None"
        is VariableReference -> name.snakeCase().sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.snakeCase().sanitize()}"
        }
        is FunctionCall -> {
            val recv = receiver
            if (recv != null) {
                "${recv.emit()}.${name.snakeCase().sanitize()}(${emitFunctionCallArgs(arguments, name)})"
            } else {
                "${name.snakeCase().sanitize()}(${emitFunctionCallArgs(arguments, name)})"
            }
        }
        is ArrayIndexCall -> emitArrayIndex(receiver, index)
        is EnumReference -> "${enumType.emit()}::${entry.pascalCase()}"
        is EnumValueCall -> "format!(\"{:?}\", ${expression.emit()})"
        is BinaryOp -> "(${left.emit()} ${operator.toRust()} ${right.emit()})"
        is TypeDescriptor -> "std::any::type_name::<${type.emit()}>()"
        is NullCheck -> {
            val exprStr = expression.emit()
            val bodyStr = body.emit()
            "$exprStr.as_ref().map(|it| $bodyStr)${emitUnwrap(alternative)}"
        }
        is NullableMap -> {
            val exprStr = expression.emit()
            val bodyStr = body.emit()
            "$exprStr.as_ref().map(|it| $bodyStr)${emitUnwrap(alternative)}"
        }
        is NullableOf -> "Some(${expression.emit()})"
        is Constraint.RegexMatch -> "regex::Regex::new(r\"${pattern}\").unwrap().is_match(&${value.emit()})"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" && ").ifEmpty { "true" }
            checks
        }
        is ErrorStatement -> "panic!(${emitErrorMessage(message)})"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Rust")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Rust")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Rust")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Rust")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Rust")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "if ${condition.emit()} { ${thenExpr.emit()} } else { ${elseExpr.emit()} }"
        is MapExpression -> "${receiver.emit()}.iter().map(|${variable.snakeCase()}| ${body.emit()}).collect::<Vec<_>>()"
        is FlatMapIndexed -> "${receiver.emit()}.iter().enumerate().flat_map(|(${indexVar.snakeCase()}, ${elementVar.snakeCase()})| ${body.emit()}).collect::<Vec<_>>()"
        is ListConcat -> when {
            lists.isEmpty() -> "vec![]"
            lists.size == 1 -> lists.single().emit()
            else -> "vec![${lists.joinToString(", ") { "${it.emit()}.as_slice()" }}].concat()"
        }
        is StringTemplate -> {
            val formatParts = mutableListOf<String>()
            val args = mutableListOf<String>()
            parts.forEach {
                when (it) {
                    is StringTemplate.Part.Text -> formatParts.add(it.value)
                    is StringTemplate.Part.Expr -> {
                        formatParts.add("{}")
                        args.add(it.expression.emit())
                    }
                }
            }
            val formatStr = formatParts.joinToString("")
            if (args.isEmpty()) {
                "String::from(\"$formatStr\")"
            } else {
                "format!(\"$formatStr\", ${args.joinToString(", ")})"
            }
        }
    }

    private fun Expression.emitWithInlinedIt(replacement: String): String = when (this) {
        is VariableReference -> if (name.camelCase() == "it") replacement else emit()
        is FunctionCall -> {
            val recv = receiver
            if (recv != null) {
                "${recv.emitWithInlinedIt(replacement)}.${name.snakeCase().sanitize()}(${emitFunctionCallArgsInlined(arguments, name, replacement)})"
            } else {
                "${name.snakeCase().sanitize()}(${emitFunctionCallArgsInlined(arguments, name, replacement)})"
            }
        }
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emitWithInlinedIt(replacement)}." } ?: ""
            "$receiverStr${field.snakeCase().sanitize()}"
        }
        is ArrayIndexCall -> {
            if (index is Literal && index.type is Type.String) {
                "${receiver.emitWithInlinedIt(replacement)}.get(\"${(index as Literal).value}\")"
            } else {
                val idxStr = if (index is Literal && (index.type is Type.Integer || index.type is Type.Number)) {
                    "${(index as Literal).value}"
                } else {
                    index.emitWithInlinedIt(replacement)
                }
                "${receiver.emitWithInlinedIt(replacement)}[$idxStr]"
            }
        }
        is EnumValueCall -> "format!(\"{:?}\", ${expression.emitWithInlinedIt(replacement)})"
        is NotExpression -> "!${expression.emitWithInlinedIt(replacement)}"
        is IfExpression -> "if ${condition.emitWithInlinedIt(replacement)} { ${thenExpr.emitWithInlinedIt(replacement)} } else { ${elseExpr.emitWithInlinedIt(replacement)} }"
        is MapExpression -> "${receiver.emitWithInlinedIt(replacement)}.iter().map(|${variable.snakeCase()}| ${body.emitWithInlinedIt(replacement)}).collect::<Vec<_>>()"
        is FlatMapIndexed -> "${receiver.emitWithInlinedIt(replacement)}.iter().enumerate().flat_map(|(${indexVar.snakeCase()}, ${elementVar.snakeCase()})| ${body.emitWithInlinedIt(replacement)}).collect::<Vec<_>>()"
        is ListConcat -> when {
            lists.isEmpty() -> "vec![]"
            lists.size == 1 -> lists.single().emitWithInlinedIt(replacement)
            else -> "vec![${lists.joinToString(", ") { "${it.emitWithInlinedIt(replacement)}.as_slice()" }}].concat()"
        }
        is StringTemplate -> {
            val formatParts = mutableListOf<String>()
            val args = mutableListOf<String>()
            parts.forEach {
                when (it) {
                    is StringTemplate.Part.Text -> formatParts.add(it.value)
                    is StringTemplate.Part.Expr -> {
                        formatParts.add("{}")
                        args.add(it.expression.emitWithInlinedIt(replacement))
                    }
                }
            }
            val formatStr = formatParts.joinToString("")
            if (args.isEmpty()) {
                "String::from(\"$formatStr\")"
            } else {
                "format!(\"$formatStr\", ${args.joinToString(", ")})"
            }
        }
        is LiteralList -> emit()
        else -> emit()
    }

    private fun LiteralList.emit(): String {
        if (values.isEmpty()) return "Vec::<${type.emit()}>::new()"
        val list = values.joinToString(", ") { it.emit() }
        return "vec![$list]"
    }

    private fun LiteralMap.emit(): String {
        if (values.isEmpty()) return "std::collections::HashMap::new()"
        val map = values.entries.joinToString(", ") {
            "(${Literal(it.key, keyType).emit()}, ${it.value.emit()})"
        }
        return "std::collections::HashMap::from([$map])"
    }

    private fun Literal.emit(): String = when {
        type is Type.String -> "String::from(\"$value\")"
        type is Type.Number -> "${value}_${(type as Type.Number).let {
            when (it.precision) {
                Precision.P32 -> "f32"
                Precision.P64 -> "f64"
            }
        }}"
        type is Type.Integer -> "${value}_${(type as Type.Integer).let {
            when (it.precision) {
                Precision.P32 -> "i32"
                Precision.P64 -> "i64"
            }
        }}"
        type == Type.Boolean -> value.toString()
        else -> value.toString()
    }
}

private fun String.sanitize(): String = if (reservedKeywords.contains(this)) "r#$this" else this

private fun String.toSnakeCase(): String {
    if (this.isEmpty()) return this
    val result = StringBuilder()
    for ((i, c) in this.withIndex()) {
        if (c.isUpperCase() && i > 0 && this[i - 1].isLowerCase()) {
            result.append('_')
        }
        result.append(c.lowercaseChar())
    }
    return result.toString()
}

private val reservedKeywords = setOf(
    "as", "break", "const", "continue", "crate",
    "else", "enum", "extern", "false", "fn",
    "for", "if", "impl", "in", "let",
    "loop", "match", "mod", "move", "mut",
    "pub", "ref", "return",
    "static", "struct", "super", "trait", "true",
    "type", "unsafe", "use", "where", "while",
    "async", "await", "dyn", "abstract", "become",
    "box", "do", "final", "macro", "override",
    "priv", "typeof", "unsized", "virtual", "yield",
    "try",
)
