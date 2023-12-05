package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter.Companion.isInt
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : AbstractEmitter(logger) {

    override val shared = """
        |package community.flock.wirespec
        |
        |import java.lang.reflect.Type
        |import java.lang.reflect.ParameterizedType
        |
        |interface Wirespec {
        |${SPACER}enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${SPACER}@JvmRecord data class Content<T> (val type:String, val body:T )
        |${SPACER}interface Request<T> { val path:String; val method: Method; val query: Map<String, List<Any?>>; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface Response<T> { val status:Int; val headers: Map<String, List<Any?>>; val content:Content<T>? }
        |${SPACER}interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: Type): Content<T> fun <T> write(content: Content<T>): Content<B> }
        |${SPACER}companion object {
        |${SPACER}${SPACER}@JvmStatic fun getType(type: Class<*>, isIterable: Boolean): Type {
        |${SPACER}${SPACER}${SPACER}return if (isIterable) {
        |${SPACER}${SPACER}${SPACER}${SPACER}object : ParameterizedType {
        |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}override fun getRawType() = MutableList::class.java
        |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}override fun getActualTypeArguments() = arrayOf(type)
        |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}override fun getOwnerType() = null
        |${SPACER}${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}${SPACER}} else {
        |${SPACER}${SPACER}${SPACER}${SPACER}type
        |${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}}
        |${SPACER}}
        |}
    """.trimMargin()

    val import = """
        |
        |import community.flock.wirespec.Wirespec
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast).map { (name, result) ->
            name.sanitizeSymbol() to """
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.hasEndpoints()) import else ""}
                    |${result}
            """.trimMargin().trimStart()
        }

    object Test

    override fun Type.emit() = withLogging(logger) {
        if(shape.value.isEmpty()){
            """|object ${name.sanitizeSymbol()}
               |""".trimMargin()
        }else{
            """|data class ${name.sanitizeSymbol()}(
               |${shape.emit()}
               |)
               |""".trimMargin()
        }

    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString(",\n") { "${SPACER}val ${it.emit()}" }
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${identifier.emit()}: ${reference.emit()}${if (isNullable) "? = null" else ""}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) {
        value
            .split("-", ".")
            .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
            .joinToString("")
            .sanitizeKeywords()
    }

    private fun Type.Shape.Field.Reference.emitSymbol() = withLogging(logger) {
        when (this) {
            is Type.Shape.Field.Reference.Unit -> "Unit"
            is Type.Shape.Field.Reference.Any -> "Any"
            is Type.Shape.Field.Reference.Custom -> value.sanitizeSymbol()
            is Type.Shape.Field.Reference.Primitive -> when (type) {
                Type.Shape.Field.Reference.Primitive.Type.String -> "String"
                Type.Shape.Field.Reference.Primitive.Type.Integer -> "Int"
                Type.Shape.Field.Reference.Primitive.Type.Number -> "Double"
                Type.Shape.Field.Reference.Primitive.Type.Boolean -> "Boolean"
            }
        }
    }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        emitSymbol()
            .let { if (isIterable) "List<$it>" else it }
            .let { if (isMap) "Map<String, $it>" else it }
    }

    override fun Enum.emit() = withLogging(logger) {
        fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()
        "enum class ${name.sanitizeSymbol()} (val label: String){\n${SPACER}${entries.joinToString(",\n${SPACER}") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }};\n\n${SPACER}override fun toString(): String {\n${SPACER}${SPACER}return label\n${SPACER}}\n}\n"
    }

    override fun Refined.emit() = withLogging(logger) {
        """data class ${name.sanitizeSymbol()}(val value: String)
            |fun $name.validate() = ${validator.emit()}
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) { "Regex($value).find(value)" }

    override fun Endpoint.emit() = withLogging(logger) {
        """interface ${name.sanitizeSymbol()}Endpoint {
        |${SPACER}sealed interface Request<T>: Wirespec.Request<T>
        |${requests.joinToString("\n") { "${SPACER}data class Request${it.content?.emitContentType()?.sanitizeSymbol() ?: "Unit"}(override val path:String, override val method: Wirespec.Method, override val query: Map<String, List<Any?>>, override val headers: Map<String, List<Any?>>, override val content:Wirespec.Content<${it.content?.reference?.emit() ?: "Unit"}>?) : Request<${it.content?.reference?.emit() ?: "Unit"}> { constructor${emitRequestSignature(it.content)}: this(path = \"${path.emitPath()}\", method = Wirespec.Method.${method.name}, query = mapOf<String, List<Any?>>(${query.emitMap()}), headers = mapOf<String, List<Any?>>(${headers.emitMap()}), content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"})}" }}
        |${SPACER}sealed interface Response<T>: Wirespec.Response<T>
        |${responses.map { it.status.groupStatus() }.toSet().joinToString("\n") { "${SPACER}sealed interface Response${it}<T>: Response<T>" }}
        |${responses.filter { it.status.isInt() }.map { it.status }.toSet().joinToString("\n") { "${SPACER}sealed interface Response${it}<T>: Response${it.groupStatus()}<T>" }}
        |${responses.filter { it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { "${SPACER}data class Response${it.status}${it.content?.emitContentType()?.sanitizeSymbol() ?: "Unit"} (override val headers: Map<String, List<Any?>>${it.content?.let { ", val body: ${it.reference.emit()}" } ?: ""} ): Response${it.status}<${it.content?.reference?.emit() ?: "Unit"}> { override val status = ${it.status}; override val content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"}}" }}
        |${responses.filter { !it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { "${SPACER}data class Response${it.status.firstToUpper()}${it.content?.emitContentType()?.sanitizeSymbol() ?: "Unit"} (override val status: Int, override val headers: Map<String, List<Any?>>${it.content?.let { ", val body: ${it.reference.emit()}" } ?: ""} ): Response${it.status.firstToUpper()}<${it.content?.reference?.emit() ?: "Unit"}> { override val content = ${it.content?.let { "Wirespec.Content(\"${it.type}\", body)" } ?: "null"}}" }}
        |${SPACER}suspend fun ${name.sanitizeSymbol().firstToLower()}(request: Request<*>): Response<*>
        |${SPACER}companion object{
        |${SPACER}${SPACER}const val PATH = "${path.emitSegment()}"
        |${SPACER}${SPACER}const val METHOD = "${method.name}"
        |${requests.emitRequestMapper()}
        |${responses.emitResponseMapper()}
        |${SPACER}}
        |}
        |""".trimMargin()
    }

    private fun Endpoint.emitRequestSignature(content: Endpoint.Content? = null): String {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return """
            |(${parameters
                .plus(content?.reference?.toField("body", false))
                .filterNotNull()
                .joinToString(", ") { it.emit() }})
        """.trimMargin()
    }

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Type.Shape.Field>.emitMap() =
        joinToString(", ") { "\"${it.identifier.value}\" to listOf(${it.identifier.emit()})" }

    private fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${${identifier.emit()}}"
        }
    }

    private fun List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }

    fun Endpoint.Segment.Param.emit(): String = withLogging(logger) {
        "$identifier : $reference.emit()"
    }

    private fun List<Endpoint.Request>.emitRequestMapper() = """
        |${SPACER}${SPACER}fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
        |${SPACER}${SPACER}${SPACER}request: Wirespec.Request<B> -> when {
        |${joinToString("\n") { it.emitRequestMapperCondition() }}
        |${SPACER}${SPACER}${SPACER}${SPACER}else -> error("Cannot map request")
        |${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}}
    """.trimMargin()

    private fun Endpoint.Request.emitRequestMapperCondition() =
        when (content) {
            null -> """
                    |${SPACER}${SPACER}${SPACER}${SPACER}request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers, null)
                """.trimMargin()

            else -> """
                    |${SPACER}${SPACER}${SPACER}${SPACER}request.content?.type == "${content.type}" -> contentMapper
                    |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}.read<${content.reference.emit()}>(request.content!!, Wirespec.getType(${content.reference.emitSymbol()}::class.java, ${content.reference.isIterable}))
                    |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}.let{ Request${content.emitContentType().sanitizeSymbol()}(request.path, request.method, request.query, request.headers, it) }
                """.trimMargin()
        }

    private fun List<Endpoint.Response>.emitResponseMapper() = """
        |${SPACER}${SPACER}fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = {
        |${SPACER}${SPACER}${SPACER}response: Wirespec.Response<B> -> when {
        |${filter { it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { it.emitResponseMapperCondition() }}
        |${filter { !it.status.isInt() }.distinctBy { it.status to it.content?.type }.joinToString("\n") { it.emitResponseMapperCondition() }}
        |${SPACER}${SPACER}${SPACER}${SPACER}else -> error("Cannot map response with status ${"$"}{response.status}")
        |${SPACER}${SPACER}${SPACER}}
        |${SPACER}${SPACER}}
    """.trimMargin()

    private fun Endpoint.Response.emitResponseMapperCondition() =
        when (content) {
            null -> """
                    |${SPACER}${SPACER}${SPACER}${SPACER}${status.takeIf { it.isInt() }?.let { "response.status == $status && " }.orEmptyString()}response.content == null -> Response${status.firstToUpper()}Unit(${status.takeIf { !it.isInt() }?.let { "response.status, " }.orEmptyString()}response.headers)
                """.trimMargin()

            else -> """
                    |${SPACER}${SPACER}${SPACER}${SPACER}${status.takeIf { it.isInt() }?.let { "response.status == $status && " }.orEmptyString()}response.content?.type == "${content.type}" -> contentMapper
                    |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}.read<${content.reference.emit()}>(response.content!!, Wirespec.getType(${content.reference.emitSymbol()}::class.java, ${content.reference.isIterable}))
                    |${SPACER}${SPACER}${SPACER}${SPACER}${SPACER}.let{ Response${status.firstToUpper()}${content.emitContentType().sanitizeSymbol()}(${status.takeIf { !it.isInt() }?.let { "response.status, " }.orEmptyString()}response.headers, it.body) }
                """.trimMargin()
        }

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
            "as",
            "break",
            "class",
            "continue",
            "do",
            "else",
            "false",
            "for",
            "fun",
            "if",
            "in",
            "interface",
            "internal",
            "is",
            "null",
            "object",
            "open",
            "package",
            "return",
            "super",
            "this",
            "throw",
            "true",
            "try",
            "typealias",
            "typeof",
            "val",
            "var",
            "when",
            "while"
        )
    }

}


fun Type.Shape.Field.Reference.toField(identifier: String, isNullable: Boolean) = Type.Shape.Field(
    Type.Shape.Field.Identifier(identifier),
    this,
    isNullable
)

private fun String?.orEmptyString() = this ?: ""

private fun String.groupStatus() =
    if (isInt()) substring(0, 1) + "XX"
    else firstToUpper()
