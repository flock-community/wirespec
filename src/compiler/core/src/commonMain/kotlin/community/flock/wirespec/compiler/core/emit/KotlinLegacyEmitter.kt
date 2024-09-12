package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.transformer.ClassModelTransformer.transform
import community.flock.wirespec.compiler.core.emit.transformer.ClassReference
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.FieldClass
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.emit.transformer.UnionClass
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class KotlinLegacyEmitter(
    private val packageName: String = DEFAULT_GENERATED_PACKAGE_STRING,
    logger: Logger = noLogger,
) : ClassModelEmitter, Emitter(logger, false) {

    val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${identifier.emit()}Endpoint"
        is Channel -> "${identifier.emit()}Channel"
        is Enum -> identifier.emit()
        is Refined -> identifier.emit()
        is Type -> identifier.emit()
        is Union -> identifier.emit()
    }

    override fun notYetImplemented() =
        """// TODO("Not yet implemented")
            |
        """.trimMargin()

    override fun emit(ast: AST): List<Emitted> =
        super.emit(ast).map {
            Emitted(
                it.typeName.sanitizeSymbol(), """
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.needImports()) import else ""}
                    |${it.result}
                    |
            """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, ast: AST) = type.transform(ast).emit()

    override fun TypeClass.emit() = """
        |data class ${name.sanitizeSymbol()}(
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |)${if (supers.isNotEmpty()) ": ${supers.joinToString(", ") { it.emit() }}" else ""}
        """.trimMargin()

    override fun emit(refined: Refined) = refined.transform().emit()

    override fun RefinedClass.emit() = """
        |data class ${name.sanitizeSymbol()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun $name.validate() = ${validator.emit()}
    """.trimMargin()

    override fun RefinedClass.Validator.emit() = "Regex(\"\"$value\"\").matches(value)"

    override fun emit(enum: Enum) = enum.transform().emit()

    override fun EnumClass.emit() = run {
        fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()
        """
            |enum class ${name.sanitizeSymbol()} (val label: String): Wirespec.Enum {
            |${entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
            |${Spacer}override fun toString(): String {
            |${Spacer(2)}return label
            |${Spacer}}
            |}
        """.trimMargin()
    }

    override fun emit(union: Union) = union.transform().emit()

    override fun emit(channel: Channel): String =
        """
            |interface ${channel.identifier.emit()}Channel {
            |   operator fun invoke(message: ${channel.reference.transform(channel.isNullable, false).emitWrap()})
            |}
        """.trimMargin()

    override fun UnionClass.emit(): String = """
        |sealed interface $name
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = endpoint.transform().emit()

    override fun EndpointClass.emit() = """
        |interface ${name.sanitizeSymbol()} : ${supers.joinToString(", ") { it.emitWrap() }} {
        |${Spacer}sealed interface Request<T> : Wirespec.Request<T>
        |${requestClasses.joinToString("\n") { it.emit() }.spacer()}
        |
        |${Spacer}sealed interface Response<T> : Wirespec.Response<T>
        |${responseInterfaces.joinToString("\n") { it.emit() }.spacer()}
        |${responseClasses.joinToString("\n") { it.emit() }.spacer()}
        |${Spacer}companion object {
        |${Spacer(2)}const val PATH = "$path"
        |${Spacer(2)}const val METHOD = "$method"
        |${requestMapper.emit().spacer(2)}
        |${responseMapper.emit().spacer(2)}
        |${Spacer}}
        |${Spacer}suspend fun ${functionName}(request: Request<*>): Response<*>
        |}
        """.trimMargin()

    override fun EndpointClass.RequestClass.emit() = """
         |data class ${name.sanitizeSymbol()}(
         |${fields.joinToString(",\n") { it.emit() }.spacer()}
         |) : ${supers.joinToString(", ") { it.emitWrap() }} {
         |${requestParameterConstructor.emit().spacer()}
         |}
    """.trimMargin()

    override fun EndpointClass.RequestClass.RequestAllArgsConstructor.emit() = notYetImplemented()

    override fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String = """
        |constructor(${parameters.joinToString(", ") { it.emit() }}) : this(
        |${Spacer}path = "${path.emit()}",
        |${Spacer}method = Wirespec.Method.${method},
        |${Spacer}query = mapOf<String, List<Any?>>(${query.joinToString(", ") { "\"${it}\" to listOf($it)" }}),
        |${Spacer}headers = mapOf<String, List<Any?>>(${headers.joinToString(", ") { "\"${it}\" to listOf($it)" }}),
        |${Spacer}content = ${content?.emit() ?: "null"}
        |)
    """.trimMargin()

    override fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emitWrap()} : ${`super`.emitWrap()}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.emit(): String = """
        |data class ${name.sanitizeSymbol()}(${fields.joinToString(", ") { it.emit() }}) : ${`super`.emitWrap()} {
        |${responseParameterConstructor.emit().spacer()}
        |}
    """.trimMargin()

    override fun EndpointClass.ResponseClass.ResponseAllArgsConstructor.emit() = notYetImplemented()

    override fun EndpointClass.ResponseClass.ResponseParameterConstructor.emit(): String = """
        |constructor(${parameters.joinToString(", ") { it.emit() }}) : this(
        |${Spacer}status = ${if (statusCode.isInt()) statusCode else "status"},
        |${Spacer}headers = mapOf<String, List<Any?>>(${headers.joinToString(", ") { "\"${it}\" to listOf(${it.sanitizeIdentifier()})" }}),
        |${Spacer}content = ${content?.emit() ?: "null"}
        |)
    """.trimMargin()

    override fun EndpointClass.Path.emit(): String =
        value.joinToString("/", "/") {
            when (it) {
                is EndpointClass.Path.Literal -> it.value
                is EndpointClass.Path.Parameter -> "${'$'}{${it.value}}"
            }
        }

    override fun EndpointClass.Content.emit(): String =
        """Wirespec.Content("$type", body)"""

    override fun EndpointClass.RequestMapper.emit(): String = """
        |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
        |${Spacer}when {
        |${conditions.joinToString("\n") { it.emit() }.spacer(2)}
        |${Spacer(2)}else -> error("Cannot map request")
        |${Spacer}}
        |}
    """.trimMargin()

    override fun EndpointClass.RequestMapper.RequestCondition.emit(): String =
        if (content == null)
            """request.content == null -> ${responseReference.emitWrap()}(request.path, request.method, request.query, request.headers)"""
        else
            """
                |request.content?.type == "${content.type}" -> contentMapper
                |  .read<${content.reference.emitWrap()}>(request.content!!, typeOf<${content.reference.emitWrap()}>())
                |  .let { ${responseReference.emitWrap()}(request.path, request.method, request.query, request.headers, it) }
            """.trimMargin()

    override fun EndpointClass.ResponseMapper.emit(): String = """
         |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
         |${Spacer}when {
         |${conditions.joinToString("\n") { it.emit() }.spacer(2)}
         |${Spacer(2)}else -> error("Cannot map response with status ${'$'}{response.status}")
         |${Spacer}}
         |}
    """.trimMargin()

    override fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String =
        if (content == null)
            """
                |${if (statusCode.isInt()) "response.status == $statusCode && " else ""}response.content == null -> ${responseReference.emitWrap()}(response.status, response.headers, null)
            """.trimMargin()
        else
            """
                |${if (statusCode.isInt()) "response.status == $statusCode && " else ""}response.content?.type == "${content.type}" -> contentMapper
                |  .read<${content.reference.emitWrap()}>(response.content!!, typeOf<${content.reference.emitWrap()}>())
                |  .let { ${responseReference.emitWrap()}(response.status, response.headers, it) }
            """.trimMargin()

    override fun Parameter.emit(): String =
        "${identifier.sanitizeIdentifier()}: ${reference.emitWrap()}"

    override fun ClassReference.Generics.emit(): String =
        if (references.isNotEmpty()) references.joinToString(", ", "<", ">") {
            it.emitWrap()
        } else
            ""

    override fun ClassReference.emit(): String =
        when (this) {
            is ClassReference.Custom -> emit()
            is ClassReference.Language -> emit()
            is ClassReference.Wirespec -> emit()
        }

    private fun ClassReference.emitWrap(): String = emit()
        .let { if (isIterable) "List<$it>" else it }
        .let { if (isNullable) "$it?" else it }
        .let { if (isOptional) "$it?" else it }
        .let { if (isDictionary) "Map<String, $it>" else it }

    override fun ClassReference.Wirespec.emit(): String =
        "Wirespec.${name}${generics.emit()}"

    override fun ClassReference.Custom.emit(): String = """
        |${if (name in internalClasses && !isInternal) "${packageName}." else ""}${name.sanitizeSymbol()}${generics.emit()}
    """.trimMargin()

    override fun ClassReference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}
    """.trimMargin()

    override fun ClassReference.Language.Primitive.emit(): String = when (this) {
        ClassReference.Language.Primitive.Any -> "Any"
        ClassReference.Language.Primitive.Unit -> "Unit"
        ClassReference.Language.Primitive.String -> "String"
        ClassReference.Language.Primitive.Integer -> "Int"
        ClassReference.Language.Primitive.Long -> "Long"
        ClassReference.Language.Primitive.Number -> "Double"
        ClassReference.Language.Primitive.Boolean -> "Boolean"
        ClassReference.Language.Primitive.Map -> "Map"
        ClassReference.Language.Primitive.List -> "List"
        ClassReference.Language.Primitive.Double -> "Double"
    }

    override fun FieldClass.emit(): String = """
        |${if (isOverride) "override " else ""}val ${identifier.sanitizeKeywords()}: ${reference.emitWrap()}${if (reference.isNullable) " = null" else ""}${if (reference.isOptional) " = null" else ""}
    """.trimMargin()

    private fun String.sanitizeIdentifier() = split("-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .sanitizeKeywords()
        .sanitizeSymbol()
        .firstToLower()

    private fun String.sanitizeKeywords() = if (this in preservedKeywords) "`$this`" else this

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .joinToString("") { it.firstToUpper() }
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    companion object {
        private val preservedKeywords = listOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while"
        )
    }

}
