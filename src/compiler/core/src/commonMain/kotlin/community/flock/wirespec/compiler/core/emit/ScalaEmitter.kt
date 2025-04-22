package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.shared.ScalaShared
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class ScalaEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: Boolean = false,
) : Emitter() {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.scala.Wirespec
        |
    """.trimMargin()

    override val extension = FileExtension.Scala

    override val shared = ScalaShared

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${emit(identifier)}Endpoint"
        is Channel -> "${emit(identifier)}Channel"
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
    }

    override val singleLineComment = "//"

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> {
        val emitted = nonEmptyListOf(
            Emitted(
                file = module.uri.split("/").last().firstToUpper(),
                result = """
                    |package $packageName
                    |${if (module.needImports()) import else ""}
                    |${super.emit(module, logger).map(Emitted::result).joinToString("\n")}
                """.trimMargin().trimStart()
            )
        )

        return if (emitShared) emitted + Emitted(PackageName(DEFAULT_GENERATED_PACKAGE_STRING).toDir() + "Wirespec", shared.source) else emitted
    }

    override fun emit(type: Type, module: Module) = """
        |case class ${type.emitName()}(
        |${type.shape.emit()}
        |)
        |
    """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { it.emit() }.dropLast(1)

    override fun Field.emit() =
        "${Spacer}val ${emit(identifier)}: ${reference.emit()},"

    override fun emit(identifier: Identifier) =
        identifier.run { if (value in reservedKeywords) value.addBackticks() else value }

    override fun emit(channel: Channel) = notYetImplemented()

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Map[String, ${reference.emit()}]"
        is Reference.Iterable -> "List[${reference.emit()}]"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (type.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Int"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }

            is Reference.Primitive.Type.Number -> when (type.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }

            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "Array[Byte]"
        }
    }.let { if (isNullable) "Option[$it]" else it }

    override fun emit(enum: Enum, module: Module) = enum.run {
        fun String.sanitize() = replace("-", "_").let { if (it.first().isDigit()) "_$it" else it }
        """
        |sealed abstract class ${emitName()}(val label: String)
        |object ${emit(identifier)} {
        |${entries.joinToString("\n") { """${Spacer}final case object ${it.sanitize().uppercase()} extends ${emit(identifier)}(label = "$it")""" }}
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
