package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Field.Reference
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class ScalaEmitter(
    private val packageName: String = DEFAULT_PACKAGE_STRING,
    logger: Logger = noLogger
) : DefinitionModelEmitter, Emitter(logger) {

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${identifier.emit()}Endpoint"
        is Enum -> identifier.emit()
        is Refined -> identifier.emit()
        is Type -> identifier.emit()
        is Union -> identifier.emit()
    }

    override fun emit(ast: AST): List<Emitted> = super.emit(ast)
        .map { Emitted(it.typeName, if (packageName.isBlank()) "" else "package $packageName\n\n${it.result}") }

    override fun Type.emit(ast: AST) =
        """case class ${emitName()}(
            |${shape.emit()}
            |)
            |
            |""".trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { it.emit() }.dropLast(1)

    override fun Field.emit() =
        "${SPACER}val ${identifier.emit()}: ${if (isNullable) "Option[${reference.emit()}]" else reference.emit()},"

    override fun Identifier.emit() = if (value in preservedKeywords) value.addBackticks() else value

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
    }.let { if (isIterable) "List[$it]" else it }

    override fun Enum.emit() = run {
        fun String.sanitize() = replace("-", "_").let { if (it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class ${emitName()}(val label: String)
        |object ${identifier.emit()} {
        |${
            entries.joinToString("\n") {
                """${SPACER}final case object ${
                    it.sanitize().uppercase()
                } extends ${identifier.emit()}(label = "$it")"""
            }
        }
        |}
        |""".trimMargin()
    }

    override fun Refined.emit() =
        """case class ${emitName()}(val value: String) {
            |${SPACER}implicit class ${emitName()}Ops(val that: ${emitName()}) {
            |${validator.emit()}
            |${SPACER}}
            |}
            |
            |""".trimMargin()


    override fun Refined.Validator.emit() =
        """${SPACER}${SPACER}val regex = new scala.util.matching.Regex(""$value"")
            |${SPACER}${SPACER}regex.findFirstIn(that.value)""".trimMargin()

    override fun Endpoint.emit() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun Union.emit() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    companion object {
        private val preservedKeywords = listOf(
            "abstract",
            "case",
            "catch",
            "class",
            "def",
            "do",
            "else",
            "extends",
            "false",
            "final",
            "finally",
            "for",
            "forSome",
            "if",
            "implicit",
            "import",
            "lazy",
            "match",
            "new",
            "null",
            "object",
            "override",
            "package",
            "private",
            "protected",
            "return",
            "sealed",
            "super",
            "this",
            "throw",
            "trait",
            "true",
            "try",
            "type",
            "val",
            "var",
            "while",
            "with",
            "yield",
        )
    }
}
