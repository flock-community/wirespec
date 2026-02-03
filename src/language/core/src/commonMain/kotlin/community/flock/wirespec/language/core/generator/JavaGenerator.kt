package community.flock.wirespec.language.core.generator

import community.flock.wirespec.language.core.Assignment
import community.flock.wirespec.language.core.Call
import community.flock.wirespec.language.core.Constructor
import community.flock.wirespec.language.core.ConstructorStatement
import community.flock.wirespec.language.core.Element
import community.flock.wirespec.language.core.Enum
import community.flock.wirespec.language.core.ErrorStatement
import community.flock.wirespec.language.core.Expression
import community.flock.wirespec.language.core.Field
import community.flock.wirespec.language.core.File
import community.flock.wirespec.language.core.Import
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Literal
import community.flock.wirespec.language.core.LiteralList
import community.flock.wirespec.language.core.LiteralMap
import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Precision
import community.flock.wirespec.language.core.PrintStatement
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.ReturnStatement
import community.flock.wirespec.language.core.Statement
import community.flock.wirespec.language.core.Static
import community.flock.wirespec.language.core.Struct
import community.flock.wirespec.language.core.Switch
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.Union
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

    private fun String.removeEmptyLines(): String = lines().filter { it.isNotEmpty() }.joinToString("\n")

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
    }

    private fun Package.emit(indent: Int): String = "package $path;\n\n".indentCode(indent)

    private fun Import.emit(indent: Int): String = "import $path;\n".indentCode(indent)

    private fun Static.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " extends ${it.emit()}" } ?: ""
        val content = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }
        return "public interface $name$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Interface.emit(indent: Int, parents: List<Element>): String {
        val extStr = extends?.let { " extends ${it.emit()}" } ?: ""
        val content = elements.joinToString("") { it.emit(1, isStatic = false, parents = parents + this) }
        return "public interface $name$extStr {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
    }

    private fun Union.emit(indent: Int, parents: List<Element>): String {
        val ext = listOfNotNull(extends?.emit()) +
            parents.filterIsInstance<Union>().map { it.name } +
            allUnions.filter { it.members.contains(this.name) }.map { it.name }

        val extStr = if (ext.isEmpty()) "" else " extends ${ext.distinct().joinToString(", ")}"
        return "public sealed interface $name$extStr {}\n\n".indentCode(indent)
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
        val implStr = extends?.let { " implements ${it.emit()}" } ?: ""

        val hasContent = fields.isNotEmpty() || constructors.isNotEmpty() || elements.isNotEmpty()
        val terminator = if (hasContent) ";\n" else ""

        val fieldsStr = fields.joinToString("\n") { "public final ${it.type.emit()} ${it.name};".indentCode(indent + 1) }
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
        val typeModifier = if (indent == 0) "public record" else "public static record"

        val combinedInterfaces = parentUnions + interfaces.map { it.emit() }
        val implStr = if (combinedInterfaces.isEmpty()) "" else " implements ${combinedInterfaces.distinct().joinToString(", ")}"

        val customConstructors = constructors.joinToString("") { it.emit(name, fields, 1, isRecord = true) }
        val nestedContent = elements.joinToString("") { it.emit(1, isStatic = true, parents = parents + this) }

        val params = fields.joinToString(",\n") { "${it.type.emit(true)} ${it.name.sanitize()}".indentCode(1) }
        val paramsStr = if (fields.isEmpty()) "()" else "(\n$params\n)"

        return "$typeModifier $name $paramsStr$implStr {\n$customConstructors$nestedContent};\n\n".indentCode(indent)
    }

    private fun Struct.resolveParentUnions(parents: List<Element>): List<String> = (
        parents.filterIsInstance<Union>().map { it.name } +
            allUnions.filter { it.members.contains(this.name) }.map { it.name }
        )
        .distinct()

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
            "java.util.concurrent.CompletableFuture<${returnType?.emit(true) ?: "Void"}>"
        } else {
            returnType?.emit() ?: "void"
        }
        val params = parameters.joinToString(", ") { it.emit(0) }
        val prefix = listOfNotNull(
            "public".takeIf { indent == 1 && !modifier.contains("public") },
            "static".takeIf { isStatic && !modifier.contains("static") },
            modifier.takeIf { it.isNotEmpty() },
        ).joinToString(" ")

        val fullPrefix = if (prefix.isEmpty()) "" else "$prefix "

        return if (body.isEmpty()) {
            "$fullPrefix$rType $name($params);\n".indentCode(indent)
        } else {
            val content = body.joinToString("") { it.emit(1) }
            "$fullPrefix$rType $name($params) {\n$content${"}".indentCode(0)}\n\n".indentCode(indent)
        }
    }

    private fun Parameter.emit(indent: Int): String = "${type.emit()} ${name.sanitize()}".indentCode(indent)

    private fun Type.emit(boxed: Boolean = false): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> if (boxed) "Integer" else "int"
            Precision.P64 -> if (boxed) "Long" else "long"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> if (boxed) "Float" else "float"
            Precision.P64 -> if (boxed) "Double" else "double"
        }
        Type.String -> "String"
        Type.Bytes -> "byte[]"
        Type.Boolean -> if (boxed) "Boolean" else "boolean"
        Type.Unit -> if (boxed) "Void" else "void"
        is Type.Array -> "java.util.List<${elementType.emit(true)}>"
        is Type.Dict -> "java.util.Map<${keyType.emit(true)}, ${valueType.emit(true)}>"
        is Type.Custom -> {
            if (generics.isEmpty()) {
                name
            } else {
                "$name<${generics.joinToString(", ") { it.emit(true) }}>"
            }
        }
        is Type.Nullable -> "java.util.Optional<${type.emit(true)}>"
    }

    private fun Statement.emit(indent: Int, isInsideConstructor: Boolean = false): String = when (this) {
        is PrintStatement -> "System.out.println(${expression.emit()});\n".indentCode(indent)
        is ReturnStatement -> "return ${expression.emit()};\n".indentCode(indent)
        is ConstructorStatement -> {
            val allArgs = namedArguments.map { it.value.emit() }
            val argsStr = when {
                allArgs.isEmpty() -> "()"
                allArgs.size == 1 -> "(${allArgs.first()})"
                else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
            }
            if (isInsideConstructor) {
                "this$argsStr;\n".indentCode(indent)
            } else {
                "new ${type.emit()}$argsStr;\n".indentCode(indent)
            }
        }
        is Call -> {
            "$name(${arguments.map { it.value.emit() }.joinToString(", ")});\n".indentCode(indent)
        }
        is Literal -> "${emit()};\n".indentCode(indent)
        is LiteralList -> "${emit()};\n".indentCode(indent)
        is LiteralMap -> "${emit()};\n".indentCode(indent)
        is Assignment -> {
            val expr = if (value is ConstructorStatement) {
                val allArgs = value.namedArguments.map { it.value.emit() }
                val argsStr = when {
                    allArgs.isEmpty() -> "()"
                    allArgs.size == 1 -> "(${allArgs.first()})"
                    else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
                }
                "new ${value.type.emit()}$argsStr"
            } else {
                value.emit()
            }
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
                    val typeStr = case.type?.emit() ?: "Object"
                    val varName = case.variable ?: "_"
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
    }

    private fun Expression.emit(): String = when (this) {
        is Call -> "$name(${arguments.map { it.value.emit() }.joinToString(", ")})"
        is ConstructorStatement -> {
            val allArgs = namedArguments.map { it.value.emit() }
            val argsStr = when {
                allArgs.isEmpty() -> "()"
                allArgs.size == 1 -> "(${allArgs.first()})"
                else -> "(\n${allArgs.joinToString(",\n") { it.indentCode(1) }}\n)"
            }
            "new ${type.emit()}$argsStr"
        }
        is Literal -> emit()
        is LiteralList -> emit()
        is LiteralMap -> emit()
        is RawExpression -> code
        is ErrorStatement -> throw IllegalArgumentException("ErrorStatement cannot be an expression in Java")
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
