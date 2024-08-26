package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class KotlinEmitter(
    private val packageName: String = DEFAULT_PACKAGE_STRING,
    logger: Logger = noLogger,
) : DefinitionModelEmitter, Emitter(logger, false) {

    open val import = """
        |
        |import community.flock.wirespec.Wirespec
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
                typeName = it.typeName.sanitizeSymbol(),
                result = """
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.needImports()) import else ""}
                    |${it.result}
                    |
                """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, ast: AST) = """
        |data class ${type.emitName()}(
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " : ${joinToString(", ") { it.emit() }}" }}
    """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}val ${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${identifier.emit()}: ${reference.emit()}${if (isNullable) "?" else ""}"

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
        .let { if (isIterable) "List<$it>" else it }
        .let { if (isDictionary) "Map<String, $it>" else it }


    override fun Identifier.emit() = if (value in preservedKeywords) value.addBackticks() else value

    override fun emit(refined: Refined) = """
        |data class ${refined.identifier.value.sanitizeSymbol()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun ${refined.identifier.value}.validate() = ${refined.validator.emit()}
    """.trimMargin()

    override fun Refined.Validator.emit() = "Regex(\"\"$value\"\").matches(value)"

    override fun emit(enum: Enum) = """
        |enum class ${enum.identifier.value.sanitizeSymbol()} (val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |}
    """.trimMargin()

    override fun emit(union: Union) = """
        |sealed interface ${union.emitName()}
    """.trimMargin()

    override fun emit(channel: Channel) = """
        |interface ${channel.identifier.emit()}Channel {
        |   operator fun invoke(message: ${channel.reference.emitWrap(channel.isNullable)})
        |}
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = """
        |object ${endpoint.identifier.emit()} {
        |${Spacer}interface Endpoint : Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.joinToString("\n") { it.emit(endpoint) }}
        |
        |${Spacer(2)}sealed interface Response<T: Any> : Wirespec.Response<T>
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.joinToString("\n") { it.emit() }}
        |
        |${Spacer(2)}companion object {
        |${Spacer(3)}const val PATH_TEMPLATE = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer(3)}const val METHOD_VALUE = "${endpoint.method}"
        |${Spacer(2)}}
        |
        |${Spacer(2)}interface Handler {
        |${Spacer(3)}suspend fun ${endpoint.identifier.emit().firstToLower()}(request: Request): Response<*>
        |${Spacer(2)}}
        |${Spacer}}
        |}
    """.trimMargin()

    private fun Endpoint.emitResponseInterfaces() = responses
        .distinctBy { it.status[0] }
        .joinToString("\n") { "${Spacer(2)}sealed interface Response${it.status[0]}XX<T: Any> : Response<T>" }

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer(2)}data object $name : $extends"
        else """
            |${Spacer(2)}data class $name(
            |${joinToString(",\n") { "${Spacer(3)}val ${block(it)}" }},
            |${Spacer(2)}) : $extends
        """.trimMargin()

    private fun <E> List<E>.emitParams(pure: Boolean = true, block: (E) -> String) =
        if (isEmpty()) ""
        else joinToString(",\n") { block(it) }
            .let { if (pure) "$it, " else "($it)" }

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer(2)}data class Request(
        |${Spacer(3)}override val path: Path,
        |${Spacer(3)}override val method: Wirespec.Method,
        |${Spacer(3)}override val queries: Queries,
        |${Spacer(3)}override val headers: Headers,
        |${Spacer(3)}override val body: ${content.emit()},
        |${Spacer(2)}) : Wirespec.Request<${content.emit()}> {
        |${Spacer(3)}constructor(${endpoint.pathParams.emitParams { it.emit() }}${endpoint.queries.emitParams { it.emit() }}${endpoint.headers.emitParams { it.emit() }}body: ${content.emit()}) : this(
        |${Spacer(4)}path = Path${endpoint.pathParams.emitParams(false) { it.identifier.emit() }},
        |${Spacer(4)}method = Wirespec.Method.${endpoint.method.name},
        |${Spacer(4)}queries = Queries${endpoint.queries.emitParams(false) { it.identifier.emit() }},
        |${Spacer(4)}headers = Headers${endpoint.headers.emitParams(false) { it.identifier.emit() }},
        |${Spacer(4)}body = body,
        |${Spacer(3)})
        |${Spacer(2)}}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer(2)}data class Response$status(override val body: ${content.emit()}) : Response${status[0]}XX<${content.emit()}> {
        |${Spacer(3)}override val status = $status
        |${Spacer(3)}override val headers = Headers
        |${Spacer(3)}data object Headers : Wirespec.Response.Headers
        |${Spacer(2)}}
    """.trimMargin()

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Unit"

    private fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "{${identifier.emit()}}"
        }

    private val Endpoint.pathParams get() = path.filterIsInstance<Endpoint.Segment.Param>()

    private fun Endpoint.Segment.Param.emit() = "${identifier.emit()}: ${reference.emit()}"

    private fun Reference.emitWrap(isNullable: Boolean): String = value
        .let { if (isIterable) "List<$it>" else it }
        .let { if (isNullable) "$it?" else it }
        .let { if (isDictionary) "Map<String, $it>" else it }

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .joinToString("") { it.firstToUpper() }
        .asSequence()
        .filter { it.isLetterOrDigit() || it in listOf('_') }
        .joinToString("")
        .sanitizeFirstIsDigit()

    private fun String.sanitizeFirstIsDigit() = if (firstOrNull()?.isDigit() == true) "_${this}" else this

    private fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_").sanitizeFirstIsDigit()

    private fun String.sanitizeKeywords() = if (this in preservedKeywords) "`$this`" else this

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
