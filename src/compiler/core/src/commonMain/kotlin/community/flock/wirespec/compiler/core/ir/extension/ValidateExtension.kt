package community.flock.wirespec.compiler.core.ir.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.FlatMapEntries
import community.flock.wirespec.compiler.core.ir.FlatMapIndexed
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.IfExpression
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.ListConcat
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.MapExpression
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.NotExpression
import community.flock.wirespec.compiler.core.ir.NullableMap
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.StringTemplate
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.converter.convertConstraint
import community.flock.wirespec.compiler.core.ir.converter.toName
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.ir.import
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.parse.ast.Type as Model

/**
 * Gives every model a `validate` function that returns the path of each value breaking its
 * constraint: a refined type or a constrained primitive, at any depth inside models, lists,
 * dicts and nullables, such as `departments[0].employees[2].tags[1]` or `labelsByKey[home].code`.
 *
 * Every [community.flock.wirespec.compiler.core.ir.emit.IrEmitter] applies it before any other
 * extension. The function is a method on the model, which takes `self` in Python and Rust; in
 * TypeScript, where a model is a plain type, it is a `validate<Model>(obj)` function next to it.
 * Unions are not descended into, because a TypeScript union carries no tag to tell its members apart.
 */
public class ValidateExtension(language: FileExtension) : IrExtension {

    private enum class Dialect { Member, Python, Rust, TypeScript }

    private val dialect: Dialect? = when (language) {
        FileExtension.Java, FileExtension.Kotlin, FileExtension.Scala -> Dialect.Member
        FileExtension.Python -> Dialect.Python
        FileExtension.Rust -> Dialect.Rust
        FileExtension.TypeScript -> Dialect.TypeScript
        FileExtension.Wirespec,
        FileExtension.JSON,
        FileExtension.YAML,
        FileExtension.AvroJson,
        FileExtension.AvroIdl,
        FileExtension.GraphQL,
        -> null
    }

    // Definitions are looked up across all modules, so a model can nest a type declared in another file.
    override fun extend(ir: IR, ast: AST): IR = dialect
        ?.let { Validation(it, ast.modules.flatMap { module -> module.statements }.associateBy { it.identifier.value }) }
        ?.let { validation -> ir.map { element -> (element as? File)?.let(validation::validate) ?: element } }
        ?: ir

    private class Validation(private val dialect: Dialect, private val definitions: Map<String, Definition>) {

        private val models = definitions.values.filterIsInstance<Model>().associateBy { it.identifier.toName().pascalCase() }

        fun validate(file: File): File = file.elements
            .map { element -> (element as? Struct)?.let { struct -> modelOf(struct)?.let { struct.withValidate(it) } } ?: element }
            .let { elements -> file.copy(elements = elements.withImports(validatorImports(file))) }

        private fun modelOf(struct: Struct): Model? = models[struct.name.pascalCase()]
            ?.takeIf { struct.interfaces.any { it.name == SHAPE } }

        private fun Struct.withValidate(model: Model): Struct = copy(elements = listOf(validateFunction(model)) + elements)

        private fun Struct.validateFunction(model: Model): Function = Function(
            name = when (dialect) {
                Dialect.TypeScript -> Name.of("validate${model.identifier.value}")
                Dialect.Member, Dialect.Python, Dialect.Rust -> VALIDATE
            },
            parameters = when (dialect) {
                Dialect.Member -> emptyList()
                Dialect.Python -> listOf(Parameter(Name.of("self"), Type.Custom("")))
                Dialect.Rust -> listOf(Parameter(Name.of("&self"), Type.Custom("")))
                Dialect.TypeScript -> listOf(Parameter(OBJ, Type.Custom(model.identifier.value)))
            },
            returnType = Type.Array(Type.String),
            body = listOf(
                ReturnStatement(
                    model.shape.fields.zip(fieldList())
                        .mapNotNull { (field, declared) -> field.reference.errors(FieldCall(receiver, declared.name), Path(field.identifier.value), 0) }
                        .concatenated(),
                ),
            ),
            isOverride = true,
        )

        private val receiver: Expression? = when (dialect) {
            Dialect.Member -> null
            Dialect.Python, Dialect.Rust -> VariableReference(Name.of("self"))
            Dialect.TypeScript -> VariableReference(OBJ)
        }

        private fun Reference.errors(subject: Expression, path: Path, depth: Int): Expression? = when {
            isNullable -> variable("it", depth).let { present ->
                copy(isNullable = false).errors(VariableReference(present), path, depth + 1)
                    ?.let { NullableMap(expression = subject, body = it, alternative = NO_ERRORS, variable = present) }
            }

            else -> when (this) {
                is Reference.Custom -> when (val definition = definitions[value]) {
                    is Refined -> IfExpression(NotExpression(validateCall(subject, definition)), path.errors(), NO_ERRORS)
                    is Model -> MapExpression(validateCall(subject, definition), ERROR, path.field(ERROR).expression())
                    else -> null
                }

                is Reference.Primitive -> takeIf { it.isConstrained() }
                    ?.convertConstraint(checked(subject))
                    ?.let { IfExpression(it, NO_ERRORS, path.errors()) }

                is Reference.Iterable -> (variable("i", depth) to variable("el", depth)).let { (index, element) ->
                    reference.errors(VariableReference(element), path.index(index), depth + 1)
                        ?.let { FlatMapIndexed(receiver = subject, indexVar = index, elementVar = element, body = it) }
                }

                is Reference.Dict -> (variable("k", depth) to variable("v", depth)).let { (key, entry) ->
                    reference.errors(VariableReference(entry), path.index(key), depth + 1)
                        ?.let { FlatMapEntries(receiver = subject, keyVar = key, valueVar = entry, body = it) }
                }

                is Reference.Any, is Reference.Unit -> null
            }
        }

        private fun validateCall(subject: Expression, definition: Definition): FunctionCall = when (dialect) {
            Dialect.TypeScript -> FunctionCall(name = Name.of("validate${definition.identifier.value}"), arguments = mapOf(OBJ to subject))
            Dialect.Member, Dialect.Python, Dialect.Rust -> FunctionCall(receiver = subject, name = VALIDATE)
        }

        // Rust binds list, dict and option values by reference, while a bound compares a number by value.
        private fun Reference.Primitive.checked(subject: Expression): Expression = when {
            dialect == Dialect.Rust && type !is Reference.Primitive.Type.String && subject is VariableReference ->
                FunctionCall(receiver = subject, name = Name.of("clone"))
            else -> subject
        }

        // A TypeScript model calls the validate functions of the types it nests, so it imports them.
        private fun validatorImports(file: File): List<Import> = when (dialect) {
            Dialect.TypeScript ->
                file.elements
                    .filterIsInstance<Struct>()
                    .mapNotNull { struct -> modelOf(struct) }
                    .flatMap { model -> model.shape.fields.flatMap { it.reference.validated() }.filterNot { it == model } }
                    .map { it.identifier.value }
                    .distinct()
                    .map { import("./$it", "validate$it") }

            Dialect.Member, Dialect.Python, Dialect.Rust -> emptyList()
        }

        private fun Reference.validated(): List<Definition> = when (this) {
            is Reference.Custom -> listOfNotNull(definitions[value]?.takeIf { it is Refined || it is Model })
            is Reference.Iterable -> reference.validated()
            is Reference.Dict -> reference.validated()
            is Reference.Primitive, is Reference.Any, is Reference.Unit -> emptyList()
        }
    }

    private class Path(private val parts: List<StringTemplate.Part>) {

        constructor(field: String) : this(listOf(StringTemplate.Part.Text(field)))

        fun index(variable: Name): Path = text("[").reference(variable).text("]")

        fun field(variable: Name): Path = text(".").reference(variable)

        fun errors(): LiteralList = LiteralList(listOf(expression()), Type.String)

        fun expression(): Expression = (parts.singleOrNull() as? StringTemplate.Part.Text)
            ?.let { Literal(it.value, Type.String) }
            ?: StringTemplate(parts)

        private fun text(value: String): Path = Path(
            (parts.lastOrNull() as? StringTemplate.Part.Text)
                ?.let { parts.dropLast(1) + StringTemplate.Part.Text(it.value + value) }
                ?: (parts + StringTemplate.Part.Text(value)),
        )

        private fun reference(variable: Name): Path = Path(parts + StringTemplate.Part.Expr(VariableReference(variable)))
    }

    private companion object {
        val SHAPE = Type.Custom("Wirespec.Shape").name
        val VALIDATE = Name.of("validate")
        val OBJ = Name.of("obj")
        val ERROR = Name.of("e")
        val NO_ERRORS = LiteralList(emptyList(), Type.String)

        // Every nesting level binds its own names, so no scope shadows another in languages that forbid it.
        fun variable(name: String, depth: Int): Name = Name.of(if (depth == 0) name else "$name$depth")

        fun Reference.Primitive.isConstrained(): Boolean = when (val type = type) {
            is Reference.Primitive.Type.String -> type.constraint != null
            is Reference.Primitive.Type.Integer -> type.constraint.isBounded()
            is Reference.Primitive.Type.Number -> type.constraint.isBounded()
            Reference.Primitive.Type.Boolean, Reference.Primitive.Type.Bytes -> false
        }

        fun Reference.Primitive.Type.Constraint.Bound?.isBounded(): Boolean = this != null && (min != null || max != null)

        fun List<Expression>.concatenated(): Expression = when (size) {
            0 -> NO_ERRORS
            1 -> single()
            else -> ListConcat(this)
        }

        fun List<Element>.withImports(imports: List<Import>): List<Element> = indexOfLast { it is Import }
            .let { last -> take(last + 1) + imports + drop(last + 1) }
    }
}
