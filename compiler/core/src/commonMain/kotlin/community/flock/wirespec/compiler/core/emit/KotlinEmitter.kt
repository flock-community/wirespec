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
        |interface Request<T> { val url:String; val method: String; val headers: Map<String, List<String>>; val contentType:String?; val content:T? }
        |interface Response<T> { val status:Int; val headers: Map<String, List<String>>; val contentType:String; val content:T }
    """.trimMargin()

    override fun emit(ast: AST): List<Pair<String, String>> = super.emit(ast)
        .map { (name, result) -> name to if (packageName.isBlank()) "" else "package $packageName\n\n$base\n\n$result" }

    override fun Type.emit() = withLogging(logger) {
        """data class $name(
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
        val pathField =
            path.filterIsInstance<Endpoint.Segment.Param>().map { Type.Shape.Field(it.identifier, it.reference, false) }
        val parameters = pathField + query + headers + cookies
        """interface $name {
        |${SPACER}sealed interface ${name}Request
        |${requests.joinToString("\n") { "${SPACER}data class ${name}Request${it.emit()}: Request<${it.content?.reference?.emit() ?: "Unit"}>, ${name}Request" }}
        |${SPACER}sealed interface ${name}Response
        |${responses.joinToString("\n") { "${SPACER}sealed interface ${name}Response${it.status}: ${name}Response" }}
        |${responses.joinToString("\n") { "${SPACER}data class ${name}Response${it.emit()}: Response<${it.content?.reference?.emit() ?: "Unit"}>, ${name}Response${it.status}" }}
        |${SPACER}fun ${name}(reqest: ${name}Request):${name}Response {
        |${SPACER}${SPACER}TODO()
        |}
        |${SPACER}companion object{
        |${SPACER}${SPACER}const val PATH = "${path.emitSegment()}"
        |${SPACER}}
        |}
        |
        |""".trimMargin()

    }

    private fun List<Endpoint.Segment>.emitSegment() = "/" + joinToString("/") {
        when (it) {
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
            is Endpoint.Segment.Literal -> it.value
        }
    }

    private fun List<Type.Shape.Field>.emitField() = joinToString(", ") { it.emit() }

    override fun Endpoint.Method.emit(): String = withLogging(logger) {
        TODO("Not yet implemented")
    }

    override fun Endpoint.Segment.emit(): String = withLogging(logger) {
        TODO("Not yet implemented")
    }

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
        "${content?.type?.emitContentType()?:"Unit"}(override val url: String, override val method: String,override val headers:Map<String, List<String>>, override val contentType:String, override val content: ${content?.reference?.emit() ?: "Unit"})"

    override fun Endpoint.Response.emit() = withLogging(logger) {
        "${status}${content?.type?.emitContentType()}(override val status:Int, override val headers:Map<String, List<String>>, override val contentType:String, override val content: ${content?.reference?.emit() ?: "Unit"})"
    }
}

fun String.emitContentType() = split("/")
    .joinToString("") { it.replaceFirstChar { it.uppercase() } }