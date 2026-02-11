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
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Precision
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
import community.flock.wirespec.language.core.TypeParameter
import community.flock.wirespec.language.core.Union
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.Function as AstFunction

object JavaGenerator : CodeGenerator {
    override fun generate(element: Element): String = when (element) {
        is File -> {
            val emitter = JavaEmitter(element)
            emitter.emitFile()
        }
        else -> {
            val emitter = JavaEmitter(File("", listOf(element)))
            emitter.emitFile()
        }
    }
}

private class JavaEmitter(val file: File) {
    private val allUnions = file.elements.flatMap { it.findAllUnions() }

    private fun Type.Custom.isInterface(): Boolean {
        if (name.contains("Wirespec") || name.endsWith("Response")) return true
        return file.elements.any {
            (it is Interface && it.name == this.name) ||
                (it is Union && it.name == this.name) ||
                (it is Static && it.name == this.name)
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
        is Static -> elements.flatMap { it.findAllUnions() }
        is Interface -> elements.flatMap { it.findAllUnions() }
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
            val isStaticContainer = lastParent is Static
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
        is Static -> emit(indent, parents)
        is Interface -> emit(indent, parents)
        is Union -> emit(indent, parents)
        is Enum -> emit(indent)
        is File -> elements.joinToString("") { it.emit(indent, isStatic, parents) }
        is RawElement -> code.indentCode(indent)
    }

    private fun Package.emit(indent: Int): String = "package $path;\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path.${type.name};\n".indentCode(indent)

    private fun Static.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " extends ${it.emitGenerics()}" } ?: ""
        val content = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }
        return "public interface $name$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val isInsideStaticOrInterface = parents.any { it is Static || it is Interface }
        val publicStr = if (indent == 0 || isInsideStaticOrInterface) "public " else ""
        val sealedStr = if (isSealed) "sealed " else ""
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extStr = if (extends.isNotEmpty()) " extends ${extends.joinToString(", ") { it.emitGenerics() }}" else ""
        val fieldsContent = fields.joinToString("") { field ->
            "${field.type.emitGenerics()} ${field.name}();\n".indentCode(1)
        }
        val elementsContent = elements.joinToString("") { it.emit(1, isStatic = false, parents = parents + this) }
        val content = fieldsContent + elementsContent
        return if (content.isEmpty()) {
            "$publicStr${sealedStr}interface $name$typeParamsStr$extStr {\n}\n\n".indentCode(indent)
        } else {
            "$publicStr${sealedStr}interface $name$typeParamsStr$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Union.emit(indent: Int, parents: List<Element>): String {
        val typeParamsStr = if (typeParameters.isNotEmpty()) "<${typeParameters.joinToString(", ") { it.emit() }}>" else ""
        val extendsName = extends?.name
        val ext = listOfNotNull(extends?.emitGenerics()) +
            parents.filterIsInstance<Union>().filter { it.name != extendsName }.map { it.name } +
            allUnions.filter { it.members.any { m -> m.name == this.name } }.filter { it.name != extendsName }.map { it.name }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.distinct().joinToString(", ")}"
        val permitsStr = if (members.isEmpty()) "" else " permits ${members.joinToString(", ") { it.name }}"
        return "public sealed interface $name$typeParamsStr$extStr$permitsStr {}\n\n".indentCode(indent)
    }

    private fun Enum.emit(indent: Int): String {
        val entriesStr = entries.joinToString(",\n") { entry ->
            val e = if (entry.values.isEmpty()) {
                entry.name
            } else {
                "${entry.name}(${entry.values.joinToString(", ")})"
            }
            e.indentCode(indent + 1)
        }
        val implStr = extends?.let { " implements ${it.emitGenerics()}" } ?: ""

        val hasContent = fields.isNotEmpty() || constructors.isNotEmpty() || elements.isNotEmpty()
        val terminator = if (hasContent) ";\n" else ""

        val fieldsStr = fields.joinToString("\n") { "public final ${it.type.emitGenerics()} ${it.name};".indentCode(indent + 1) }
        val constructorsStr = constructors.joinToString("\n") { it.emit(name, fields, indent + 1, false, "") }
        val functionsStr = elements.filterIsInstance<AstFunction>().joinToString("\n") {
            val isOverride = it.isOverride || it.name == "toString" || it.name == "getLabel"
            val overridePrefix = if (isOverride) "@Override\n${"".indentCode(indent + 1)}" else ""
            val visibility = "public"
            val staticStr = if (it.isStatic) "static" else ""
            val modParts = listOf(visibility, staticStr).filter { it.isNotEmpty() }
            val fullModifier = "$overridePrefix${modParts.joinToString(" ")}"

            it.emit(indent + 1, it.isStatic, fullModifier).trimEnd()
        }

        val content = listOf(fieldsStr, constructorsStr, functionsStr).filter { it.isNotEmpty() }.joinToString("\n")
        val sep = if (content.isNotEmpty()) "\n" else ""

        return ("public enum $name$implStr {\n$entriesStr$terminator$sep$content\n${"}".indentCode(indent)}\n".indentCode(indent)).trimEnd()
    }

    private fun Struct.emit(indent: Int, parents: List<Element>): String {
        val parentUnions = resolveParentUnions(parents)
        val combinedInterfaces = parentUnions + interfaces.map { it.emitGenerics() }
        val implStr = if (combinedInterfaces.isEmpty()) "" else " implements ${combinedInterfaces.distinct().joinToString(", ")}"

        val isInsideStaticOrInterface = parents.any { it is Static || it is Interface }
        val typeModifier = when {
            indent == 0 -> "public record"
            isInsideStaticOrInterface -> "public static record"
            else -> "record"
        }

        val customConstructors = constructors.joinToString("") { it.emit(name, fields, 1, isRecord = true) }
        val nestedContent = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }

        val params = fields.joinToString(",\n") { "${it.type.emitGenerics()} ${it.name.sanitize()}".indentCode(1) }
        val paramsStr = if (fields.isEmpty()) " ()" else " (\n$params\n)"

        return "$typeModifier $name$paramsStr$implStr {\n$customConstructors$nestedContent};\n\n".indentCode(indent)
    }

    private fun Struct.resolveParentUnions(parents: List<Element>): List<String> {
        val bodyType = fields.find { it.name == "body" }?.type

        fun Union.emitAsImplements(): String = if (typeParameters.isNotEmpty() && bodyType != null) {
            "$name<${bodyType.emitGenerics()}>"
        } else {
            name
        }

        return (
            parents.filterIsInstance<Union>().map { it.emitAsImplements() } +
                allUnions.filter { it.members.any { m -> m.name == this.name } }.map { it.emitAsImplements() }
            ).distinct()
    }

    private fun Constructor.emit(structName: String, structFields: List<Field>, indent: Int, isRecord: Boolean, modifier: String = "public"): String {
        val params = parameters.joinToString(", ") { it.emit(0) }
        val isDelegating = body.any { it is ConstructorStatement }
        val prefix = if (modifier.isEmpty()) "" else "$modifier "

        if (isRecord && !isDelegating) {
            val assignments = body.filterIsInstance<Assignment>().associate {
                it.name.removePrefix("this.") to it.value.emit()
            }
            val constructorArgs = structFields.map { field ->
                assignments[field.name] ?: "null"
            }
            val otherStatements = body.filter { it !is Assignment || it.name.removePrefix("this.") !in structFields.map { f -> f.name } }
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
            "$fullPrefix$typeParamsStr$rType $name($params);\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "$fullPrefix$typeParamsStr$rType $name($params) {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Parameter.emit(indent: Int): String = "${type.emitGenerics()} ${name.sanitize()}".indentCode(indent)

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
        is Type.Array -> "java.util.List"
        is Type.Dict -> "java.util.Map"
        is Type.Custom -> name
        is Type.Nullable -> "java.util.Optional<${type.emitGenerics()}>"
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
                "${name.sanitize()} = $expr;\n".indentCode(indent)
            } else {
                "final var ${name.sanitize()} = $expr;\n".indentCode(indent)
            }
        }
        is ErrorStatement -> "throw new IllegalStateException(${message.emit()});\n".indentCode(indent)
        is Switch -> {
            val isPatternSwitch = cases.any { it.type != null }
            if (isPatternSwitch) {
                // Use if-else chain with instanceof for pattern matching (Java 16+)
                val casesStr = cases.mapIndexed { index, case ->
                    val bodyStr = case.body.joinToString("") { it.emit(1) }
                    val typeStr = case.type?.emitGenerics() ?: "Object"
                    val varName = variable ?: "_"
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
        is NullLiteral -> "null;\n".indentCode(indent)
        is NullableEmpty -> "java.util.Optional.empty();\n".indentCode(indent)
        is VariableReference -> "${name.sanitize()};\n".indentCode(indent)
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.sanitize()}();\n".indentCode(indent)
        }
        is FunctionCall -> {
            val typeArgsStr = if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr$typeArgsStr${name.sanitize()}(${arguments.values.joinToString(", ") { it.emit() }});\n".indentCode(indent)
        }
        is ArrayIndexCall -> "${receiver.emit()}.get(${index.emit()});\n".indentCode(indent)
        is EnumReference -> "${enumType.emitGenerics()}.$entry;\n".indentCode(indent)
        is EnumValueCall -> "${expression.emit()}.name();\n".indentCode(indent)
        is BinaryOp -> "(${left.emit()} ${operator.toJava()} ${right.emit()});\n".indentCode(indent)
        is TypeDescriptor -> "${emitTypeDescriptor()};\n".indentCode(indent)
        is NullCheck -> "${emit()};\n".indentCode(indent)
        is NullableMap -> "${emit()};\n".indentCode(indent)
        is NullableOf -> "${emit()};\n".indentCode(indent)
    }

    private fun BinaryOp.Operator.toJava(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
    }

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
        is VariableReference -> name.sanitize()
        is FieldCall -> {
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr${field.sanitize()}()"
        }
        is FunctionCall -> {
            val typeArgsStr = if (typeArguments.isNotEmpty()) "<${typeArguments.joinToString(", ") { it.emitGenerics() }}>" else ""
            val receiverStr = receiver?.let { "${it.emit()}." } ?: ""
            "$receiverStr$typeArgsStr${name.sanitize()}(${arguments.values.joinToString(", ") { it.emit() }})"
        }
        is ArrayIndexCall -> "${receiver.emit()}.get(${index.emit()})"
        is EnumReference -> "${enumType.emitGenerics()}.$entry"
        is EnumValueCall -> "${expression.emit()}.name()"
        is BinaryOp -> "(${left.emit()} ${operator.toJava()} ${right.emit()})"
        is TypeDescriptor -> emitTypeDescriptor()
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
        is ErrorStatement -> "throw new IllegalStateException(${message.emit()});"
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Java")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Java")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Java")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Java")
    }

    private fun LiteralList.emit(): String {
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

    private fun Literal.emit(): String = when (type) {
        Type.String -> "\"$value\""
        else -> value.toString()
    }

    private fun TypeDescriptor.emitTypeDescriptor(): String {
        fun Type.findRoot(): Type = when (this) {
            is Type.Nullable -> this.type.findRoot()
            is Type.Array -> this.elementType.findRoot()
            is Type.Dict -> this.valueType.findRoot()
            else -> this
        }
        fun Type.emitRawContainer(): String? = when (this) {
            is Type.Nullable -> "java.util.Optional"
            is Type.Array -> "java.util.List"
            is Type.Dict -> "java.util.Map"
            else -> null
        }
        val rootType = type.findRoot().emit()
        val containerType = type.emitRawContainer()?.let { "$it.class" } ?: "null"
        return "Wirespec.getType($rootType.class, $containerType)"
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
