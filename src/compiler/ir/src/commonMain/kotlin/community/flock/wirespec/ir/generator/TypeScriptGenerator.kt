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
import community.flock.wirespec.ir.core.forEachElement
import community.flock.wirespec.ir.core.Function as AstFunction

object TypeScriptGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> TypeScriptFileEmitter(element).emitFile()
        else -> TypeScriptFileEmitter(File(Name.of(""), listOf(element))).emitFile()
    }

    fun generateExpression(expression: Expression): String = TypeScriptFileEmitter(File(Name.of(""), emptyList())).renderExpression(expression)
}

private class TypeScriptFileEmitter(val file: File) {

    private val structsWithConstructors: Set<String> = buildSet {
        file.forEachElement { element ->
            if (element is Struct && element.constructors.isNotEmpty()) {
                add(element.name.pascalCase())
            }
        }
    }

    private val constructorFuncNames: Set<String> = structsWithConstructors
        .map { it.replaceFirstChar { c -> c.lowercaseChar() } }
        .toSet()

    fun emitFile(): String = file.elements.joinToString("") { it.emit(0) }.removeEmptyLines()

    fun renderExpression(expression: Expression): String = expression.emit()

    private fun String.indentCode(level: Int): String = " ".repeat(level * 2) + this

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n").plus("\n")

    private fun Element.emit(indent: Int): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> emit(indent)
        is AstFunction -> emit(indent)
        is Namespace -> emit(indent)
        is Interface -> emit(indent)
        is Union -> emit(indent)
        is Enum -> emit(indent)
        is Main -> {
            val staticContent = statics.joinToString("") { it.emit(indent) }
            val content = body.joinToString("") { it.emit(indent + 1) }
            "$staticContent${";(async () => {\n$content${"})();".indentCode(indent)}\n".indentCode(indent)}"
        }
        is File -> elements.joinToString("") { it.emit(indent) }
        is RawElement -> code.lines().joinToString("\n") { if (it.isEmpty()) it else it.indentCode(indent) } + "\n"
    }

    private fun Package.emit(indent: Int): String = ""

    private fun Import.emit(indent: Int): String {
        val prefix = if (isTypeOnly) "type " else ""
        return "import {$prefix${type.name}} from '$path'\n".indentCode(indent)
    }

    private fun Namespace.emit(indent: Int): String {
        val content = elements.joinToString("") { it.emit(indent + 1) }
        val closingBrace = "}\n".indentCode(indent)
        return "export namespace ${name.pascalCase()} {\n$content$closingBrace".indentCode(indent)
    }

    private fun Interface.emit(indent: Int): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { tp ->
                val extendsStr = if (tp.extends.isNotEmpty()) " extends ${tp.extends.joinToString(" & ") { it.emit() }}" else ""
                "${tp.type.emit()}$extendsStr"
            }}>"
        } else {
            ""
        }
        val ext = extends.map { it.emit() }
        val extStr = if (ext.isEmpty()) "" else " extends ${ext.joinToString(", ")}"
        val nestedInterfaces = elements.filterIsInstance<Interface>().associateBy { it.name.pascalCase() }
        val nonInterfaceElements = elements.filter { it !is Interface }
        val fieldsContent = fields.joinToString("") { field ->
            val typeStr = field.type.emitWithInlineInterfaces(nestedInterfaces)
            "${field.name.value()}: $typeStr;\n".indentCode(indent + 1)
        }
        val elementsContent = nonInterfaceElements.joinToString("") {
            when (it) {
                is AstFunction -> it.emit(indent + 1, nestedInterfaces)
                else -> it.emit(indent + 1)
            }
        }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "export interface ${name.pascalCase()}$typeParamsStr$extStr {}\n".indentCode(indent)
        } else {
            val closingBrace = "}\n".indentCode(indent)
            "export interface ${name.pascalCase()}$typeParamsStr$extStr {\n$content$closingBrace".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { "${it.type.emit()} = unknown" }}>"
        } else {
            ""
        }
        return if (members.isNotEmpty()) {
            "export type ${name.pascalCase()}$typeParamsStr = ${members.joinToString(" | ") { it.name }}\n".indentCode(indent)
        } else {
            val extStr = extends?.let { " extends ${it.emit()}" } ?: ""
            "export interface ${name.pascalCase()}$typeParamsStr$extStr {}\n".indentCode(indent)
        }
    }

    private fun Enum.emit(indent: Int): String = "export type ${name.pascalCase()} = ${entries.joinToString(" | ") { "\"${it.name.value()}\"" }}\n".indentCode(indent)

    private fun Struct.emit(indent: Int): String {
        val nestedStructs = elements.filterIsInstance<Struct>().associateBy { it.name.pascalCase() }
        val nonStructElements = elements.filter { it !is Struct }
        val fieldsContent = if (fields.isEmpty()) {
            ""
        } else {
            fields.joinToString("") { it.emit(indent + 1, nestedStructs) }
        }
        val nestedContent = nonStructElements.joinToString("") { it.emit(indent) }
        val closingBrace = "}".indentCode(indent)
        val pascalName = name.pascalCase()
        val typeStr = if (fields.isEmpty() && nonStructElements.isEmpty()) {
            "export type $pascalName = {}\n".indentCode(indent)
        } else if (fields.isEmpty()) {
            "export type $pascalName = {}\n".indentCode(indent) + nestedContent
        } else {
            "export type $pascalName = {\n$fieldsContent$closingBrace\n".indentCode(indent) + nestedContent
        }
        val constructorFunctions = constructors.joinToString("") { constructor ->
            emitStructConstructor(pascalName, constructor, indent)
        }
        return typeStr + constructorFunctions
    }

    private fun Field.emit(indent: Int, inlineStructs: Map<String, Struct> = emptyMap()): String {
        val typeStr = type.emitWithInlineStructs(inlineStructs)
        return "\"${name.value()}\": $typeStr,\n".indentCode(indent)
    }

    private fun Type.emitWithInlineStructs(inlineStructs: Map<String, Struct>): String = when (this) {
        is Type.Custom -> inlineStructs[name]?.emitInline() ?: emit()
        is Type.Nullable -> "${type.emitWithInlineStructs(inlineStructs)} | undefined"
        is Type.Array -> {
            val element = elementType.emitWithInlineStructs(inlineStructs)
            if (elementType is Type.Nullable) "($element)[]" else "$element[]"
        }
        else -> emit()
    }

    private fun Struct.emitInline(): String {
        if (fields.isEmpty()) return "{}"
        val nestedStructs = elements.filterIsInstance<Struct>().associateBy { it.name.pascalCase() }
        return "{${fields.joinToString(", ") { field ->
            val typeStr = field.type.emitWithInlineStructs(nestedStructs)
            "\"${field.name.value()}\": $typeStr"
        }}}"
    }

    private fun emitStructConstructor(structName: String, constructor: Constructor, indent: Int): String {
        val funcName = structName.replaceFirstChar { it.lowercaseChar() }
        val paramsTypeName = "${structName}Params"
        val paramNames = constructor.parameters.map { it.name.value() }.toSet()

        // Emit params type
        val paramsTypeContent = if (constructor.parameters.isEmpty()) {
            "{}"
        } else {
            constructor.parameters.joinToString(", ") { param ->
                when (val t = param.type) {
                    is Type.Nullable -> "\"${param.name.value()}\"?: ${t.type.emit()}"
                    else -> "\"${param.name.value()}\": ${param.type.emit()}"
                }
            }.let { "{$it}" }
        }
        val paramsTypeLine = "export type $paramsTypeName = $paramsTypeContent\n".indentCode(indent)

        // Emit constructor arrow function
        val paramsArg = if (constructor.parameters.isEmpty()) "" else "params: $paramsTypeName"
        val bodyAssignments = constructor.body.filterIsInstance<Assignment>()
        val bodyContent = bodyAssignments.joinToString("") { assignment ->
            val value = emitConstructorValue(assignment.value, paramNames)
            "${assignment.name.value()}: $value,\n".indentCode(indent + 1)
        }
        val closingParen = "})".indentCode(indent)
        val funcLine = "export const $funcName = ($paramsArg): $structName => ({\n$bodyContent$closingParen\n".indentCode(indent)

        return paramsTypeLine + funcLine
    }

    private fun emitConstructorValue(expr: Expression, paramNames: Set<String>): String = when (expr) {
        is RawExpression -> when {
            expr.code in paramNames -> "params.${expr.code}"
            else -> expr.code
        }
        is VariableReference -> when {
            expr.name.value() in paramNames -> "params.${expr.name.value()}"
            else -> expr.name.value()
        }
        is EnumReference -> "\"${expr.entry.value()}\""
        is ConstructorStatement -> when {
            expr.type == Type.Unit -> "undefined"
            expr.namedArguments.isEmpty() -> "{}"
            else -> {
                val args = expr.namedArguments.entries.joinToString(", ") { (key, value) ->
                    "\"${key.value()}\": ${emitConstructorArgValue(value, paramNames)}"
                }
                "{$args}"
            }
        }
        is NullLiteral -> "undefined"
        is NullableEmpty -> "undefined"
        else -> expr.emit()
    }

    private fun emitConstructorArgValue(expr: Expression, paramNames: Set<String>): String = when (expr) {
        is RawExpression -> when {
            expr.code in paramNames -> "params[\"${expr.code}\"]"
            else -> expr.code
        }
        is VariableReference -> when {
            expr.name.value() in paramNames -> "params[\"${expr.name.value()}\"]"
            else -> expr.name.value()
        }
        else -> emitConstructorValue(expr, paramNames)
    }

    private fun AstFunction.emit(indent: Int, inlineInterfaces: Map<String, Interface> = emptyMap()): String {
        val retType = returnType
        val rType = retType?.let { ": ${it.emitWithInlineInterfaces(inlineInterfaces)}" } ?: ""

        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { tp ->
                val extendsStr = if (tp.extends.isNotEmpty()) " extends ${tp.extends.joinToString(" & ") { it.emit() }}" else ""
                "${tp.type.emit()}$extendsStr"
            }}>"
        } else {
            ""
        }

        // Detect parameter names that collide with constructor function names
        val renames = parameters
            .filter { it.name.camelCase() in constructorFuncNames }
            .associate { it.name.camelCase() to "_${it.name.camelCase()}" }

        val effectiveParams = parameters.map { p ->
            renames[p.name.camelCase()]?.let { Parameter(Name(listOf(it)), p.type) } ?: p
        }
        val effectiveBody = if (renames.isNotEmpty()) {
            body.map { stmt -> renameVariables(stmt, renames) }
        } else {
            body
        }

        val params = effectiveParams.joinToString(", ") { it.emitWithInlineInterfaces(inlineInterfaces) }
        val prefix = if (isAsync) "async " else ""
        return if (effectiveBody.isEmpty()) {
            val tsRType = if (isAsync) {
                if (retType == null || retType == Type.Unit) {
                    ": Promise<void>"
                } else {
                    ": Promise<${retType.emitWithInlineInterfaces(inlineInterfaces)}>"
                }
            } else {
                rType
            }
            "${name.camelCase()}$typeParamsStr($params)$tsRType;\n".indentCode(indent)
        } else {
            val content = effectiveBody.joinToString("") { it.emit(indent + 1) }
            val closingBrace = "}\n".indentCode(indent)
            "export ${prefix}function ${name.camelCase()}$typeParamsStr($params)$rType {\n$content$closingBrace".indentCode(indent)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Expression> renameVariables(expr: T, renames: Map<String, String>): T {
        if (renames.isEmpty()) return expr
        return when (expr) {
            is VariableReference -> {
                val newName = renames[expr.name.camelCase()] ?: return expr
                VariableReference(Name(listOf(newName))) as T
            }
            is RawExpression -> {
                var code = expr.code
                for ((old, new) in renames) {
                    code = code.replace(old, new)
                }
                RawExpression(code) as T
            }
            is FieldCall -> FieldCall(
                receiver = expr.receiver?.let { renameVariables(it, renames) },
                field = expr.field,
            ) as T
            is FunctionCall -> FunctionCall(
                receiver = expr.receiver?.let { renameVariables(it, renames) },
                typeArguments = expr.typeArguments,
                name = expr.name,
                arguments = expr.arguments.mapValues { renameVariables(it.value, renames) },
            ) as T
            is ArrayIndexCall -> ArrayIndexCall(
                receiver = renameVariables(expr.receiver, renames),
                index = renameVariables(expr.index, renames),
                caseSensitive = expr.caseSensitive,
            ) as T
            is ConstructorStatement -> ConstructorStatement(
                type = expr.type,
                namedArguments = expr.namedArguments.mapValues { renameVariables(it.value, renames) },
            ) as T
            is ReturnStatement -> ReturnStatement(
                expression = renameVariables(expr.expression, renames),
            ) as T
            is Assignment -> Assignment(
                name = expr.name,
                value = renameVariables(expr.value, renames),
                isProperty = expr.isProperty,
            ) as T
            is NullCheck -> NullCheck(
                expression = renameVariables(expr.expression, renames),
                body = renameVariables(expr.body, renames),
                alternative = expr.alternative?.let { renameVariables(it, renames) },
            ) as T
            is NullableMap -> NullableMap(
                expression = renameVariables(expr.expression, renames),
                body = renameVariables(expr.body, renames),
                alternative = renameVariables(expr.alternative, renames),
            ) as T
            is BinaryOp -> BinaryOp(
                left = renameVariables(expr.left, renames),
                operator = expr.operator,
                right = renameVariables(expr.right, renames),
            ) as T
            is Switch -> Switch(
                expression = renameVariables(expr.expression, renames),
                cases = expr.cases.map { case ->
                    case.copy(body = case.body.map { renameVariables(it, renames) })
                },
                default = expr.default?.map { renameVariables(it, renames) },
                variable = expr.variable,
            ) as T
            is PrintStatement -> PrintStatement(
                expression = renameVariables(expr.expression, renames),
            ) as T
            is ErrorStatement -> ErrorStatement(
                message = renameVariables(expr.message, renames),
            ) as T
            is AssertStatement -> AssertStatement(
                expression = renameVariables(expr.expression, renames),
                message = expr.message,
            ) as T
            is LiteralMap -> LiteralMap(
                values = expr.values.mapValues { renameVariables(it.value, renames) },
                keyType = expr.keyType,
                valueType = expr.valueType,
            ) as T
            is LiteralList -> LiteralList(
                values = expr.values.map { renameVariables(it, renames) },
                type = expr.type,
            ) as T
            is EnumValueCall -> EnumValueCall(
                expression = renameVariables(expr.expression, renames),
            ) as T
            is NullableOf -> NullableOf(
                expression = renameVariables(expr.expression, renames),
            ) as T
            is NullableGet -> NullableGet(
                expression = renameVariables(expr.expression, renames),
            ) as T
            else -> expr
        }
    }

    private fun Parameter.emit(): String = "${name.camelCase()}: ${type.emit()}"

    private fun Parameter.emitWithInlineInterfaces(inlineInterfaces: Map<String, Interface>): String = "${name.camelCase()}: ${type.emitWithInlineInterfaces(inlineInterfaces)}"

    private fun Type.emitWithInlineInterfaces(inlineInterfaces: Map<String, Interface>): String = when {
        inlineInterfaces.isEmpty() -> emit()
        this is Type.Custom && inlineInterfaces.containsKey(name) -> {
            val nested = inlineInterfaces[name]!!
            if (nested.elements.isEmpty() && nested.extends.isNotEmpty()) {
                nested.extends.joinToString(" & ") { it.emit() }
            } else if (nested.elements.isEmpty()) {
                "{}"
            } else {
                emit()
            }
        }
        this is Type.Nullable -> "${type.emitWithInlineInterfaces(inlineInterfaces)} | undefined"
        this is Type.Array -> {
            val element = elementType.emitWithInlineInterfaces(inlineInterfaces)
            if (elementType is Type.Nullable) "($element)[]" else "$element[]"
        }
        else -> emit()
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> "number"
        is Type.Number -> "number"
        Type.Any -> "any"
        Type.String -> "string"
        Type.Boolean -> "boolean"
        Type.Bytes -> "Uint8Array"
        Type.Unit -> "void"
        Type.Wildcard -> "unknown"
        Type.Reflect -> "Type"
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
        is Type.Nullable -> "${type.emit()} | undefined"
        is Type.IntegerLiteral -> value.toString()
        is Type.StringLiteral -> "\"$value\""
    }

    private fun emitConstructorCall(type: Type, namedArguments: Map<Name, Expression>): String {
        val typeName = (type as? Type.Custom)?.name
        if (typeName != null && typeName in structsWithConstructors) {
            val funcName = typeName.replaceFirstChar { it.lowercaseChar() }
            if (namedArguments.isEmpty()) return "$funcName()"
            val args = namedArguments.map { "\"${it.key.value()}\": ${it.value.emit()}" }.joinToString(", ")
            return "$funcName({$args})"
        }
        if (type == Type.Unit) return "undefined"
        val named = namedArguments.map { "${it.key.value()}: ${it.value.emit()}" }.joinToString(", ")
        return if (named.isEmpty()) "{}" else "{ $named }"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "console.log(${expression.emit()});\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()};\n".indentCode(indent)
        is ConstructorStatement -> "${emitConstructorCall(type, namedArguments)};\n".indentCode(indent)
        is Literal -> "${emit()};\n".indentCode(indent)
        is LiteralList -> "${emit()};\n".indentCode(indent)
        is LiteralMap -> "${emit()};\n".indentCode(indent)
        is Assignment -> {
            if (isProperty) {
                "${name.value()} = ${value.emit()};\n".indentCode(indent)
            } else {
                "const ${name.camelCase()} = ${value.emit()};\n".indentCode(indent)
            }
        }
        is ErrorStatement -> "throw new Error(${message.emit()});\n".indentCode(indent)
        is AssertStatement -> "if (!(${expression.emit()})) throw new Error('${message.replace("'", "\\'")}');\n".indentCode(indent)
        is Switch -> {
            val isBlockStyle = cases.any { case -> case.body.any { it is Assignment } }
            val casesStr = cases.joinToString("") { case ->
                val bodyStr = case.body.joinToString("") { it.emit(indent + 2) }
                if (isBlockStyle) {
                    "case ${case.value.emit()}: {\n".indentCode(indent + 1) + bodyStr + "}\n".indentCode(indent + 1)
                } else {
                    "case ${case.value.emit()}:\n$bodyStr${"break;\n".indentCode(indent + 2)}".indentCode(indent + 1)
                }
            }
            val defaultStr = default?.let {
                val bodyStr = it.joinToString("") { stmt -> stmt.emit(indent + 2) }
                if (isBlockStyle) {
                    "default: {\n".indentCode(indent + 1) +
                        bodyStr +
                        "}\n".indentCode(indent + 1)
                } else {
                    "default:\n$bodyStr".indentCode(indent + 1)
                }
            } ?: ""
            "${"switch (${expression.emit()}) {\n".indentCode(indent)}$casesStr$defaultStr${"}\n".indentCode(indent)}"
        }
        is RawExpression -> "$code;\n".indentCode(indent)
        is NullLiteral -> "undefined;\n".indentCode(indent)
        is NullableEmpty -> "undefined;\n".indentCode(indent)
        is VariableReference -> "${name.camelCase()};\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value()};\n".indentCode(indent)
        }
        is FunctionCall -> {
            val awaitPrefix = if (isAwait) "await " else ""
            val recv = receiver
            if (recv != null) {
                "$awaitPrefix${recv.emit()}.${name.value()}(${arguments.values.joinToString(", ") { it.emit() }});\n".indentCode(indent)
            } else {
                "$awaitPrefix${name.value()}(${arguments.values.joinToString(", ") { it.emit() }});\n".indentCode(indent)
            }
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}];\n".indentCode(indent)
        } else {
            "Object.entries(${receiver.emit()}).find(([k]) => k.toLowerCase() === ${index.emit()}.toLowerCase())?.[1] ?? null;\n".indentCode(indent)
        }
        is EnumReference -> "${enumType.emit()}.${entry.pascalCase()};\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()};\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toTypeScript()} ${right.emit()});\n".indentCode(indent)
        is TypeDescriptor -> "\"${type.emit()}\";\n".indentCode(indent)
        is NullCheck -> "${emit()};\n".indentCode(indent)
        is NullableMap -> "${emit()};\n".indentCode(indent)
        is NullableOf -> "${emit()};\n".indentCode(indent)
        is NullableGet -> "${emit()};\n".indentCode(indent)
        is Constraint.RegexMatch -> "${emit()};\n".indentCode(indent)
        is Constraint.BoundCheck -> "${emit()};\n".indentCode(indent)
        is NotExpression -> "!${expression.emit()};\n".indentCode(indent)
        is IfExpression -> "${emit()};\n".indentCode(indent)
        is MapExpression -> "${emit()};\n".indentCode(indent)
        is FlatMapIndexed -> "${emit()};\n".indentCode(indent)
        is ListConcat -> "${emit()};\n".indentCode(indent)
        is StringTemplate -> "${emit()};\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toTypeScript(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "==="
        BinaryOp.Operator.NOT_EQUALS -> "!=="
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> emitConstructorCall(type, namedArguments)
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is ClassReference -> TODO("ClassReference emission not yet implemented")
        is RawExpression -> code
        is NullLiteral -> "undefined"
        is NullableEmpty -> "undefined"
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
                "$awaitPrefix${name.value()}(${arguments.values.joinToString(", ") { it.emit() }})"
            }
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}[${index.emit()}]"
        } else {
            "Object.entries(${receiver.emit()}).find(([k]) => k.toLowerCase() === ${index.emit()}.toLowerCase())?.[1]"
        }
        is EnumReference -> "${enumType.emit()}.${entry.pascalCase()}"
        is EnumValueCall -> expression.emit()
        is BinaryOp -> "(${left.emit()} ${operator.toTypeScript()} ${right.emit()})"
        is TypeDescriptor -> "\"${type.emit()}\""
        is NullCheck -> {
            val exprStr = expression.emit()
            // When expression might be undefined (e.g. case-insensitive header lookup),
            // add non-null assertion for the inlined replacement in the body
            val bodyReplacement = if (expression is ArrayIndexCall && !(expression as ArrayIndexCall).caseSensitive) "$exprStr!" else exprStr
            val bodyStr = body.emitWithInlinedIt(bodyReplacement)
            val altStr = alternative?.emit() ?: "undefined"
            "$exprStr != null ? $bodyStr : $altStr"
        }
        is NullableMap -> {
            val exprStr = expression.emit()
            val bodyStr = body.emitWithInlinedIt(exprStr)
            val altStr = alternative.emit()
            "$exprStr != null ? $bodyStr : $altStr"
        }
        is NullableOf -> expression.emit()
        is NullableGet -> "${expression.emit()}!"
        is Constraint.RegexMatch -> "$rawValue.test(${value.emit()})"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" && ").ifEmpty { "true" }
            checks
        }
        is ErrorStatement -> "(() => { throw new Error(${message.emit()}) })()"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in TypeScript")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in TypeScript")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in TypeScript")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in TypeScript")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in TypeScript")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "(${condition.emit()} ? ${thenExpr.emit()} : ${elseExpr.emit()})"
        is MapExpression -> "${receiver.emit()}.map(${variable.camelCase()} => ${body.emit()})"
        is FlatMapIndexed -> "${receiver.emit()}.flatMap((${elementVar.camelCase()}, ${indexVar.camelCase()}) => ${body.emit()})"
        is ListConcat -> when {
            lists.isEmpty() -> "[] as string[]"
            lists.size == 1 -> lists.single().emit()
            else -> "[${lists.joinToString(", ") { "...${it.emit()}" }}]"
        }
        is StringTemplate -> "`${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "\${${it.expression.emit()}}"
            }
        }}`"
    }

    private fun Expression.emitWithInlinedIt(replacement: String): String = when (this) {
        is VariableReference -> if (name.value() == "it") replacement else emit()
        is FunctionCall -> {
            val recv = receiver
            val inlinedArgs = arguments.mapValues { it.value.emitWithInlinedIt(replacement) }
            if (recv != null) {
                "${recv.emitWithInlinedIt(replacement)}.${name.value()}(${inlinedArgs.values.joinToString(", ")})"
            } else {
                "${name.value()}(${inlinedArgs.values.joinToString(", ")})"
            }
        }
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emitWithInlinedIt(replacement)}." } ?: ""
            "$receiverStr${field.value()}"
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emitWithInlinedIt(replacement)}[${index.emitWithInlinedIt(replacement)}]"
        } else {
            "Object.entries(${receiver.emitWithInlinedIt(replacement)}).find(([k]) => k.toLowerCase() === ${index.emitWithInlinedIt(replacement)}.toLowerCase())?.[1]"
        }
        is EnumValueCall -> expression.emitWithInlinedIt(replacement)
        is NotExpression -> "!${expression.emitWithInlinedIt(replacement)}"
        is IfExpression -> "(${condition.emitWithInlinedIt(replacement)} ? ${thenExpr.emitWithInlinedIt(replacement)} : ${elseExpr.emitWithInlinedIt(replacement)})"
        is MapExpression -> "${receiver.emitWithInlinedIt(replacement)}.map(${variable.camelCase()} => ${body.emitWithInlinedIt(replacement)})"
        is FlatMapIndexed -> "${receiver.emitWithInlinedIt(replacement)}.flatMap((${elementVar.camelCase()}, ${indexVar.camelCase()}) => ${body.emitWithInlinedIt(replacement)})"
        is ListConcat -> "[${lists.joinToString(", ") { "...${it.emitWithInlinedIt(replacement)}" }}]"
        is StringTemplate -> "`${parts.joinToString("") {
            when (it) {
                is StringTemplate.Part.Text -> it.value
                is StringTemplate.Part.Expr -> "\${${it.expression.emitWithInlinedIt(replacement)}}"
            }
        }}`"
        is LiteralList -> emit()
        else -> emit()
    }

    private fun LiteralList.emit(): String {
        if (values.isEmpty()) return "[] as ${type.emit()}[]"
        val list = values.joinToString(", ") { it.emit() }
        return "[$list]"
    }

    private fun LiteralMap.emit(): String {
        if (values.isEmpty()) return "{}"
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
