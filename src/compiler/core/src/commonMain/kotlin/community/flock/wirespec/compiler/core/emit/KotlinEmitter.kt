package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.ClassModelEmitter
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.transformer.ClassModelTransformer.transform
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.core.emit.transformer.EnumClass
import community.flock.wirespec.compiler.core.emit.transformer.Field
import community.flock.wirespec.compiler.core.emit.transformer.Parameter
import community.flock.wirespec.compiler.core.emit.transformer.Reference
import community.flock.wirespec.compiler.core.emit.transformer.RefinedClass
import community.flock.wirespec.compiler.core.emit.transformer.TypeClass
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger,
) : ClassModelEmitter(logger, false) {

    override val shared = """
        |package community.flock.wirespec
        |
        |import java.lang.reflect.Type
        |import java.lang.reflect.ParameterizedType
        |
        |object Wirespec {
        |${SPACER}interface Enum
        |${SPACER}interface Endpoint
        |${SPACER}interface Refined { val value: String }
        |${SPACER}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${SPACER}@JvmRecord data class Content<T> (val type:String, val body:T )
        |${SPACER}interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: Type): Content<T> fun <T> write(content: Content<T>): Content<B> }
        |${SPACER}${SPACER}@JvmStatic fun getType(type: Class<*>, isIterable: Boolean): Type {
        |${SPACER}${SPACER}return if (isIterable) {
        |${SPACER}${SPACER}${SPACER}object : ParameterizedType {
        |${SPACER}${SPACER}${SPACER}${SPACER}override fun getRawType() = MutableList::class.java
        |${SPACER}${SPACER}${SPACER}${SPACER}override fun getActualTypeArguments() = arrayOf(type)
        |${SPACER}${SPACER}${SPACER}${SPACER}override fun getOwnerType() = null
        |${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}} else {
        |${SPACER}${SPACER}${SPACER}type
        |${SPACER}${SPACER}}
        |${SPACER}}
        |}
    """.trimMargin()

    val import = """
        |
        |import community.flock.wirespec.Wirespec
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

    override fun Type.emit(): String = transform().emit()

    fun TypeClass.emit() = """
        |data class ${name.sanitizeSymbol()}(
        |${fields.joinToString(",\n") { it.emit() }.spacer()}
        |)
        """.trimMargin()

    override fun Type.Shape.emit(): String = TODO("Not yet implemented")
    override fun Type.Shape.Field.emit(): String = TODO("Not yet implemented")
    override fun Type.Shape.Field.Identifier.emit(): String = TODO("Not yet implemented")
    override fun Type.Shape.Field.Reference.emit(): String = TODO("Not yet implemented")

    override fun Refined.emit() = transform().emit()

    fun RefinedClass.emit() = """
        |data class ${name.sanitizeSymbol()}(override val value: String): Wirespec.Refined {
        |${SPACER}override fun toString() = value
        |}
        |
        |fun $name.validate() = ${validator.emit()}
    """.trimMargin()

    override fun Refined.Validator.emit(): String = TODO("Not yet implemented")

    fun RefinedClass.Validator.emit() = "Regex(\"\"$value\"\").matches(value)"

    override fun Enum.emit(): String = transform().emit()

    fun EnumClass.emit() = run {
        fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()
        """
            |enum class ${name.sanitizeSymbol()} (val label: String): Wirespec.Enum {
            |${entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
            |${SPACER}override fun toString(): String {
            |${SPACER}${SPACER}return label
            |${SPACER}}
            |}
        """.trimMargin()
    }

    override fun Endpoint.emit(): String = transform().emit()

    fun EndpointClass.emit() = """
        |interface ${name.sanitizeSymbol()} : ${supers.joinToString(", ") { it.emitWrap() }} {
        |${SPACER}sealed interface Request<T> : Wirespec.Request<T>
        |${requestClasses.joinToString("\n") { it.emit() }.spacer()}
        |
        |${SPACER}sealed interface Response<T> : Wirespec.Response<T>
        |${responseInterfaces.joinToString("\n") { it.emit() }.spacer()}
        |${responseClasses.joinToString("\n") { it.emit() }.spacer()}
        |${SPACER}companion object {
        |${SPACER}${SPACER}const val PATH = "$path"
        |${SPACER}${SPACER}const val METHOD = "$method"
        |${requestMapper.emit().spacer(2)}
        |${responseMapper.emit().spacer(2)}
        |${SPACER}}
        |${SPACER}suspend fun ${functionName}(request: Request<*>): Response<*>
        |}
        """.trimMargin()

    fun EndpointClass.RequestClass.emit() = """
         |data class ${name.sanitizeSymbol()}(
         |${fields.joinToString(",\n") { it.emit() }.spacer()}
         |) : ${supers.joinToString(", ") { it.emitWrap() }} {
         |${requestParameterConstructor.emit().spacer()}
         |}
    """.trimMargin()

    fun EndpointClass.RequestClass.RequestParameterConstructor.emit(): String = """
        |constructor(${parameters.joinToString(", ") { it.emit() }}) : this(
        |${SPACER}path = "${path.emit()}",
        |${SPACER}method = Wirespec.Method.${method},
        |${SPACER}query = mapOf<String, List<Any?>>(${query.joinToString(", ") { "\"${it}\" to listOf($it)" }}),
        |${SPACER}headers = mapOf<String, List<Any?>>(${headers.joinToString(", ") { "\"${it}\" to listOf($it)" }}),
        |${SPACER}content = ${content?.emit() ?: "null"}
        |)
    """.trimMargin()

    fun EndpointClass.ResponseInterface.emit(): String = """
        |sealed interface ${name.emitWrap()} : ${`super`.emitWrap()}
    """.trimMargin()

    fun EndpointClass.ResponseClass.emit(): String = """
        |data class ${name.sanitizeSymbol()}(${fields.joinToString(", ") { it.emit() }}) : ${`super`.emitWrap()} {
        |${responseParameterConstructor.emit().spacer()}
        |}
    """.trimMargin()

    fun EndpointClass.ResponseClass.ResponseParameterConstructor.emit(): String = """
        |constructor(${parameters.joinToString(", ") { it.emit() }}) : this(
        |${SPACER}status = ${if (statusCode.isInt()) statusCode else "status"},
        |${SPACER}headers = mapOf<String, List<Any?>>(${headers.joinToString(", ") { "\"${it}\" to listOf(${it.sanitizeIdentifier()})" }}),
        |${SPACER}content = ${content?.emit() ?: "null"}
        |)
    """.trimMargin()

    fun EndpointClass.Path.emit(): String =
        value.joinToString("/", "/") {
            when (it) {
                is EndpointClass.Path.Literal -> it.value
                is EndpointClass.Path.Parameter -> "${'$'}{${it.value}}"
            }
        }

    fun EndpointClass.Content.emit(): String =
        """Wirespec.Content("$type", body)"""

    fun EndpointClass.RequestMapper.emit(): String = """
        |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
         |${SPACER}when {
         |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
         |${SPACER}${SPACER}else -> error("Cannot map request")
         |${SPACER}}
         |}
    """.trimMargin()

    fun EndpointClass.RequestMapper.RequestCondition.emit(): String =
        if (content == null)
            """request.content == null -> ${responseReference.emitWrap()}(request.path, request.method, request.query, request.headers)"""
        else
            """
                |request.content?.type == "${content.type}" -> contentMapper
                |  .read<${content.reference.emitWrap()}>(request.content!!, Wirespec.getType(${content.reference.emit()}::class.java, ${isIterable}))
                |  .let { ${responseReference.emitWrap()}(request.path, request.method, request.query, request.headers, it) }
            """.trimMargin()

    fun EndpointClass.ResponseMapper.emit(): String = """
         |fun <B> $name(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
         |${SPACER}when {
         |${this.conditions.joinToString("\n") { it.emit() }.spacer(2)}
         |${SPACER}${SPACER}else -> error("Cannot map response with status ${'$'}{response.status}")
         |${SPACER}}
         |}
    """.trimMargin()

    fun EndpointClass.ResponseMapper.ResponseCondition.emit(): String =
        if (content == null)
            """
                |${if (statusCode.isInt()) "response.status == $statusCode && " else ""}response.content == null -> ${responseReference.emitWrap()}(response.status, response.headers, null)
            """.trimMargin()
        else
            """
                |${if (statusCode.isInt()) "response.status == $statusCode && " else ""}response.content?.type == "${content.type}" -> contentMapper
                |  .read<${content.reference.emitWrap()}>(response.content!!, Wirespec.getType(${content.reference.emit()}::class.java, false))
                |  .let { ${responseReference.emitWrap()}(response.status, response.headers, it) }
            """.trimMargin()

    fun Parameter.emit(): String =
        "${identifier.sanitizeIdentifier()}: ${reference.emitWrap()}"

    fun Reference.Generics.emit(): String =
        if (references.isNotEmpty()) references.joinToString(", ", "<", ">") {
            it.emitWrap()
        } else
            ""

    fun Reference.emit(): String =
        when (this) {
            is Reference.Custom -> emit()
            is Reference.Language -> emit()
            is Reference.Wirespec -> emit()
        }

    private fun Reference.emitWrap(): String = emit()
        .let { if (isIterable) "List<$it>" else it }
        .let { if (isNullable) "$it?" else it }
        .let { if (isOptional) "$it?" else it }

    fun Reference.Wirespec.emit(): String =
        "Wirespec.${name}${generics.emit()}"

    fun Reference.Custom.emit(): String = """
        |${name.sanitizeSymbol()}${generics.emit()}
    """.trimMargin()

    fun Reference.Language.emit(): String = """
        |${primitive.emit()}${generics.emit()}
    """.trimMargin()

    fun Reference.Language.Primitive.emit(): String = when (this) {
        Reference.Language.Primitive.Any -> "Any"
        Reference.Language.Primitive.Unit -> "Unit"
        Reference.Language.Primitive.String -> "String"
        Reference.Language.Primitive.Integer -> "Int"
        Reference.Language.Primitive.Long -> "Long"
        Reference.Language.Primitive.Number -> "Double"
        Reference.Language.Primitive.Boolean -> "Boolean"
        Reference.Language.Primitive.Map -> "Map"
        Reference.Language.Primitive.List -> "List"
        Reference.Language.Primitive.Double -> "Double"
    }

    fun Field.emit(): String = """
        |${if (isOverride) "override " else ""}val ${identifier.sanitizeKeywords()}: ${reference.emitWrap()}${if (reference.isNullable) " = null" else ""}${if (reference.isOptional) " = null" else ""}
    """.trimMargin()

    private fun String.sanitizeIdentifier() = split("-")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .sanitizeKeywords()
        .sanitizeSymbol()
        .firstToLower()

    private fun String.sanitizeKeywords() = if (preservedKeywords.contains(this)) "`$this`" else this

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .joinToString("") { it.firstToUpper() }
        .asSequence()
        .filter { it.isLetterOrDigit() || listOf('_').contains(it) }
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