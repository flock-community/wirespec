package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Identifier.Type.*
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class ScalaEmitter(
    private val packageName: String = DEFAULT_GENERATED_PACKAGE_STRING,
    logger: Logger = noLogger
) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${identifier.emit(Field)}Endpoint"
        is Channel -> "${identifier.emit(Field)}Channel"
        is Enum -> identifier.emit(Field)
        is Refined -> identifier.emit(Field)
        is Type -> identifier.emit(Field)
        is Union -> identifier.emit(Field)
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> = super.emit(ast)
        .map { Emitted(it.typeName, if (packageName.isBlank()) "" else "package $packageName\n\n${it.result}") }

    override fun emit(type: Type, ast: AST) = """
        |case class ${type.emitName()}(
        |${type.shape.emit()}
        |)
        |
    """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { it.emit() }.dropLast(1)

    override fun Field.emit() =
        "${Spacer}val ${identifier.emit(Field)}: ${if (isNullable) "Option[${reference.emit()}]" else reference.emit()},"

    override fun Identifier.emit(type: Identifier.Type) = if (value in reservedKeywords) value.addBackticks() else value

    override fun emit(channel: Channel) = notYetImplemented()

    override fun Reference.emit() = when (this) {
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            Reference.Primitive.Type.String -> "String"
            Reference.Primitive.Type.Integer -> "Long"
            Reference.Primitive.Type.Number -> "Double"
            Reference.Primitive.Type.Boolean -> "Boolean"
        }
    }
        .let { if (isIterable) "List[$it]" else it }
        .let { if (isDictionary) "Map[String, $it]" else it }

    override fun emit(enum: Enum) = enum.run {
        fun String.sanitize() = replace("-", "_").let { if (it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class ${emitName()}(val label: String)
        |object ${identifier.emit(Field)} {
        |${entries.joinToString("\n") { """${Spacer}final case object ${it.sanitize().uppercase()} extends ${identifier.emit(Class)}(label = "$it")""" }}
        |}
        |""".trimMargin()
    }

    override fun emit(refined: Refined) = """
        |case class ${refined.emitName()}(val value: String) {
        |${Spacer}implicit class ${refined.emitName()}Ops(val that: ${refined.emitName()}) {
        |${refined.validator.emit()}
        |${Spacer}}
        |}
        |
        |
    """.trimMargin()


    override fun Refined.Validator.emit() =
        """${Spacer(2)}val regex = new scala.util.matching.Regex(${"\"\"\""}$expression${"\"\"\""})
            |${Spacer(2)}regex.findFirstIn(that.value)""".trimMargin()

    override fun emit(endpoint: Endpoint) = notYetImplemented()

    override fun emit(union: Union) = notYetImplemented()

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "abstract", "case", "catch", "class", "def",
            "do", "else", "extends", "false", "final",
            "finally", "for", "forSome", "if", "implicit",
            "import", "lazy", "match", "new", "null",
            "object", "override", "package", "private", "protected",
            "return", "sealed", "super", "this", "throw",
            "trait", "true", "try", "type", "val",
            "var", "while", "with", "yield",
        )
    }
}
