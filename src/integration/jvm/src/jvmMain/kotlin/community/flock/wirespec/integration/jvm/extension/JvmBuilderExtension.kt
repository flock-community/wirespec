package community.flock.wirespec.integration.jvm.extension

import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.Field
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Namespace
import community.flock.wirespec.compiler.core.ir.NullableEmpty
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.Parameter
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.ReturnStatement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.Visibility
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.fieldList
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeDefinition

/**
 * Adds an immutable builder to every generated model record:
 *
 * ```
 * Todo.builder().withId(id).withName("Groceries").build()
 * todo.toBuilder().withName("Laundry").build()
 * ```
 *
 * `Builder` holds every field of the model as optional; each `with<Field>` returns a copy
 * with that field set, and `build()` fails with an [IllegalStateException] naming the first
 * required field that was never set.
 *
 * The builder is expressed in IR so the generators render the field types. Where it lives
 * depends on the language: Java nests it in the record next to a static `builder()`; Kotlin
 * nests it in the data class and adds `builder()` through a raw companion object, because
 * Kotlin has no statics; Scala puts both in a companion `object`, because a case class
 * nested in a case class would be an inner class bound to an instance. Register on the Java,
 * Kotlin or Scala emitter; for any other [language] the IR is returned unchanged.
 */
public class JvmBuilderExtension(
    private val language: FileExtension,
) : IrExtension {

    private enum class Target { Java, Kotlin, Scala }

    private class Model(val type: Type.Custom, val builder: Type.Custom, val fields: List<Field>)

    private val target: Target? = when (language) {
        FileExtension.Java -> Target.Java
        FileExtension.Kotlin -> Target.Kotlin
        FileExtension.Scala -> Target.Scala
        FileExtension.TypeScript,
        FileExtension.Python,
        FileExtension.Rust,
        FileExtension.Wirespec,
        FileExtension.JSON,
        FileExtension.YAML,
        FileExtension.AvroJson,
        FileExtension.AvroIdl,
        FileExtension.GraphQL,
        -> null
    }

    override fun extend(ir: IR, ast: AST): IR = target?.let { target ->
        val recordNames = ast.modules.asSequence()
            .flatMap { it.statements }
            .filterIsInstance<TypeDefinition>()
            .map { Name.of(it.identifier.value).pascalCase() }
            .toSet()
        ir.map { (it as? LanguageFile)?.withBuilders(target, recordNames) ?: it }
    } ?: ir

    private fun LanguageFile.withBuilders(target: Target, recordNames: Set<String>): LanguageFile = copy(
        elements = elements.flatMap { element ->
            (element as? Struct)?.takeIf { it.isRecord(recordNames) }?.withBuilder(target) ?: listOf(element)
        },
    )

    private fun Struct.isRecord(recordNames: Set<String>): Boolean = name.pascalCase() in recordNames &&
        fieldList().isNotEmpty() &&
        elements.none { it is Struct && it.name == BUILDER }

    private fun Struct.withBuilder(target: Target): List<Element> = name.pascalCase().let { pascal ->
        // Scala resolves `Builder` inside the case class only through its companion.
        val builder = when (target) {
            Target.Java, Target.Kotlin -> Type.Custom(BUILDER.value())
            Target.Scala -> Type.Custom("$pascal.${BUILDER.value()}")
        }
        val model = Model(Type.Custom(pascal), builder, fieldList())
        when (target) {
            Target.Java -> listOf(copy(elements = elements + model.entryPoint() + model.toBuilder() + model.builder(target)))
            // An extension that ran earlier may have added an empty companion; ours replaces it.
            Target.Kotlin -> listOf(
                copy(
                    elements = elements.filterNot { it is RawElement && it.code.trim() == "companion object" } +
                        KOTLIN_COMPANION + model.toBuilder() + model.builder(target),
                ),
            )
            Target.Scala -> listOf(
                copy(elements = elements + model.toBuilder()),
                Namespace(name = name, elements = listOf(model.entryPoint(), model.builder(target))),
            )
        }
    }

    private fun Model.entryPoint(): Function = Function(
        name = Name.of("builder"),
        parameters = emptyList(),
        returnType = builder,
        body = listOf(ReturnStatement(ConstructorStatement(builder, fields.associate { it.name to NullableEmpty }))),
        isStatic = true,
    )

    private fun Model.toBuilder(): Function = Function(
        name = Name.of("toBuilder"),
        parameters = emptyList(),
        returnType = builder,
        body = listOf(
            ReturnStatement(
                ConstructorStatement(
                    builder,
                    fields.associate { it.name to it.self().let { self -> if (it.type is Type.Nullable) self else NullableOf(self) } },
                ),
            ),
        ),
    )

    private fun Model.builder(target: Target): Struct = Struct(
        name = BUILDER,
        fields = fields.map { Field(name = it.name, type = it.type.optional(), initializer = NullableEmpty) },
        elements = fields.map { wither(it) } + build(target),
        visibility = Visibility.PUBLIC,
    )

    private fun Model.wither(field: Field): Function = Function(
        name = Name(listOf("with") + Name.of(field.name.value()).parts),
        parameters = listOf(Parameter(VALUE, field.type.required())),
        returnType = builder,
        body = listOf(
            ReturnStatement(
                ConstructorStatement(
                    builder,
                    fields.associate { it.name to if (it.name == field.name) NullableOf(VariableReference(VALUE)) else it.self() },
                ),
            ),
        ),
    )

    private fun Model.build(target: Target): Function = Function(
        name = Name.of("build"),
        parameters = emptyList(),
        returnType = type,
        body = listOf(
            ReturnStatement(
                ConstructorStatement(
                    type,
                    fields.associate { it.name to if (it.type is Type.Nullable) it.self() else it.require(target, type) },
                ),
            ),
        ),
    )

    private fun Field.require(target: Target, model: Type.Custom): Expression = "${model.name.pascalCase()}.${name.value().trim('`')} is required"
        .let { message ->
            when (target) {
                Target.Java -> FunctionCall(
                    receiver = self(),
                    name = Name.of("orElseThrow"),
                    arguments = mapOf(Name.of("exceptionSupplier") to RawExpression("() -> new IllegalStateException(\"$message\")")),
                )
                Target.Kotlin -> FunctionCall(
                    name = Name.of("checkNotNull"),
                    arguments = mapOf(VALUE to self(), Name.of("lazyMessage") to RawExpression("{ \"$message\" }")),
                )
                Target.Scala -> FunctionCall(
                    receiver = self(),
                    name = Name.of("getOrElse"),
                    arguments = mapOf(Name.of("default") to RawExpression("throw new IllegalStateException(\"$message\")")),
                )
            }
        }

    private fun Field.self(): Expression = FieldCall(receiver = RawExpression("this"), field = name)

    private fun Type.optional(): Type = this as? Type.Nullable ?: Type.Nullable(this)

    private fun Type.required(): Type = (this as? Type.Nullable)?.type ?: this

    private companion object {
        val BUILDER = Name.of("Builder")
        val VALUE = Name.of("value")
        val KOTLIN_COMPANION = RawElement("companion object {\n  fun builder(): Builder = Builder()\n}")
    }
}
