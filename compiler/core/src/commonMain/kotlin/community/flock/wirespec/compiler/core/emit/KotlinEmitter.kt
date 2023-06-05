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

    val base = """
        |import kotlin.reflect.KType
        |import kotlin.reflect.typeOf
        |
        |enum class Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |data class Content<T> (val type:String, val body:T )
        |interface Request<T> { val url:String; val method: Method; val query: Map<String, String>; val headers: Map<String, List<String>>; val content:Content<T>? }
        |interface Response<T> { val status:Int; val headers: Map<String, List<String>>; val content:Content<T>? }
        |interface ContentMapper<B> { fun <T> read(content: Content<B>, valueType: KType): Content<T> fun <T> write(content: Content<T>): Content<B> }
        |
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> =
        super.emit(ast)
            .map { (name, result) ->
                name to """
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
        |${SPACER}sealed interface ${name}Request<T>: Request<T>
        |${requests.joinToString("\n") { "${SPACER}class ${name}Request${it.content?.emitContentType() ?: "Unit"} ${emitSignature(it.content)}: ${name}Request<${it.content?.reference?.emit() ?: "Unit"}> {override val url = \"${path.emitPath()}\"; override val method = Method.${method.name}; override val query = mapOf<String, String>(${query.emitMap()}); override val headers = mapOf<String, List<String>>(${headers.emitMap()}); override val content = ${it.content?.let { "Content(\"${it.type}\", body)" } ?:"null"}}" }}
        |${SPACER}sealed interface ${name}Response<T>: Response<T>
        |${responses.filter { it.status.isInt() }.map{it.status.statusXX()}.toSet().joinToString("\n") { "${SPACER}sealed interface ${name}Response${it}<T>: ${name}Response<T>" }}
        |${responses.filter { it.status.isInt() }.map{it.status}.joinToString("\n") { "${SPACER}sealed interface ${name}Response${it}<T>: ${name}Response${it.statusXX()}<T>" }}
        |${responses.filter { it.status.isInt() }.joinToString("\n") { "${SPACER}class ${name}Response${it.emit()}: ${name}Response${it.status}<${it.content?.reference?.emit() ?: "Unit"}> { override val status = ${it.status}; override val content = ${it.content?.let { "Content(\"${it.type}\", body)" } ?: "null"}}" }}
        |suspend fun ${name.replaceFirstChar(Char::lowercase)}(request: ${name}Request<*>): ${name}Response<*>
        |${SPACER}companion object{
        |${SPACER}${SPACER}const val PATH = "${path.emitSegment()}"
        |${SPACER}${SPACER}${responses.emitResponseMapper(this)}
        |${SPACER}${SPACER}}
        |${SPACER}}
        |}
        |
        |""".trimMargin()
    }

    private fun String.statusXX() = substring(0,1) + "XX"
    private fun String.isInt() = toIntOrNull() != null
    private fun String.firstToUpper() = replaceFirstChar(Char::uppercase )

    private fun Endpoint.emitSignature(content: Endpoint.Content? = null): String {
        val pathField = path
            .filterIsInstance<Endpoint.Segment.Param>()
            .map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        return """
            |(${
            parameters
                .plus(content?.reference?.toField("body", false))
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

    private fun List<Type.Shape.Field>.emitMap() = joinToString(", ") { "\"${it.identifier.emit()}\" to ${it.identifier.emit()}.toString()" }

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
        "${content?.emitContentType() ?: "Unit"}(override val url: String, override val method: String,override val headers: Map<String, List<String>>, override val content: Content<${content?.reference?.emit() ?: "Unit"}>? )"

    override fun Endpoint.Response.emit() = withLogging(logger) {
        "${status}${content?.emitContentType() ?: "Unit"}(override val headers: Map<String, List<String>>${ content?.let { ", body: ${it.reference.emit()}" } ?: "" } )"
    }

    private fun List<Endpoint.Response>.emitResponseMapper(endpoint: Endpoint) = """
        |fun <B> RESPONSE_MAPPER(contentMapper: ContentMapper<B>) =
        |${SPACER}fun(status: Int, headers:Map<String, List<String>>, content: Content<B>?) =
        |${SPACER}${SPACER}when {
        |${filter { it.status.isInt() }.joinToString("") { it.emitResponseMapperCondition(endpoint) }}
        |${SPACER}${SPACER}${SPACER}else -> error("Cannot map response with status ${"$"}status")
        |
    """.trimMargin()

    private fun Endpoint.Response.emitResponseMapperCondition(endpoint: Endpoint) =
        when (content) {
            null -> """
                |${SPACER}${SPACER}${SPACER}status == ${status} && content == null -> ${endpoint.name}Response${status}Unit(headers)
                |
            """.trimMargin()

            else -> """
                |${SPACER}${SPACER}${SPACER}status == ${status} && content?.type == "${content.type}" -> contentMapper
                |${SPACER}${SPACER}${SPACER}${SPACER}.read<${content.reference.emit()}>(content, typeOf<${content.reference.emit()}>())
                |${SPACER}${SPACER}${SPACER}${SPACER}.let{ ${endpoint.name}Response${status}${content.emitContentType()}(headers, it.body) }
                |
            """.trimMargin()
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