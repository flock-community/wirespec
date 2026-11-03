package community.flock.wirespec.compiler.core.ir.generator

import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.ir.ArrayIndexCall
import community.flock.wirespec.compiler.core.ir.AssertStatement
import community.flock.wirespec.compiler.core.ir.Assignment
import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.Case
import community.flock.wirespec.compiler.core.ir.Cast
import community.flock.wirespec.compiler.core.ir.ClassReference
import community.flock.wirespec.compiler.core.ir.Constraint
import community.flock.wirespec.compiler.core.ir.Constructor
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Enum
import community.flock.wirespec.compiler.core.ir.EnumReference
import community.flock.wirespec.compiler.core.ir.EnumValueCall
import community.flock.wirespec.compiler.core.ir.ErrorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FlatMapEntries
import community.flock.wirespec.compiler.core.ir.FlatMapIndexed
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.IfExpression
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Lambda
import community.flock.wirespec.compiler.core.ir.ListConcat
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.Main
import community.flock.wirespec.compiler.core.ir.MapExpression
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NotExpression
import community.flock.wirespec.compiler.core.ir.NullCheck
import community.flock.wirespec.compiler.core.ir.NullLiteral
import community.flock.wirespec.compiler.core.ir.NullableEmpty
import community.flock.wirespec.compiler.core.ir.NullableGet
import community.flock.wirespec.compiler.core.ir.NullableMap
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.PrintStatement
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Statement
import community.flock.wirespec.compiler.core.ir.StringTemplate
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Switch
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.TypeDescriptor
import community.flock.wirespec.compiler.core.ir.TypeParameter
import community.flock.wirespec.compiler.core.ir.Union
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.ir.substitute
import community.flock.wirespec.compiler.core.ir.Function as AstFunction

/**
 * Renders the IR as PHP 8.2. PHP cannot nest declarations, so a type declared inside another
 * (an endpoint's `Request`, the shared `Request.Headers`) moves to a namespace named after its
 * container: IR `PutTodo.Request` becomes `PutTodo\Request`, and a dotted IR name always maps to
 * a backslash-separated PHP name. A file whose declarations end up in several namespaces renders
 * one namespace block per namespace, deepest first, each with the file's `use` imports.
 *
 * Generics have no runtime presence in PHP: they become `@template`, `@implements` and typed
 * `@param`/`@return` docblocks next to the native types, and a [TypeDescriptor] becomes a
 * PHPDoc type string such as `'list<' . TodoDto::class . '>'` that a serializer can parse.
 */
public object PhpGenerator :
    Generator,
    Keywords {
    override val reservedKeywords: Set<String> = setOf(
        "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone",
        "const", "continue", "declare", "default", "do", "echo", "else", "elseif", "empty",
        "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit",
        "extends", "final", "finally", "fn", "for", "foreach", "function", "global", "goto", "if",
        "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset",
        "list", "match", "namespace", "new", "or", "print", "private", "protected", "public",
        "readonly", "require", "require_once", "return", "static", "switch", "throw", "trait",
        "try", "unset", "use", "var", "while", "xor", "yield",
    )

    // Names no class may have, compared case-insensitively: the keywords plus the built-in types.
    private val reservedClassNames: Set<String> = reservedKeywords + setOf(
        "bool", "false", "float", "int", "iterable", "mixed", "never", "null", "numeric",
        "object", "parent", "resource", "self", "string", "true", "void",
    )

    private const val INDENT = 4

    private const val MAX_LINE = 120

    override fun generate(element: Element): String = when (element) {
        is File -> element
        else -> File(Name.of(""), listOf(element))
    }.emit()

    /** Renders a single expression, for emitters that splice generated code into raw templates. */
    public fun generateExpression(expression: Expression): String = expression.emit(Scope())

    private data class Block(val namespace: String, val code: String)

    /** What a declaration sees: the generic parameters in scope and how to refer to types declared next to it. */
    private data class Scope(
        val typeParameters: Set<String> = emptySet(),
        val siblings: Map<String, String> = emptyMap(),
    ) {
        fun withTypeParameters(parameters: List<TypeParameter>): Scope = copy(typeParameters = typeParameters + parameters.mapNotNull { (it.type as? Type.Custom)?.name?.referenceName() })

        fun qualifying(container: String, names: List<String>): Scope = copy(siblings = siblings + names.associateWith { "$container\\$it" })
    }

    private fun String.indentCode(level: Int): String = indentLines(level, INDENT)

    private fun File.emit(): String {
        val namespace = elements.filterIsInstance<Package>().firstOrNull()?.path.orEmpty()
        val uses = elements.filterIsInstance<Import>().joinToString("") { it.emit() }
        val blocks = elements
            .filterNot { it is Package || it is Import }
            .flatMap { it.declarations(namespace, Scope()) }
            .groupBy { it.namespace }
            .toList()
            .sortedByDescending { (name, _) -> name.count { it == '\\' } + (if (name.isEmpty()) 0 else 1) }
        val body = blocks.joinToString("\n") { (name, declarations) ->
            listOf(
                name.takeIf { it.isNotEmpty() }?.let { "namespace $it;\n" }.orEmpty(),
                uses,
                declarations.joinToString("\n") { it.code },
            ).filter { it.isNotEmpty() }.joinToString("\n")
        }
        return "<?php\n\ndeclare(strict_types=1);\n\n$body".trimEnd() + "\n"
    }

    private fun Import.emit(): String = "use ${path.wrapIfNotEmpty("", "\\")}${type.name.value()};\n"

    private fun Element.declarations(namespace: String, scope: Scope): List<Block> = when (this) {
        is Struct -> hoisted(namespace, name.pascalCase(), elements, scope) + Block(namespace, emit(scope))
        is Interface -> hoisted(namespace, name.pascalCase(), elements, scope) + Block(namespace, emit(scope))
        is Namespace -> hoisted(namespace, name.pascalCase(), elements, scope) + listOfNotNull(emit(scope)?.let { Block(namespace, it) })
        is Enum -> listOf(Block(namespace, emit(scope)))
        is Union -> listOf(Block(namespace, emit(scope)))
        is File -> elements.flatMap { it.declarations(namespace, scope) }
        is AstFunction -> listOf(Block(namespace, emit(scope, isInterface = false, isStatic = false)))
        is Main -> listOf(Block(namespace, emit(scope)))
        is RawElement -> listOf(Block(namespace, "$code\n"))
        is Package, is Import, is Field -> emptyList()
    }

    private fun hoisted(namespace: String, container: String, elements: List<Element>, scope: Scope): List<Block> = elements.filter { it.isDeclaration() }
        .flatMap { it.declarations("$namespace\\$container".removePrefix("\\"), scope) }

    private fun Element.isDeclaration(): Boolean = this is Struct || this is Interface || this is Namespace || this is Enum || this is Union

    private fun Element.declaredName(): String? = when (this) {
        is Struct -> name.pascalCase()
        is Interface -> name.pascalCase()
        is Namespace -> name.pascalCase()
        is Enum -> name.pascalCase()
        is Union -> name.pascalCase()
        else -> null
    }

    private fun List<Element>.nestedScope(container: String, scope: Scope): Scope = scope.qualifying(container, mapNotNull { it.declaredName() })

    private fun docBlock(lines: List<String>, indent: Int = 0): String = when {
        lines.isEmpty() -> ""
        else -> (listOf("/**") + lines.map { " * $it" } + " */").joinToString("\n", postfix = "\n").indentCode(indent)
    }

    private fun List<TypeParameter>.templates(scope: Scope): List<String> = map { "@template ${it.type.doc(scope)}" }

    private fun List<Type.Custom>.genericDocs(tag: String, scope: Scope): List<String> = filter { it.generics.isNotEmpty() }.map { "$tag ${it.doc(scope)}" }

    private fun Struct.emit(outer: Scope): String {
        val scope = elements.nestedScope(name.pascalCase(), outer).withTypeParameters(typeParameters)
        val fields = fieldList()
        val readonly = fields.none { it.isMutable }
        val header = docBlock(typeParameters.templates(scope) + interfaces.genericDocs("@implements", scope)) +
            "final ${if (readonly) "readonly " else ""}class ${name.pascalCase().className()}" +
            interfaces.joinNonEmpty(", ", " implements ") { it.native(scope) }
        val members = buildList {
            if (constructors.isEmpty()) {
                if (fields.isNotEmpty()) add(fields.promotedConstructor(scope))
            } else {
                add(fields.joinToString("") { it.property(scope) })
                constructors.forEach { add(it.emit(scope)) }
            }
            elements.filterIsInstance<AstFunction>().forEach { add(it.emit(scope, isInterface = false, isStatic = it.isStatic).indentCode(1)) }
            elements.filterIsInstance<RawElement>().forEach { add("${it.code}\n".indentCode(1)) }
        }
        return "$header\n{\n${members.joinToString("\n")}}\n"
    }

    private fun Field.property(scope: Scope): String = (docBlock(listOfNotNull(type.docIfNeeded(scope)?.let { "@var $it" })) + "public ${type.native(scope)} $${name.php()};\n").indentCode(1)

    private fun List<Field>.promotedConstructor(scope: Scope): String {
        val docs = mapNotNull { field -> field.type.docIfNeeded(scope)?.let { "@param $it $${field.name.php()}" } }
        val params = joinToString("") { "public ${it.type.native(scope)} $${it.name.php()},\n".indentCode(1) }
        return (docBlock(docs) + "public function __construct(\n$params) {\n}\n").indentCode(1)
    }

    private fun Constructor.emit(scope: Scope): String {
        val docs = parameters.mapNotNull { it.paramDoc(scope) }
        val body = body.joinToString("") { statement ->
            when (statement) {
                is Assignment -> "\$this->${statement.name.php()} = ${statement.value.emit(scope)};\n".indentCode(1)
                else -> statement.emit(1, scope)
            }
        }
        return (docBlock(docs) + signature("public function __construct", parameters.map { it.emit(scope) }, "").withBody(body)).indentCode(1)
    }

    // PSR-12: the brace of a one-line signature goes on its own line, that of a wrapped signature after the closing parenthesis.
    private fun String.withBody(body: String): String = "$this${if ('\n' in this) " " else "\n"}{\n$body}\n"

    private fun signature(prefix: String, params: List<String>, suffix: String): String = "$prefix(${params.joinToString(", ")})$suffix".takeIf { it.length <= MAX_LINE }
        ?: "$prefix(\n${params.joinToString("") { "$it,\n".indentCode(1) }})$suffix"

    private fun Parameter.emit(scope: Scope): String = "${type.native(scope)} $${name.php()}"

    private fun Parameter.paramDoc(scope: Scope): String? = type.docIfNeeded(scope)?.let { "@param $it $${name.php()}" }

    private fun AstFunction.emit(outer: Scope, isInterface: Boolean, isStatic: Boolean): String {
        val scope = outer.withTypeParameters(typeParameters)
        val returns = returnType?.let { if (it == Type.Unit) "void" else it.native(scope) }
        val docs = typeParameters.templates(scope) +
            parameters.mapNotNull { it.paramDoc(scope) } +
            listOfNotNull(returnType?.takeIf { it != Type.Unit }?.docIfNeeded(scope)?.let { "@return $it" })
        val modifiers = "public ${if (isStatic) "static " else ""}function ${functionName()}"
        val head = signature(modifiers, parameters.map { it.emit(scope) }, returns?.let { ": $it" }.orEmpty())
        return docBlock(docs) + if (isInterface) "$head;\n" else head.withBody(body.joinToString("") { it.emit(1, scope) })
    }

    private fun AstFunction.functionName(): String = name.camelCase()

    private fun Interface.emit(outer: Scope): String {
        val scope = elements.nestedScope(name.pascalCase(), outer).withTypeParameters(typeParameters)
        val header = docBlock(typeParameters.templates(scope) + extends.genericDocs("@extends", scope)) +
            "interface ${name.pascalCase().className()}" + extends.joinNonEmpty(", ", " extends ") { it.native(scope) }
        val methods = elements.filterIsInstance<AstFunction>()
            .joinToString("\n") { it.emit(scope, isInterface = it.body.isEmpty(), isStatic = it.isStatic).indentCode(1) }
        return "$header\n{\n$methods}\n"
    }

    private fun Union.emit(outer: Scope): String {
        val scope = outer.withTypeParameters(typeParameters)
        val parents = listOfNotNull(extends)
        return docBlock(typeParameters.templates(scope) + parents.genericDocs("@extends", scope)) +
            "interface ${name.pascalCase().className()}" + parents.joinNonEmpty(", ", " extends ") { it.native(scope) } + "\n{\n}\n"
    }

    private fun Enum.emit(scope: Scope): String {
        val cases = entries.joinToString("") { entry ->
            val label = entry.values.firstOrNull()?.removeSurrounding("\"") ?: entry.name.value()
            "case ${entry.name.value()} = ${label.phpString()};\n".indentCode(1)
        }
        val methods = elements.filterIsInstance<AstFunction>()
            .joinToString("") { "\n" + it.emit(scope, isInterface = false, isStatic = it.isStatic).indentCode(1) }
        return "enum ${name.pascalCase().className()}: string${extends?.let { " implements ${it.native(scope)}" }.orEmpty()}\n{\n$cases$methods}\n"
    }

    /** A namespace holding functions becomes a class of static functions; one with only a parent becomes a marker interface. */
    private fun Namespace.emit(outer: Scope): String? {
        val scope = elements.nestedScope(name.pascalCase(), outer)
        val functions = elements.filterIsInstance<AstFunction>()
        val className = name.pascalCase().className()
        return when {
            functions.isNotEmpty() -> "final class $className${extends?.let { " implements ${it.native(scope)}" }.orEmpty()}\n{\n" +
                functions.joinToString("\n") { it.emit(scope, isInterface = false, isStatic = true).indentCode(1) } + "}\n"
            extends != null -> "interface $className extends ${extends.native(scope)}\n{\n}\n"
            else -> null
        }
    }

    private fun Main.emit(scope: Scope): String = statics.joinToString("") { element -> element.declarations("", scope).joinToString("") { it.code } } +
        body.joinToString("") { it.emit(0, scope) }

    // Types

    private fun Type.isTypeParameter(scope: Scope): Boolean = this is Type.Custom && generics.isEmpty() && name.referenceName() in scope.typeParameters

    private fun Type.native(scope: Scope): String = when (this) {
        is Type.Integer, is Type.IntegerLiteral -> "int"
        is Type.Number -> "float"
        Type.String, Type.Bytes, Type.Reflect, is Type.StringLiteral -> "string"
        Type.Boolean -> "bool"
        Type.Any, Type.Wildcard -> "mixed"
        Type.Unit -> "null"
        is Type.Array, is Type.Dict -> "array"
        is Type.Function -> "\\Closure"
        is Type.Custom -> if (isTypeParameter(scope)) "mixed" else name.className(scope)
        is Type.Nullable -> type.native(scope).let { if (it == "mixed" || it == "null") it else "?$it" }
    }

    private fun Type.doc(scope: Scope): String = when (this) {
        is Type.Array -> "list<${elementType.doc(scope)}>"
        is Type.Dict -> "array<${keyType.doc(scope)}, ${valueType.doc(scope)}>"
        is Type.Function -> "\\Closure(${parameterTypes.joinToString(", ") { it.doc(scope) }}): ${returnType.doc(scope)}"
        is Type.Custom -> when {
            isTypeParameter(scope) -> name.referenceName()
            generics.isEmpty() || generics.all { it == Type.Wildcard } -> name.className(scope)
            else -> "${name.className(scope)}<${generics.joinToString(", ") { it.doc(scope) }}>"
        }
        is Type.Nullable -> type.doc(scope).let { inner ->
            when {
                inner == "mixed" || inner == "null" -> inner
                inner == type.native(scope) -> "?$inner"
                else -> "$inner|null"
            }
        }
        else -> native(scope)
    }

    private fun Type.docIfNeeded(scope: Scope): String? = doc(scope).takeIf { it != native(scope) }

    private fun Name.className(scope: Scope = Scope()): String = referenceName().let { scope.siblings[it] ?: it }.className()

    private fun String.className(): String = split('.', '\\').joinToString("\\") { if (it.lowercase() in reservedClassNames) "${it}_" else it }

    /** The PHPDoc type string of a [TypeDescriptor], with `::class` for every class so `use` imports resolve it. */
    private fun Type.descriptor(scope: Scope): String {
        val parts = descriptorParts(scope).fold(emptyList<DescriptorPart>()) { acc, part ->
            val last = acc.lastOrNull()
            if (last is DescriptorPart.Text && part is DescriptorPart.Text) acc.dropLast(1) + DescriptorPart.Text(last.value + part.value) else acc + part
        }
        return parts.joinToString(" . ") {
            when (it) {
                is DescriptorPart.Text -> it.value.phpString()
                is DescriptorPart.Class -> "${it.name}::class"
            }
        }
    }

    private sealed interface DescriptorPart {
        data class Text(val value: String) : DescriptorPart
        data class Class(val name: String) : DescriptorPart
    }

    private fun Type.descriptorParts(scope: Scope): List<DescriptorPart> = when (this) {
        is Type.Custom -> if (isTypeParameter(scope)) listOf(DescriptorPart.Text("mixed")) else listOf(DescriptorPart.Class(name.className(scope)))
        is Type.Array -> listOf(DescriptorPart.Text("list<")) + elementType.descriptorParts(scope) + DescriptorPart.Text(">")
        is Type.Dict -> listOf(DescriptorPart.Text("array<string, ")) + valueType.descriptorParts(scope) + DescriptorPart.Text(">")
        is Type.Nullable -> listOf(DescriptorPart.Text("?")) + type.descriptorParts(scope)
        else -> listOf(DescriptorPart.Text(native(scope)))
    }

    // Statements

    private fun Statement.emit(indent: Int, scope: Scope): String = when (this) {
        is ReturnStatement -> "return ${expression.emit(scope)};\n"
        is Assignment -> "${if (isProperty) "\$this->" else "$"}${name.php()} = ${value.emit(scope)};\n"
        is ErrorStatement -> "throw ${exception(message, scope)};\n"
        is AssertStatement -> "assert(${expression.emit(scope)}, ${message.phpString()});\n"
        is PrintStatement -> "echo ${expression.emit(scope)}, \\PHP_EOL;\n"
        is Switch -> emit(scope)
        is RawExpression -> code.trimEnd().let { if (it.endsWith(";") || it.endsWith("}")) "$it\n" else "$it;\n" }
        is ConstructorStatement, is NullLiteral, is NullableEmpty, is VariableReference, is FieldCall, is FunctionCall,
        is ArrayIndexCall, is EnumReference, is EnumValueCall, is BinaryOp, is TypeDescriptor, is Cast,
        is Literal, is LiteralList, is LiteralMap, is NullCheck, is NullableMap, is NullableOf,
        is NullableGet, is Constraint, is NotExpression, is IfExpression, is MapExpression, is FlatMapIndexed,
        is FlatMapEntries, is ListConcat, is StringTemplate, is Lambda,
        -> "${emit(scope)};\n"
    }.indentCode(indent)

    private fun exception(message: Expression, scope: Scope): String = "new \\Exception(${message.emit(scope)})"

    private fun Switch.emit(scope: Scope): String = when {
        isMatch() -> emitMatch(scope)
        cases.any { it.type != null } -> emitInstanceOfChain(scope)
        else -> emitSwitch(scope)
    }

    // A switch whose every branch returns or throws is a `return match (...)`.
    private fun Switch.isMatch(): Boolean = default?.singleOrNull()?.let { it is ReturnStatement || it is ErrorStatement } == true &&
        cases.all { it.body.singleOrNull() is ReturnStatement }

    private fun Switch.emitMatch(scope: Scope): String {
        val matchesType = cases.any { it.type != null }
        val subject = expression.emit(scope)
        val arms = cases.map { case ->
            val condition = if (matchesType) "$subject instanceof ${case.type?.native(scope)}" else case.value.emit(scope)
            "$condition => ${case.returned(this, scope)},\n"
        } + "default => ${default.orEmpty().single().let { if (it is ReturnStatement) it.expression.emit(scope) else "throw ${exception((it as ErrorStatement).message, scope)}" }},\n"
        return "return match (${if (matchesType) "true" else subject}) {\n${arms.joinToString("") { it.indentCode(1) }}};\n"
    }

    // A match arm cannot bind the matched value to a variable, so the arm refers to the subject itself.
    private fun Case.returned(switch: Switch, scope: Scope): String = (body.single() as ReturnStatement).expression
        .let { returned -> switch.variable?.let { returned.substitute(it, switch.expression) } ?: returned }
        .emit(scope)

    private fun Switch.emitInstanceOfChain(scope: Scope): String {
        val subject = expression.emit(scope)
        val branches = cases.joinToString(" else") { case ->
            val binding = variable?.let { "$${it.php()} = $subject;\n".indentCode(1) }.orEmpty()
            " if ($subject instanceof ${case.type?.native(scope)}) {\n$binding${case.body.joinToString("") { it.emit(1, scope) }}}"
        }.trimStart()
        val otherwise = default?.let { " else {\n${it.joinToString("") { statement -> statement.emit(1, scope) }}}" }.orEmpty()
        return "$branches$otherwise\n"
    }

    private fun Switch.emitSwitch(scope: Scope): String {
        val branches = cases.joinToString("") { case ->
            "case ${case.value.emit(scope)}:\n".indentCode(1) + case.body.emitBranch(scope)
        } + default?.let { "default:\n".indentCode(1) + it.emitBranch(scope) }.orEmpty()
        return "switch (${expression.emit(scope)}) {\n$branches}\n"
    }

    private fun List<Statement>.emitBranch(scope: Scope): String = joinToString("") { it.emit(2, scope) } + if (lastOrNull() is ReturnStatement || lastOrNull() is ErrorStatement) "" else "break;\n".indentCode(2)

    // Expressions

    private fun Expression.emit(scope: Scope): String = when (this) {
        is ConstructorStatement -> if (type == Type.Unit) "null" else "new ${type.native(scope)}(${namedArguments.entries.joinToString(", ") { (name, value) -> "${name.php()}: ${value.emit(scope)}" }})"
        is Literal -> emit()
        is LiteralList -> "[${values.joinToString(", ") { it.emit(scope) }}]"
        is LiteralMap -> "[${values.entries.joinToString(", ") { (key, value) -> "${key.phpString()} => ${value.emit(scope)}" }}]"
        is ClassReference -> "${type.native(scope)}::class"
        is RawExpression -> code
        is NullLiteral, is NullableEmpty -> "null"
        is VariableReference -> "$${name.php()}"
        is FieldCall -> "${receiver?.emit(scope) ?: "\$this"}->${field.php()}"
        is FunctionCall -> emit(scope)
        is ArrayIndexCall -> when {
            index is Literal && index.type is Type.Integer -> "${receiver.emit(scope)}[${index.emit(scope)}]"
            caseSensitive -> "(${receiver.emit(scope)}[${index.emit(scope)}] ?? null)"
            index is Literal -> "(\\array_change_key_case(${receiver.emit(scope)})[${index.value.toString().lowercase().phpString()}] ?? null)"
            else -> "(\\array_change_key_case(${receiver.emit(scope)})[\\strtolower(${index.emit(scope)})] ?? null)"
        }
        is EnumReference -> "${enumType.native(scope)}::${entry.value()}"
        is EnumValueCall -> "${expression.emit(scope)}->value"
        is BinaryOp -> "(${left.emit(scope)} ${operator.toPhp(isConcatenation())} ${right.emit(scope)})"
        is TypeDescriptor -> type.descriptor(scope)
        is Cast -> expression.emit(scope)
        is NullCheck -> expression.emit(scope).let { subject ->
            "($subject !== null ? ${body.substitute(Name.of("it"), RawExpression(subject)).emit(scope)} : ${alternative?.emit(scope) ?: "null"})"
        }
        is NullableMap -> expression.emit(scope).let { subject ->
            "($subject !== null ? ${body.substitute(variable, RawExpression(subject)).emit(scope)} : ${alternative.emit(scope)})"
        }
        is NullableOf -> expression.emit(scope)
        is NullableGet -> expression.emit(scope)
        is Constraint.RegexMatch -> "(\\preg_match(${phpRegex(pattern, rawValue).phpString()}, ${value.emit(scope)}) === 1)"
        is Constraint.BoundCheck -> listOfNotNull(
            min?.let { "$it <= ${value.emit(scope)}" },
            max?.let { "${value.emit(scope)} <= $it" },
        ).joinToString(" && ").ifEmpty { "true" }.let { "($it)" }
        is ErrorStatement -> "throw ${exception(message, scope)}"
        is NotExpression -> "!${expression.emit(scope)}"
        is IfExpression -> "(${condition.emit(scope)} ? ${thenExpr.emit(scope)} : ${elseExpr.emit(scope)})"
        is MapExpression -> when {
            receiver is BinaryOp && receiver.operator == BinaryOp.Operator.UNTIL ->
                "\\array_map(fn (\$${variable.php()}) => ${body.emit(scope)}, \\range(${receiver.left.emit(scope)}, ${receiver.right.emit(scope)} - 1))"
            else -> "\\array_map(fn (\$${variable.php()}) => ${body.emit(scope)}, ${receiver.emit(scope)})"
        }
        is FlatMapIndexed -> receiver.emit(scope).let { list ->
            "\\array_merge(...\\array_map(fn (\$${indexVar.php()}, \$${elementVar.php()}) => ${body.emit(scope)}, \\array_keys($list), $list))"
        }
        is FlatMapEntries -> receiver.emit(scope).let { dict ->
            "\\array_merge(...\\array_map(fn (\$${keyVar.php()}, \$${valueVar.php()}) => ${body.emit(scope)}, \\array_keys($dict), \\array_values($dict)))"
        }
        is ListConcat -> when {
            lists.isEmpty() -> "[]"
            lists.size == 1 -> lists.single().emit(scope)
            else -> "[${lists.joinToString(", ") { "...${it.emit(scope)}" }}]"
        }
        is StringTemplate -> parts.joinToString(" . ") {
            when (it) {
                is StringTemplate.Part.Text -> it.value.phpString()
                is StringTemplate.Part.Expr -> it.expression.emit(scope)
            }
        }.ifEmpty { "''" }.let { if (parts.size > 1) "($it)" else it }
        is Lambda -> "fn (${parameters.joinToString(", ") { param -> param.type.native(scope).let { if (it == "mixed") "$${param.name.php()}" else "$it $${param.name.php()}" } }}) => ${body.emit(scope)}"
        is AssertStatement, is Switch, is Assignment, is PrintStatement, is ReturnStatement ->
            throw IllegalArgumentException("${this::class.simpleName} cannot be an expression in PHP")
    }

    private fun FunctionCall.emit(scope: Scope): String {
        val arguments = arguments.values.joinToString(", ") { it.emit(scope) }
        val qualified = name.value()
        return when {
            receiver == null && '.' in qualified ->
                "${qualified.substringBeforeLast('.').let { Name(listOf(it)).className(scope) }}::${qualified.substringAfterLast('.')}($arguments)"
            receiver == null -> "${name.camelCase()}($arguments)"
            receiver is RawExpression && receiver.code.isClassName() -> "${receiver.code.className()}::${name.camelCase()}($arguments)"
            receiver is ConstructorStatement -> "(${receiver.emit(scope)})->${name.camelCase()}($arguments)"
            else -> "${receiver.emit(scope)}->${name.camelCase()}($arguments)"
        }
    }

    private fun String.isClassName(): Boolean = isNotEmpty() && first().isUpperCase() && all { it.isLetterOrDigit() || it == '_' || it == '\\' || it == '.' }

    private fun BinaryOp.isConcatenation(): Boolean = operator == BinaryOp.Operator.PLUS && listOf(left, right).any { it.isString() }

    private fun Expression.isString(): Boolean = (this is Literal && type == Type.String) ||
        this is StringTemplate ||
        (this is BinaryOp && isConcatenation())

    private fun BinaryOp.Operator.toPhp(concatenation: Boolean): String = when (this) {
        BinaryOp.Operator.PLUS -> if (concatenation) "." else "+"
        BinaryOp.Operator.EQUALS -> "==="
        BinaryOp.Operator.NOT_EQUALS -> "!=="
        BinaryOp.Operator.UNTIL -> throw IllegalArgumentException("UNTIL operator is only supported as the receiver of a map in PHP")
    }

    private fun Literal.emit(): String = when (type) {
        Type.String -> value.toString().phpString()
        Type.Boolean -> value.toString()
        is Type.Number -> value.toString().let { if (it.any { c -> c == '.' || c == 'e' || c == 'E' }) it else "$it.0" }
        else -> value.toString()
    }

    // Names

    /** A field, parameter or variable: a multi-word wire name is camel cased, a single word keeps its spelling. */
    private fun Name.php(): String = (if (parts.size > 1) camelCase() else value())
        .filter { it.isLetterOrDigit() || it == '_' }
        .ifEmpty { "_" }
        .let { if (it.first().isDigit() || it == "this") "_$it" else it }

    private fun String.phpString(): String = if (any { it.code < 0x20 }) {
        "\"${escapeStringLiteral('"', '$')}\""
    } else {
        "'${replace("\\", "\\\\").replace("'", "\\'")}'"
    }

    // A PCRE pattern needs delimiters that do not occur in it; the Wirespec flags PCRE does not know (such as `g`) are dropped.
    private fun phpRegex(pattern: String, rawValue: String): String {
        val flags = rawValue.substringAfterLast('/', "").filter { it in "imsxuU" }
        val delimiter = listOf('/', '#', '~', '%', '!', '@').firstOrNull { it !in pattern } ?: '\u0001'
        return "$delimiter$pattern$delimiter$flags"
    }
}
