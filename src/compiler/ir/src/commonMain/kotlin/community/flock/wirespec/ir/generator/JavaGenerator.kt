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
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.TypeParameter
import community.flock.wirespec.ir.core.Union
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.Function as AstFunction

object JavaGenerator : Generator {
    override fun generate(element: Element): String = when (element) {
        is File -> {
            val emitter = JavaEmitter(element)
            emitter.emitFile()
        }
        else -> {
            val emitter = JavaEmitter(File(Name.of(""), listOf(element)))
            emitter.emitFile()
        }
    }
}

private class JavaEmitter(val file: File) {
    private val allUnions = file.elements.flatMap { it.findAllUnions() }

    private fun Type.Custom.isInterface(): Boolean {
        if (name.contains("Wirespec") || name.endsWith("Response")) return true
        return file.elements.any {
            (it is Interface && it.name.pascalCase() == this.name) ||
                (it is Union && it.name.pascalCase() == this.name) ||
                (it is Namespace && it.name.pascalCase() == this.name)
        }
    }

    fun emitFile(): String {
        val packages = file.elements.filterIsInstance<Package>()
        val imports = file.elements.filterIsInstance<Import>()
        val otherElements = file.elements.filter { it !is Package && it !is Import }

        val packagesStr = packages.joinToString("") { it.emit(0) }
        val importsStr = imports.joinToString("") { it.emit(0) }
        val elementsStr = otherElements.joinToString("") { it.emit(0, parents = emptyList()) }

        return "$packagesStr$importsStr$elementsStr".removeEmptyLines()
    }

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n").plus("\n")

    private fun Element.findAllUnions(): List<Union> = when (this) {
        is Union -> listOf(this)
        is Struct -> elements.flatMap { it.findAllUnions() }
        is Namespace -> elements.flatMap { it.findAllUnions() }
        is Interface -> elements.flatMap { it.findAllUnions() }
        is Main -> emptyList()
        else -> emptyList()
    }

    private fun String.indentCode(level: Int): String {
        if (level <= 0) return this
        val prefix = " ".repeat(level * 2)
        return this.lines().joinToString("\n") { line ->
            if (line.isEmpty()) line else prefix + line
        }
    }

    private fun Element.emit(indent: Int, isStatic: Boolean = true, parents: List<Element>): String = when (this) {
        is Package -> emit(indent)
        is Import -> emit(indent)
        is Struct -> {
            emit(indent, parents)
        }
        is AstFunction -> {
            val lastParent = parents.lastOrNull()
            val isInterface = lastParent is Interface
            val isStaticContainer = lastParent is Namespace
            val isInterfaceBody = isInterface && body.isNotEmpty()
            val isInsideStruct = lastParent is Struct
            val shouldBeStatic = (isStatic || isStaticContainer || this.isStatic) && !isInterface && (!isInsideStruct || this.isStatic)
            val overridePrefix = if (isOverride) "@Override\n" else ""

            if (indent == 0) {
                emit(indent, isStatic = true, modifier = "public")
            } else if (isInterfaceBody) {
                if (this.isStatic) {
                    emit(indent, isStatic = true, modifier = "public")
                } else {
                    emit(indent, isStatic = false, modifier = "${overridePrefix}default")
                }
            } else {
                val visibility = if (indent == 1) "public" else ""
                val staticStr = if (shouldBeStatic) "static" else ""
                val modParts = listOf(visibility, staticStr).filter { it.isNotEmpty() }
                val modSuffix = modParts.joinToString(" ")
                val fullModifier = if (isOverride) {
                    if (modSuffix.isNotEmpty()) "$overridePrefix$modSuffix" else "@Override"
                } else {
                    modSuffix
                }
                emit(indent, isStatic = shouldBeStatic, modifier = fullModifier)
            }
        }
        is Namespace -> emit(indent, parents)
        is Interface -> emit(indent, parents)
        is Union -> emit(indent, parents)
        is Enum -> emit(indent)
        is Main -> {
            val staticContent = statics.joinToString("") { it.emit(1, true, parents) }
            val content = body.joinToString("") { it.emit(1) }
            "public class ${file.name.pascalCase()} {\n$staticContent" +
                "  public static void main(String[] args) {\n$content  }\n}\n"
        }
        is File -> elements.joinToString("") { it.emit(indent, isStatic, parents) }
        is RawElement -> code.indentCode(indent)
    }

    private fun Package.emit(indent: Int): String = "package $path;\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path.${type.name};\n".indentCode(indent)

    private fun Namespace.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " extends ${it.emitGenerics()}" } ?: ""
        val content = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }
        return "public interface ${name.pascalCase()}$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val isInsideStaticOrInterface = parents.any { it is Namespace || it is Interface }
        val publicStr = if (indent == 0 || isInsideStaticOrInterface) "public " else ""
        val sealedStr = if (isSealed) "sealed " else ""
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extStr = if (extends.isNotEmpty()) " extends ${extends.joinToString(", ") { it.emitGenerics() }}" else ""
        val fieldsContent = fields.joinToString("") { field ->
            "${field.type.emitGenerics()} ${field.name.value()}();\n".indentCode(1)
        }
        val elementsContent = elements.joinToString("") { it.emit(1, isStatic = false, parents = parents + this) }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "$publicStr${sealedStr}interface ${name.pascalCase()}$typeParamsStr$extStr {\n}\n\n".indentCode(indent)
        } else {
            "$publicStr${sealedStr}interface ${name.pascalCase()}$typeParamsStr$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int, parents: List<Element>): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extendsName = extends?.name
        val ext = listOfNotNull(extends?.emitGenerics()) +
            parents.filterIsInstance<Union>().filter { it.name.pascalCase() != extendsName }.map { it.name.pascalCase() } +
            allUnions.filter { it.members.any { m -> m.name == this.name.pascalCase() } }.filter { it.name.pascalCase() != extendsName }.map { it.name.pascalCase() }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.distinct().joinToString(", ")}"
        val permitsStr = if (members.isEmpty()) "" else " permits ${members.joinToString(", ") { it.name }}"
        return "public sealed interface ${name.pascalCase()}$typeParamsStr$extStr$permitsStr {}\n\n".indentCode(indent)
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
        val implStr = extends?.let { " implements ${it.emitGenerics()}" } ?: ""

        val hasContent = fields.isNotEmpty() || constructors.isNotEmpty() || elements.isNotEmpty()
        val terminator = if (hasContent) ";\n" else ""

        val fieldsStr = fields.joinToString("\n") { "public final ${it.type.emitGenerics()} ${it.name.value()};".indentCode(indent + 1) }
        val constructorsStr = constructors.joinToString("\n") { it.emit(name.pascalCase(), fields, indent + 1, false, "") }
        val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
            val isOverride = it.isOverride || it.name.camelCase() == "toString" || it.name.camelCase() == "getLabel"
            val overridePrefix = if (isOverride) "@Override\n${"".indentCode(indent + 1)}" else ""
            val visibility = "public"
            val staticStr = if (it.isStatic) "static" else ""
            val modParts = listOf(visibility, staticStr).filter { it.isNotEmpty() }
            val fullModifier = "$overridePrefix${modParts.joinToString(" ")}"

            it.emit(indent + 1, it.isStatic, fullModifier).trimEnd()
        }

        val content = listOf(fieldsStr, constructorsStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
        val sep = if (content.isNotEmpty()) "\n" else ""

        return ("public enum ${name.pascalCase()}$implStr {\n$entriesStr$terminator$sep$content\n${"}".indentCode(indent)}\n".indentCode(indent)).trimEnd()
    }

    private fun Struct.emit(indent: Int, parents: List<Element>): String {
        val parentUnions = resolveParentUnions(parents)
        val combinedInterfaces = parentUnions + interfaces.map { it.emitGenerics() }
        val implStr = if (combinedInterfaces.isEmpty()) "" else " implements ${combinedInterfaces.distinct().joinToString(", ")}"

        val isInsideStaticOrInterface = parents.any { it is Namespace || it is Interface }
        val typeModifier = when {
            indent == 0 -> "public record"
            isInsideStaticOrInterface -> "public static record"
            else -> "record"
        }

        val customConstructors = constructors.joinToString("") { it.emit(name.pascalCase(), fields, 1, isRecord = true) }
        val nestedContent = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }

        val params = fields.joinToString(",\n") { "${it.type.emitGenerics()} ${it.name.value().sanitize()}".indentCode(1) }
        val paramsStr = if (fields.isEmpty()) " ()" else " (\n$params\n)"

        return "$typeModifier ${name.pascalCase()}$paramsStr$implStr {\n$customConstructors$nestedContent};\n\n".indentCode(indent)
    }

    private fun Struct.resolveParentUnions(parents: List<Element>): List<String> {
        val bodyType = fields.find { it.name.value() == "body" }?.type

        fun Union.emitAsImplements(): String = if (typeParameters.isNotEmpty() && bodyType != null) {
            "${name.pascalCase()}<${bodyType.emitGenerics()}>"
        } else {
            name.pascalCase()
        }

        return (
            parents.filterIsInstance<Union>().map { it.emitAsImplements() } +
                allUnions.filter { it.members.any { m -> m.name == this.name.pascalCase() } }.map { it.emitAsImplements() }
            ).distinct()
    }

    private fun Constructor.emit(structName: String, structFields: List<Field>, indent: Int, isRecord: Boolean, modifier: String = "public"): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val isDelegating = body.any { it is ConstructorStatement }
        val prefix = if (modifier.isEmpty()) "" else "$modifier "

        if (isRecord && !isDelegating) {
            val assignments = body.filterIsInstance<Assignment>().associate {
                it.name.value().removePrefix("this.") to it.value.emit()
            }
            val constructorArgs = structFields.map { field ->
                assignments[field.name.value()] ?: "null"
            }
            val otherStatements = body.filter { it !is Assignment || it.name.value().removePrefix("this.") !in structFields.map { f -> f.name.value() } }
            val bodyContent = (
                listOf("this(${constructorArgs.joinToString(", ")});\n") +
                    otherStatements.map { it.emit(0) }
                )
                .joinToString("") { it.indentCode(1) }

            return "${prefix}$structName($params) {\n$bodyContent}\n".indentCode(indent)
        }

        val bodyContent = body.joinToString("") { it.emit(1, isInsideConstructor = true) }

        return if (isRecord && !isDelegating) {
            "${prefix}$structName {\n$bodyContent}\n".indentCode(indent)
        } else {
            "${prefix}$structName($params) {\n$bodyContent}\n".indentCode(indent)
        }
    }

    private fun AstFunction.emit(indent: Int, isStatic: Boolean, modifier: String): String {
        val rType = if (isAsync) {
            "java.util.concurrent.CompletableFuture<${returnType?.emitGenerics() ?: "Void"}>"
        } else {
            returnType?.takeIf { it != Type.Unit }?.emitGenerics() ?: "void"
        }
        val params = parameters.joinToString(", ") { it.emit(0) }
        val typeParamsStr = if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { it.emit() }}> "
        } else {
            ""
        }
        val prefix = listOfNotNull(
            "public".takeIf { indent == 1 && !modifier.contains("public") },
            "static".takeIf { isStatic && !modifier.contains("static") },
            modifier.takeIf { it.isNotEmpty() },
        ).joinToString(" ")

        val fullPrefix = if (prefix.isEmpty()) "" else "$prefix "

        return if (body.isEmpty()) {
            "$fullPrefix$typeParamsStr$rType ${name.camelCase()}($params);\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "$fullPrefix$typeParamsStr$rType ${name.camelCase()}($params) {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Parameter.emit(indent: Int): String = "${type.emitGenerics()} ${name.camelCase().sanitize()}".indentCode(indent)

    private fun TypeParameter.emit(): String {
        val typeStr = type.emitGenerics()
        return if (extends.isEmpty()) {
            typeStr
        } else {
            "$typeStr extends ${extends.joinToString(" & ") { it.emitGenerics() }}"
        }
    }

    private fun Type.emit(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "Integer"
            Precision.P64 -> "Long"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "Float"
            Precision.P64 -> "Double"
        }
        Type.Any -> "Object"
        Type.String -> "String"
        Type.Bytes -> "byte[]"
        Type.Boolean -> "Boolean"
        Type.Unit -> "Void"
        Type.Wildcard -> "?"
        Type.Reflect -> "Type"
        is Type.Array -> "java.util.List"
        is Type.Dict -> "java.util.Map"
        is Type.Custom -> name
        is Type.Nullable -> "java.util.Optional<${type.emitGenerics()}>"
        is Type.IntegerLiteral -> "Integer"
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
        is Type.Nullable -> "java.util.Optional<${type.emitGenerics()}>"
        else -> emit()
    }

    private fun Statement.emit(indent: Int, isInsideConstructor: Boolean = false): String = when (this) {
        is PrintStatement -> "System.out.println(${expression.emit()});\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()};\n".indentCode(indent)
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "null;\n".indentCode(indent)
            } else {
                val allArgs = namedArguments.map { it.value.emit() }
                val argsStr = when {
                    allArgs.isEmpty() -> "()"
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                if (isInsideConstructor) {
                    "this$argsStr;\n".indentCode(indent)
                } else {
                    "new ${type.emitGenerics()}$argsStr;\n".indentCode(indent)
                }
            }
        }
        is Literal -> "${emit()};\n".indentCode(indent)
        is LiteralList -> "${emit()};\n".indentCode(indent)
        is LiteralMap -> "${emit()};\n".indentCode(indent)
        is Assignment -> {
            val expr = (value as? ConstructorStatement)?.let { constructorStmt ->
                if (constructorStmt.type == Type.Unit) {
                    "null"
                } else {
                    val allArgs = constructorStmt.namedArguments.map { it.value.emit() }
                    val argsStr = when {
                        allArgs.isEmpty() -> "()"
                        allArgs.size == 1 -> "(${allArgs.first()})"
                        else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                    }
                    "new ${constructorStmt.type.emitGenerics()}$argsStr"
                }
            } ?: value.emit()
            if (isProperty) {
                "${name.value().sanitize()} = $expr;\n".indentCode(indent)
            } else {
                "final var ${name.camelCase().sanitize()} = $expr;\n".indentCode(indent)
            }
        }
        is ErrorStatement -> "throw new IllegalStateException(${message.emit()});\n".indentCode(indent)
        is AssertStatement -> "assert ${expression.emit()} : \"$message\";\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                // Use if-else chain with instanceof for pattern matching (Java 16+)
                val casesStr = cases.mapIndexed { index, case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Object"
                    val varName = variable?.camelCase() ?: "_"
                    val prefix = if (index == 0) "if" else " else if"
                    "$prefix (${expression.emit()} instanceof $typeStr $varName) {\n$bodyStr}"
                }.joinToString("")
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    " else {\n$bodyStr}"
                } ?: ""
                "$casesStr$defaultStr\n".indentCode(indent)
            } else {
                // Regular switch with arrow syntax
                val casesStr = cases.joinToString("") { case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    "case ${case.value.emit()} -> {\n$bodyStr}\n".indentCode(indent + 1)
                }
                val defaultStr = default?.let {
                    val bodyStr = it.joinToString("") { stmt -> stmt.emit(1) }
                    "default -> {\n$bodyStr}\n".indentCode(indent + 1)
                } ?: ""
                "switch (${expression.emit()}) {\n$casesStr$defaultStr}\n".indentCode(indent)
            }
        }
        is RawExpression -> "$code;\n".indentCode(indent)
        is NullLiteral -> "null;\n".indentCode(indent)
        is NullableEmpty -> "java.util.Optional.empty();\n".indentCode(indent)
        is VariableReference -> "${name.camelCase().sanitize()};\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}();\n".indentCode(indent)
        }
        is FunctionCall -> {
            val typeArgsStr = if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            val awaitSuffix = if (isAwait) ".join()" else ""
            "$receiverStr$typeArgsStr${name.value().sanitize()}(${arguments.values.joinToString(", ") { it.emit() }})$awaitSuffix;\n".indentCode(indent)
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}.get(${index.emit()});\n".indentCode(indent)
        } else {
            "${receiver.emit()}.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(${index.emit()})).findFirst().map(java.util.Map.Entry::getValue).orElse(null);\n".indentCode(indent)
        }
        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()};\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()}.name();\n".indentCode(indent)
        is BinaryOp -> when {
            operator == BinaryOp.Operator.EQUALS && right is NullLiteral -> "(${left.emit()} == null);\n".indentCode(indent)
            operator == BinaryOp.Operator.NOT_EQUALS && right is NullLiteral -> "(${left.emit()} != null);\n".indentCode(indent)
            operator == BinaryOp.Operator.EQUALS && left is NullLiteral -> "(null == ${right.emit()});\n".indentCode(indent)
            operator == BinaryOp.Operator.NOT_EQUALS && left is NullLiteral -> "(null != ${right.emit()});\n".indentCode(indent)
            operator == BinaryOp.Operator.EQUALS && isPrimitiveLiteral() -> "(${left.emit()} == ${right.emit()});\n".indentCode(indent)
            operator == BinaryOp.Operator.NOT_EQUALS && isPrimitiveLiteral() -> "(${left.emit()} != ${right.emit()});\n".indentCode(indent)
            operator == BinaryOp.Operator.EQUALS -> "(${left.emit()}.equals(${right.emit()}));\n".indentCode(indent)
            operator == BinaryOp.Operator.NOT_EQUALS -> "(!${left.emit()}.equals(${right.emit()}));\n".indentCode(indent)
            else -> "(${left.emit()} ${operator.toJava()} ${right.emit()});\n".indentCode(indent)
        }
        is TypeDescriptor -> error("TypeDescriptor should be transformed before reaching the generator")
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

    private fun BinaryOp.Operator.toJava(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

    private fun BinaryOp.isPrimitiveLiteral(): Boolean = left is Literal &&
        ((left as Literal).type is Type.Integer || (left as Literal).type is Type.Number || (left as Literal).type is Type.Boolean) ||
        right is Literal &&
        ((right as Literal).type is Type.Integer || (right as Literal).type is Type.Number || (right as Literal).type is Type.Boolean)

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> {
            if (type == Type.Unit) {
                "null"
            } else {
                val allArgs = namedArguments.map { it.value.emit() }
                val argsStr = when {
                    allArgs.isEmpty() -> "()"
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                "new ${type.emitGenerics()}$argsStr"
            }
        }
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is NullLiteral -> "null"
        is NullableEmpty -> "java.util.Optional.empty()"
        is VariableReference -> name.camelCase().sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.value().sanitize()}()"
        }
        is FunctionCall -> {
            val typeArgsStr = if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            val awaitSuffix = if (isAwait) ".join()" else ""
            "$receiverStr$typeArgsStr${name.value().sanitize()}(${arguments.values.joinToString(", ") { it.emit() }})$awaitSuffix"
        }
        is ArrayIndexCall -> if (caseSensitive) {
            "${receiver.emit()}.get(${index.emit()})"
        } else {
            "${receiver.emit()}.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(${index.emit()})).findFirst().map(java.util.Map.Entry::getValue).orElse(null)"
        }
        is EnumReference -> "${enumType.emitGenerics()}.${entry.value()}"
        is EnumValueCall -> "${expression.emit()}.name()"
        is BinaryOp -> when {
            operator == BinaryOp.Operator.EQUALS && right is NullLiteral -> "(${left.emit()} == null)"
            operator == BinaryOp.Operator.NOT_EQUALS && right is NullLiteral -> "(${left.emit()} != null)"
            operator == BinaryOp.Operator.EQUALS && left is NullLiteral -> "(null == ${right.emit()})"
            operator == BinaryOp.Operator.NOT_EQUALS && left is NullLiteral -> "(null != ${right.emit()})"
            operator == BinaryOp.Operator.EQUALS && isPrimitiveLiteral() -> "(${left.emit()} == ${right.emit()})"
            operator == BinaryOp.Operator.NOT_EQUALS && isPrimitiveLiteral() -> "(${left.emit()} != ${right.emit()})"
            operator == BinaryOp.Operator.EQUALS -> "(${left.emit()}.equals(${right.emit()}))"
            operator == BinaryOp.Operator.NOT_EQUALS -> "(!${left.emit()}.equals(${right.emit()}))"
            else -> "(${left.emit()} ${operator.toJava()} ${right.emit()})"
        }
        is TypeDescriptor -> error("TypeDescriptor should be transformed before reaching the generator")
        is NullCheck -> {
            val orElse = when (val alt = alternative) {
                is ErrorStatement -> ".orElseThrow(() -> new IllegalStateException(${alt.message.emit()}))"
                null -> ""
                else -> ".orElse(${alt.emit()})"
            }
            "java.util.Optional.ofNullable(${expression.emit()}).map(it -> ${body.emit()})$orElse"
        }
        is NullableMap -> {
            val orElse = when (val alt = alternative) {
                is ErrorStatement -> "orElseThrow(() -> new IllegalStateException(${alt.message.emit()}))"
                else -> "orElse(${alternative.emit()})"
            }
            "${expression.emit()}.map(it -> ${body.emit()}).$orElse"
        }
        is NullableOf -> "java.util.Optional.of(${expression.emit()})"
        is NullableGet -> "${expression.emit()}.get()"
        is Constraint.RegexMatch -> "java.util.regex.Pattern.compile(\"${pattern.replace("\\", "\\\\")}\").matcher(${value.emit()}).find()"
        is Constraint.BoundCheck -> {
            val checks = listOfNotNull(
                min?.let { "$it <= ${value.emit()}" },
                max?.let { "${value.emit()} <= $it" },
            ).joinToString(" && ").ifEmpty { "true" }
            checks
        }
        is ErrorStatement -> "throw new IllegalStateException(${message.emit()});"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Java")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Java")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Java")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Java")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Java")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "(${condition.emit()} ? ${thenExpr.emit()} : ${elseExpr.emit()})"
        is MapExpression -> "${receiver.emit()}.stream().map(${variable.camelCase()} -> ${body.emit()}).toList()"
        is FlatMapIndexed -> {
            val recv = receiver.emit()
            val bodyWithSubstitution = body.emitWithSubstitution(elementVar, "$recv.get(${indexVar.camelCase()})")
            "java.util.stream.IntStream.range(0, $recv.size()).mapToObj(${indexVar.camelCase()} -> $bodyWithSubstitution).flatMap(java.util.Collection::stream).toList()"
        }
        is ListConcat -> when {
            lists.isEmpty() -> "java.util.List.of()"
            lists.size == 1 -> lists.single().emit()
            else -> "java.util.stream.Stream.of(${lists.joinToString(", ") { it.emit() }}).flatMap(java.util.Collection::stream).toList()"
        }
        is StringTemplate -> parts.joinToString(" + ") {
            when (it) {
                is StringTemplate.Part.Text -> "\"${it.value}\""
                is StringTemplate.Part.Expr -> it.expression.emit()
            }
        }
    }

    private fun Expression.emitWithSubstitution(varName: Name, replacement: String): String = when (this) {
        is VariableReference -> if (name == varName) replacement else emit()
        is FunctionCall -> {
            val recv = receiver?.emitWithSubstitution(varName, replacement)
            val args = arguments.values.map { it.emitWithSubstitution(varName, replacement) }
            val typeArgsStr = if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = recv?.let { "$it." } ?: ""
            "$receiverStr$typeArgsStr${name.value().sanitize()}(${args.joinToString(", ")})"
        }
        is FieldCall -> {
            val recv = receiver?.emitWithSubstitution(varName, replacement) ?: ""
            val dot = if (recv.isNotEmpty()) "." else ""
            "$recv$dot${field.value().sanitize()}()"
        }
        is NotExpression -> "!${expression.emitWithSubstitution(varName, replacement)}"
        is IfExpression -> "(${condition.emitWithSubstitution(varName, replacement)} ? ${thenExpr.emitWithSubstitution(varName, replacement)} : ${elseExpr.emitWithSubstitution(varName, replacement)})"
        is MapExpression -> "${receiver.emitWithSubstitution(varName, replacement)}.stream().map(${variable.camelCase()} -> ${body.emitWithSubstitution(varName, replacement)}).toList()"
        is LiteralList -> {
            if (values.isEmpty()) {
                "java.util.List.<${type.emit()}>of()"
            } else {
                val list = values.map { it.emitWithSubstitution(varName, replacement) }.joinToString(", ")
                "java.util.List.of($list)"
            }
        }
        is StringTemplate -> parts.joinToString(" + ") {
            when (it) {
                is StringTemplate.Part.Text -> "\"${it.value}\""
                is StringTemplate.Part.Expr -> it.expression.emitWithSubstitution(varName, replacement)
            }
        }
        else -> emit()
    }

    private fun LiteralList.emit(): String {
        if (values.isEmpty()) return "java.util.List.<${type.emit()}>of()"
        val list = values.joinToString(", ") { it.emit() }
        return "java.util.List.of($list)"
    }

    private fun LiteralMap.emit(): String {
        if (values.isEmpty()) return "java.util.Collections.emptyMap()"
        val map = values.entries.joinToString(", ") {
            "java.util.Map.entry(${Literal(it.key, keyType).emit()}, ${it.value.emit()})"
        }
        return "java.util.Map.ofEntries($map)"
    }

    private fun Literal.emit(): String = when {
        type is Type.String -> "\"$value\""
        value is Long -> "${value}L"
        else -> value.toString()
    }

}

private fun String.sanitize(): String = if (reservedKeywords.contains(this)) "_$this" else this

private val reservedKeywords = setOf(
    "abstract", "continue", "for", "new", "switch",
    "assert", "default", "if", "package", "synchronized",
    "boolean", "do", "goto", "private", "this",
    "break", "double", "implements", "protected", "throw",
    "byte", "else", "import", "public", "throws",
    "case", "enum", "instanceof", "return", "transient",
    "catch", "extends", "int", "short", "try",
    "char", "final", "interface", "static", "void",
    "class", "finally", "long", "strictfp", "volatile",
    "const", "float", "native", "super", "while",
    "true", "false", "null",
)
