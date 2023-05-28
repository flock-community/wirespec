package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_NAME
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    logger: Logger = noLogger
) : Emitter(logger) {

    val optIn = """
        |@file:OptIn(ExperimentalStdlibApi::class)
    """.trimMargin()
    val base = """
        |import kotlin.reflect.KType
        |import kotlin.reflect.typeOf
        |
        |enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |data class Content<T> (val type:String, val body:T )
        |data class Request<T> ( val url:String, val method: Method, val query: Map<String, Any?>, val headers: Map<String, List<Any>>, val content:Content<T>? )
        |interface Response<T> { val status:Int; val headers: Map<String, List<Any>>; val content:Content<T>? }
        |interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: KType): Content<T> fun <T> write(content: Content<T>): Content<B> }
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast)
            .map { (name, result) ->
                name to """
                    |${if (ast.hasEndpoints()) "${optIn}\n" else ""}
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.hasEndpoints()) "$base" else ""}
                    |${result}
            """.trimMargin().trimStart()
            }


    private fun AST.hasEndpoints() = any { it is Endpoint }

    override fun Type.emit() = withLogging(logger) {
        """|data class $name(
           |${shape.emit()}
           |)
           |
           |""".trimMargin()
    }

    override fun Type.Shape.emit() = withLogging(logger) {
        value.joinToString("\n") { "${SPACER}val ${it.emit()}," }.dropLast(1)
    }

    override fun Type.Shape.Field.emit() = withLogging(logger) {
        "${identifier.emit()}: ${reference.emit()}${if (isNullable) "?" else ""}"
    }

    override fun Type.Shape.Field.Identifier.emit() = withLogging(logger) {
        value
            .split("-")
            .mapIndexed { index, s -> if (index > 0) s.replaceFirstChar(Char::uppercase) else s }
            .joinToString("")
    }

    override fun Type.Shape.Field.Reference.emit() = withLogging(logger) {
        when (this) {
            is Custom -> value
            is Primitive -> when (type) {
                Primitive.Type.String -> "String"
                Primitive.Type.Integer -> "Int"
                Primitive.Type.Boolean -> "Boolean"
            }
        }.let { if (isIterable) "List<$it>" else it }
    }

    override fun Refined.emit() = withLogging(logger) {
        """data class $name(val value: String)
            |fun $name.validate() = ${validator.emit()}
            |
            |""".trimMargin()
    }

    override fun Refined.Validator.emit() = withLogging(logger) { "Regex($value).find(value)" }

    override fun Endpoint.emit() = withLogging(logger) {
        """interface $name {
        |${SPACER}sealed interface ${name}Response<T>: Response<T>
        |${responses.joinToString("\n") { "${SPACER}sealed interface ${name}Response${it.status}<T>: ${name}Response<T>" }}
        |${responses.joinToString("\n") { "${SPACER}data class Response${it.emit()}: ${name}Response${it.status}<${it.content?.reference?.emit() ?: "Unit"}>" }}
        |${requests.joinToString("\n") { it.emitFunction(this) }}
        |${SPACER}companion object{
        |${SPACER}${SPACER}const val PATH = "${path.emitSegment()}"
        |${SPACER}${SPACER}${requests.emitRequestMapper(this)}
        |${SPACER}${SPACER}${responses.emitResponseMapper()}
        |${SPACER}}
        |}
        |
        |""".trimMargin()
    }

    private fun Endpoint.Request.emitFunction(endpoint: Endpoint) =
        """
        |${SPACER}suspend fun ${endpoint.name.replaceFirstChar(Char::lowercase)}${content?.emitContentType().orEmptyString()}${endpoint.emitFunctionSignature(content)}:${endpoint.name}Response<out Any>
        """.trimMargin()

    private fun Endpoint.emitFunctionSignature(content: Endpoint.Content? = null): String {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return """
            |(${
            parameters
                .plus(content?.reference?.toField("content", false))
                .filterNotNull()
                .joinToString(", ") { it.emit() }
            })
        """.trimMargin()
    }

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Type.Shape.Field>.emitField() = joinToString(", ") { it.emit() }

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ") { "\"${it.identifier.emit()}\" to ${it.identifier.emit()}" }

    override fun Endpoint.Method.emit(): String = withLogging(logger) {
        TODO("Not yet implemented")
    }

    override fun Endpoint.Segment.emit(): String = withLogging(logger) {
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "\${${identifier.value}}"
        }
    }

    fun  List<Endpoint.Segment>.emitPath() = "/" + joinToString("/") { it.emit() }

    override fun Endpoint.Segment.Param.emit(): String = withLogging(logger) {
        when (reference) {
            is Custom -> identifier to reference.value
            is Primitive -> identifier to reference.type.name
        }.run { "$first: $second" }
    }

    override fun Endpoint.Segment.Literal.emit(): String = withLogging(logger) {
        TODO("Not yet implemented")
    }

    fun Endpoint.Request.emit() =
        "${content?.emitContentType() ?: "Unit"}(override val url: String, override val method: String,override val headers: Map<String, List<Any>>, override val content: Content<${content?.reference?.emit() ?: "Unit"}>? )"

    override fun Endpoint.Response.emit() = withLogging(logger) {
        "${status}${content?.emitContentType() ?: ""}(override val status: Int, override val headers: Map<String, List<Any>>, override val content: Content<${content?.reference?.emit() ?: "Unit"}>${content?.let { "" } ?: "? = null"} )"
    }

    private fun List<Endpoint.Request>.emitRequestMapper(endpoint: Endpoint) = joinToString("") { request ->
        """
            |fun REQUEST_MAPPER ${endpoint.emitFunctionSignature(request.content)} =
            |${SPACER}fun(contentType: String?) =
            |${SPACER}${SPACER}when (contentType) {
            |${SPACER}${SPACER}${SPACER}${request.content?.let { "\"${it.type}\"" } ?: "null"} -> Request<${request.content?.reference?.emit() ?: "Unit"}>("${endpoint.path.emitPath()}", Method.${endpoint.method.name}, mapOf(${endpoint.query.emitMap()}), mapOf(${endpoint.headers.emitMap()}), ${request.content?.reference?.let { "Content(\"${request.content.emitContentType()}\", content)" } ?:"null"} )
            |${SPACER}${SPACER}${SPACER}else -> error("Cannot map request with content type ${"$"}contentType")
            |${SPACER}${SPACER}}
        """.trimMargin()
    }

    private fun List<Endpoint.Response>.emitResponseMapper() = """
        |fun <B> RESPONSE_MAPPER(contentMapper: ContentMapper<B>) =
        |${SPACER}fun(status: Int, headers:Map<String, List<String>>, content: Content<B>) =
        |${SPACER}${SPACER}when {
        |${emitResponseMapperCondition()}  
        |${SPACER}${SPACER}${SPACER}else -> error("Cannot map response with status ${"$"}status and content type ${"$"}{content.type}")
        |${SPACER}${SPACER}}
    """.trimMargin()

    private fun List<Endpoint.Response>.emitResponseMapperCondition() = joinToString("") {
        when (it.content) {
            null -> """
                |${SPACER}${SPACER}${SPACER}status == ${it.status} && content.type == null -> Response${it.status}(status, headers)
                |
            """.trimMargin()

            else -> """
                |${SPACER}${SPACER}${SPACER}status == ${it.status} && content.type == "${it.content.type}" -> contentMapper
                |${SPACER}${SPACER}${SPACER}${SPACER}.read<${it.content.reference.emit()}>(content, typeOf<${it.content.reference.emit()}>())
                |${SPACER}${SPACER}${SPACER}${SPACER}.let{ Response${it.status}${it.content.emitContentType()}(status, headers, it) }
                |
            """.trimMargin()
        }
    }
}



fun Endpoint.Content.emitContentType() = type.split("/")
    .joinToString("") { it.replaceFirstChar ( Char::uppercase ) }

fun Type.Shape.Field.Reference.toField(identifier: String, isNullable: Boolean) = Type.Shape.Field(
    Type.Shape.Field.Identifier(identifier),
    this,
    isNullable
)

fun String?.orEmptyString() = this ?: ""