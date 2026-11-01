package community.flock.wirespec.compiler.core.ir.generator

import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.ir.ArrayIndexCall
import community.flock.wirespec.compiler.core.ir.AssertStatement
import community.flock.wirespec.compiler.core.ir.Assignment
import community.flock.wirespec.compiler.core.ir.BinaryOp
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
import community.flock.wirespec.compiler.core.ir.Precision
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
import community.flock.wirespec.compiler.core.ir.Function as AstFunction

/**
 * Every element ends its output with a single newline: [compactSections] drops blank lines anyway and
 * only separates the imports from the rest of the file.
 */
public object RustGenerator :
    Generator,
    Keywords {
    override val reservedKeywords: Set<String> = setOf(
        "as", "break", "const", "continue", "crate",
        "else", "enum", "extern", "false", "fn",
        "for", "if", "impl", "in", "let",
        "loop", "match", "mod", "move", "mut",
        "pub", "ref", "return", "self", "Self",
        "static", "struct", "super", "trait", "true",
        "type", "unsafe", "use", "where", "while",
        "async", "await", "dyn", "abstract", "become",
        "box", "do", "final", "macro", "override",
        "priv", "typeof", "unsized", "virtual", "yield",
        "try",
    )

    override fun generate(element: Element): String = when (element) {
        is File -> element
        else -> File(Name.of(""), listOf(element))
    }.emit()

    private fun String.indentCode(level: Int): String = indentLines(level, width = 4)

    private fun File.emit(): String = elements.splitAfterLastImport().let { (header, body) ->
        compactSections(header.emit(0), body.emit(0))
    }

    private fun List<Element>.emit(indent: Int): String = joinToString("") { it.emit(indent) }

    private fun List<Statement>.emitBody(): String = joinToString("") { it.emit(1) }

    private fun Element.emit(indent: Int): String = when (this) {
        is Package -> "// package $path\n".indentCode(indent)
        is Import -> "use ${path.wrapIfNotEmpty("", "::")}${type.name.value()};\n".indentCode(indent)
        is Struct -> emit(indent)
        is AstFunction -> emit(indent)
        is Namespace -> emit(indent)
        is Interface -> emit(indent)
        is Union -> emit(indent)
        is Enum -> emit(indent)
        is Main -> emit(indent)
        is File -> elements.emit(indent)
        // A field has no standalone rendering; it is emitted inline within its enclosing Struct parameter list via Struct.emit.
        is Field -> ""
        is RawElement -> "$code\n".indentCode(indent)
    }

    private fun Main.emit(indent: Int): String {
        val content = body.emitBody().let {
            if (isAsync) "pollster::block_on(async {\n".indentCode(1) + it + "});\n".indentCode(1) else it
        }
        return statics.emit(indent) + "fn main() {\n$content}\n".indentCode(indent)
    }

    private fun Namespace.emit(indent: Int): String {
        val rustName = name.pascalCase()
        val content = elements.emit(1)
        return when {
            content.isBlank() -> "pub struct $rustName;\n"
            elements.any { it is Interface || it is Union || it is Enum || it is Struct } -> "pub mod $rustName {\n${"use super::*;\n".indentCode(1)}$content}\n"
            else -> "pub struct $rustName;\nimpl $rustName {\n$content}\n"
        }.indentCode(indent)
    }

    private fun Interface.emit(indent: Int): String {
        val signature = "pub trait ${name.pascalCase()}${typeParameters.emit()}${extends.joinNonEmpty(" + ", ": ") { it.emit() }}"
        val content = fields.joinToString("") { "fn ${it.name.snakeCase().sanitize()}(&self) -> ${it.type.emit()};\n".indentCode(1) } + elements.emit(1)
        return (if (content.isEmpty()) "$signature {}\n" else "$signature {\n$content}\n").indentCode(indent)
    }

    private fun Union.emit(indent: Int): String {
        val variants = members.joinToString("") { "${it.name.pascalCase()}(${it.name.pascalCase()}),\n".indentCode(1) }
        val definition = "pub enum ${name.pascalCase()}${typeParameters.emit()}"
        return "#[derive(Debug, Clone, PartialEq)]\n${if (variants.isEmpty()) "$definition {}" else "$definition {\n$variants}"}\n".indentCode(indent)
    }

    private fun Enum.Entry.wireValue(): String = values.firstOrNull()?.removeSurrounding("\"") ?: name.value()

    private fun Enum.emit(indent: Int): String {
        val rustName = name.pascalCase()
        val definition = "#[derive(Debug, Clone, PartialEq)]\npub enum $rustName {\n${entries.joinToString("") { "${it.name.value()},\n".indentCode(1) }}}\n"
        val labelArms = entries.joinToString("\n") { "$rustName::${it.name.value()} => \"${it.wireValue()}\",".indentCode(3) }
        val fromLabelArms = entries.joinToString("\n") { "\"${it.wireValue()}\" => Some($rustName::${it.name.value()}),".indentCode(3) }
        val implementation = if (entries.isEmpty()) {
            ""
        } else {
            """
            |impl Enum for $rustName {
            |    fn label(&self) -> &str {
            |        match self {
            |$labelArms
            |        }
            |    }
            |    fn from_label(s: &str) -> Option<Self> {
            |        match s {
            |$fromLabelArms
            |            _ => None,
            |        }
            |    }
            |}
            |impl std::fmt::Display for $rustName {
            |    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
            |        write!(f, "{}", self.label())
            |    }
            |}
            |
            """.trimMargin()
        }
        return (definition + implementation).indentCode(indent)
    }

    private fun Struct.emit(indent: Int): String {
        val rustName = name.pascalCase()
        val fields = fieldList()
        val (functions, nested) = elements.partition { it is AstFunction }
        val body = if (fields.isEmpty() && constructors.isEmpty()) {
            ";\n"
        } else {
            " {\n${fields.joinToString("") { "pub ${it.name.snakeCase().sanitize()}: ${it.type.emit()},\n".indentCode(1) }}}\n"
        }
        val definition = "pub struct $rustName${typeParameters.joinNonEmpty(", ", "<", ">") { it.type.emit() }}$body"
        val implementations = constructors.joinToString("") { it.emit(rustName, fields) } +
            functions.joinNonEmpty("", "impl $rustName {\n", "}\n") { it.emit(1) }
        return (definition + implementations).indentCode(indent) + nested.emit(indent)
    }

    private fun Constructor.emit(structName: String, fields: List<Field>): String {
        val params = parameters.joinToString(", ") { "${it.name.snakeCase().sanitize()}: ${it.type.emit()}" }
        val values = body.filterIsInstance<Assignment>().associate { it.name.value() to it.value.emit() }
        val initializers = fields.joinToString(",\n") { "${it.name.snakeCase().sanitize()}: ${values[it.name.value()] ?: "Default::default()"}" }
        val construction = "$structName {\n${initializers.indentCode(1)}\n}"
        val function = "pub fn new($params) -> Self {\n${construction.indentCode(1)}\n}"
        return "impl $structName {\n${function.indentCode(1)}\n}\n"
    }

    private fun AstFunction.emit(indent: Int): String {
        val params = parameters.joinToString(", ") {
            if (it.name.value() in selfReceivers) it.name.value() else "${it.name.snakeCase().sanitize()}: ${it.type.emit()}"
        }
        val returns = returnType?.takeIf { it != Type.Unit }?.let { " -> ${it.emit()}" }.orEmpty()
        // Only trait methods are declared without a body, and Rust allows no visibility modifier on them.
        val modifiers = (if (body.isEmpty()) "" else "pub ") + (if (isAsync) "async " else "")
        val signature = "${modifiers}fn ${name.snakeCase().sanitize()}${typeParameters.emit()}($params)$returns"
        return (if (body.isEmpty()) "$signature;\n" else "$signature {\n${body.emitBody()}}\n").indentCode(indent)
    }

    private fun List<TypeParameter>.emit(): String = joinNonEmpty(", ", "<", ">") { it.emit() }

    // `T : Any?` is unbounded: a Rust bound must be a trait, which `Option<Box<dyn std::any::Any>>` is not (E0404).
    private fun TypeParameter.emit(): String = type.emit() + extends.filterNot { it == Type.Nullable(Type.Any) }.joinNonEmpty(" + ", ": ") { it.emit() }

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
        Type.Reflect -> "std::any::TypeId"
        is Type.Array -> "Vec<${elementType.emit()}>"
        is Type.Dict -> "std::collections::HashMap<${keyType.emit()}, ${valueType.emit()}>"
        is Type.Custom -> when {
            generics.isEmpty() -> name.referenceName()
            Type.Wildcard in generics -> "Box<dyn std::any::Any>"
            else -> "${name.referenceName()}<${generics.joinToString(", ") { it.emit() }}>"
        }
        is Type.Nullable -> "Option<${type.emit()}>"
        is Type.IntegerLiteral -> "i32"
        is Type.StringLiteral -> "String"
        is Type.Function -> "Box<dyn Fn(${parameterTypes.joinToString(", ") { it.emit() }}) -> ${returnType.emit()}>"
    }

    private fun Statement.emit(indent: Int): String = when (this) {
        is PrintStatement -> "println!(\"{}\", ${expression.emit()});\n"
        is ReturnStatement -> "return ${expression.emit()};\n"
        is ConstructorStatement -> when {
            type == Type.Unit -> "()\n"
            namedArguments.isEmpty() -> "${type.emit()} {}\n"
            else -> "${type.emit()} {\n${namedArguments.emit(",\n").indentCode(1)},\n}\n"
        }
        is Assignment -> "let ${name.snakeCase().sanitize()} = ${value.emit()};\n"
        is AssertStatement -> "assert!(${expression.emit()}, \"$message\");\n"
        is Switch -> emitMatch()
        is RawExpression, is NullLiteral, is NullableEmpty, is VariableReference, is FieldCall, is FunctionCall,
        is ArrayIndexCall, is EnumReference, is EnumValueCall, is BinaryOp, is TypeDescriptor, is Cast,
        is Literal, is LiteralList, is LiteralMap, is ErrorStatement, is NullCheck, is NullableMap, is NullableOf,
        is NullableGet, is Constraint, is NotExpression, is IfExpression, is MapExpression, is FlatMapIndexed,
        is FlatMapEntries, is ListConcat, is StringTemplate, is Lambda,
        -> "${emit()};\n"
    }.indentCode(indent)

    private fun Switch.emitMatch(): String {
        val matchesType = cases.any { it.type != null }
        val arms = cases.map { case ->
            (if (matchesType) "${case.type?.emit() ?: "_"}(${variable?.snakeCase() ?: "_"})" else case.value.emit()) to case.body
        } + listOfNotNull(default?.let { "_" to it })
        return "match ${expression.emit()} {\n${arms.joinToString("") { (pattern, body) -> "$pattern => {\n${body.emitBody()}}\n" }.indentCode(1)}}\n"
    }

    private fun Map<Name, Expression>.emit(separator: String): String = entries.joinToString(separator) { (name, value) -> "${name.snakeCase().sanitize()}: ${value.emit()}" }

    private fun BinaryOp.Operator.toRust(): String = when (this) {
        BinaryOp.Operator.PLUS -> "+"
        BinaryOp.Operator.EQUALS -> "=="
        BinaryOp.Operator.NOT_EQUALS -> "!="
        BinaryOp.Operator.UNTIL -> throw IllegalArgumentException("UNTIL operator is not supported in Rust")
    }

    private fun Expression.emit(): String = when (this) {
        is ConstructorStatement -> when {
            type == Type.Unit -> "()"
            namedArguments.isEmpty() -> "${type.emit()} {}"
            else -> "${type.emit()} { ${namedArguments.emit(", ")} }"
        }
        is Literal -> emit()
        is LiteralList -> if (values.isEmpty()) "Vec::<${type.emit()}>::new()" else "vec![${values.joinToString(", ") { it.emit() }}]"
        is LiteralMap -> if (values.isEmpty()) {
            "std::collections::HashMap::new()"
        } else {
            "std::collections::HashMap::from([${values.entries.joinToString(", ") { (key, value) -> "(${Literal(key, keyType).emit()}, ${value.emit()})" }}])"
        }
        is ClassReference -> "std::any::TypeId::of::<${type.emit()}>()"
        is RawExpression -> code
        is NullLiteral, is NullableEmpty -> "None"
        is VariableReference -> name.snakeCase().sanitize()
        is FieldCall -> "${receiver?.let { "${it.emit()}." }.orEmpty()}${field.snakeCase().sanitize()}"
        is FunctionCall -> {
            val function = if ("::" in name.value()) name.value() else name.snakeCase().sanitize()
            "${receiver?.let { "${it.emit()}." }.orEmpty()}$function(${arguments.values.joinToString(", ") { it.emit() }})${if (isAwait) ".await" else ""}"
        }
        is ArrayIndexCall -> when {
            index is Literal && index.type is Type.String -> if (caseSensitive) {
                "${receiver.emit()}.get(\"${index.value}\")"
            } else {
                "${receiver.emit()}.iter().find(|(k, _)| k.eq_ignore_ascii_case(\"${index.value}\")).map(|(_, v)| v.clone())"
            }
            index is Literal && (index.type is Type.Integer || index.type is Type.Number) -> "${receiver.emit()}[${index.value}]"
            else -> "${receiver.emit()}[${index.emit()}]"
        }
        is EnumReference -> "${enumType.emit()}::${entry.value()}"
        is EnumValueCall -> "format!(\"{:?}\", ${expression.emit()})"
        is BinaryOp -> "(${left.emit()} ${operator.toRust()} ${right.emit()})"
        is TypeDescriptor -> "std::any::TypeId::of::<${type.emit()}>()"
        is Cast -> "(${expression.emit()} as ${targetType.emit()})"
        is NullCheck -> "${expression.emit()}.as_ref().map(|it| ${body.emit()})${alternative.emitUnwrap()}"
        is NullableMap -> "${expression.emit()}.as_ref().map(|${variable.snakeCase()}| ${body.emit()})${alternative.emitUnwrap()}"
        is NullableOf -> "Some(${expression.emit()})"
        is NullableGet -> "${expression.emit()}.unwrap()"
        is Constraint.RegexMatch -> "regex::Regex::new(r\"$pattern\").unwrap().is_match(&${value.emit()})"
        is Constraint.BoundCheck -> listOfNotNull(
            min?.let { "$it <= ${value.emit()}" },
            max?.let { "${value.emit()} <= $it" },
        ).joinToString(" && ").ifEmpty { "true" }
        is ErrorStatement -> "panic!(${message.emitFormatArguments()})"
        is AssertStatement -> throw IllegalArgumentException("AssertStatement cannot be an expression in Rust")
        is Switch -> throw IllegalArgumentException("Switch cannot be an expression in Rust")
        is Assignment -> throw IllegalArgumentException("Assignment cannot be an expression in Rust")
        is PrintStatement -> throw IllegalArgumentException("PrintStatement cannot be an expression in Rust")
        is ReturnStatement -> throw IllegalArgumentException("ReturnStatement cannot be an expression in Rust")
        is NotExpression -> "!${expression.emit()}"
        is IfExpression -> "if ${condition.emit()} { ${thenExpr.emit()} } else { ${elseExpr.emit()} }"
        is MapExpression -> if (receiver is BinaryOp && receiver.operator == BinaryOp.Operator.UNTIL) {
            receiver.right.emit()
        } else {
            "${receiver.emit()}.iter().map(|${variable.snakeCase()}| ${body.emit()}).collect::<Vec<_>>()"
        }
        is FlatMapIndexed -> "${receiver.emit()}.iter().enumerate().flat_map(|(${indexVar.snakeCase()}, ${elementVar.snakeCase()})| ${body.emit()}).collect::<Vec<_>>()"
        is FlatMapEntries -> "${receiver.emit()}.iter().flat_map(|(${keyVar.snakeCase()}, ${valueVar.snakeCase()})| ${body.emit()}).collect::<Vec<_>>()"
        is ListConcat -> when {
            lists.isEmpty() -> "vec![]"
            lists.size == 1 -> lists.single().emit()
            else -> "vec![${lists.joinToString(", ") { "${it.emit()}.as_slice()" }}].concat()"
        }
        is StringTemplate -> {
            val template = parts.joinToString("") { part ->
                when (part) {
                    is StringTemplate.Part.Text -> part.value
                    is StringTemplate.Part.Expr -> "{}"
                }
            }
            val arguments = parts.filterIsInstance<StringTemplate.Part.Expr>().map { it.expression.emit() }
            if (arguments.isEmpty()) "String::from(\"$template\")" else "format!(\"$template\", ${arguments.joinToString(", ")})"
        }
        is Lambda -> "Box::new(|${parameters.joinToString(", ") { it.name.snakeCase().sanitize() }}| ${body.emit()})"
    }

    private fun Expression?.emitUnwrap(): String = when (this) {
        null -> ""
        is ErrorStatement -> message.stringLiteral()?.let { ".expect(\"$it\")" } ?: ".unwrap_or_else(|| ${emit()})"
        else -> ".unwrap_or(${emit()})"
    }

    // The format string of `panic!`: string literals concatenated into the message are inlined, anything else becomes a `{}` argument.
    private fun Expression.emitFormatArguments(): String = concatenation().let { parts ->
        val template = parts.joinToString("") { it.stringLiteral() ?: "{}" }
        (listOf("\"$template\"") + parts.filter { it.stringLiteral() == null }.map { it.emit() }).joinToString(", ")
    }

    private fun Expression.concatenation(): List<Expression> = if (this is BinaryOp && operator == BinaryOp.Operator.PLUS) {
        left.concatenation() + right.concatenation()
    } else {
        listOf(this)
    }

    private fun Expression.stringLiteral(): String? = (this as? Literal)?.takeIf { it.type is Type.String }?.value?.toString()

    private fun Literal.emit(): String = when (type) {
        is Type.String -> "String::from(\"${value.toString().escapeStringLiteral('"')}\")"
        is Type.Integer, is Type.Number -> "${value}_${type.emit()}"
        else -> value.toString()
    }
}

private val selfReceivers = setOf("self", "&self")

// `self` and `Self` cannot be escaped as raw identifiers (r#self is invalid Rust); the emitter's
// sanitization renames such fields instead, so the generator leaves them untouched.
private val escapableKeywords = RustGenerator.reservedKeywords - setOf("self", "Self")

private fun String.sanitize(): String = if (this in escapableKeywords) "r#$this" else this
