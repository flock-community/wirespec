package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.orNull
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
        |object ${endpoint.identifier.emit()}Endpoint : Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.joinToString("\n") { it.emit(endpoint) }}
        |
        |${Spacer}sealed interface Response<T: Any> : Wirespec.Response<T>
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.joinToString("\n") { it.emit() }}
        |
        |${Spacer}const val PATH_TEMPLATE = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer}const val METHOD_VALUE = "${endpoint.method}"
        |
        |${Spacer}interface Handler {
        |${Spacer(2)}suspend fun ${endpoint.identifier.emit().firstToLower()}(request: Request): Response<*>
        |${Spacer}}
        |}
    """.trimMargin()

    private fun Endpoint.emitResponseInterfaces() = responses
        .distinctBy { it.status[0] }
        .joinToString("\n") { "${Spacer}sealed interface Response${it.status[0]}XX<T: Any> : Response<T>" }

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}data object $name : $extends"
        else """
            |${Spacer}data class $name(
            |${joinToString(",\n") { "${Spacer(2)}val ${block(it)}" }},
            |${Spacer}) : $extends
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}data class Request(
        |${Spacer(2)}val path: Path,
        |${Spacer(2)}val method: Wirespec.Method,
        |${Spacer(2)}val queries: Queries,
        |${Spacer(2)}val headers: Headers,
        |${Spacer(2)}val body: ${content.emit()},
        |${Spacer}) {
        |${Spacer(2)}constructor(${emitParams(endpoint)}) : this(
        |${Spacer(3)}path = Path${endpoint.pathParams.joinToString { it.identifier.emit() }.brace()},
        |${Spacer(3)}method = Wirespec.Method.${endpoint.method.name},
        |${Spacer(3)}queries = Queries${endpoint.queries.joinToString { it.identifier.emit() }.brace()},
        |${Spacer(3)}headers = Headers${endpoint.headers.joinToString { it.identifier.emit() }.brace()},
        |${Spacer(3)}body = body,
        |${Spacer(2)})
        |${Spacer}}
        |
        |${Spacer}fun Wirespec.Serializer<String>.externalizeRequest(request: Request): Wirespec.RawRequest =
        |${Spacer(2)}Wirespec.RawRequest(
        |${Spacer(3)}path = listOf("${endpoint.pathLiterals.joinToString { it.value }}"${endpoint.pathParams.emitIdentifiers()}),
        |${Spacer(3)}method = request.method.name,
        |${Spacer(3)}queries = mapOf(${endpoint.queries.joinToString { it.emitSerialized("queries") }}),
        |${Spacer(3)}headers = mapOf(${endpoint.headers.joinToString { it.emitSerialized("headers") }}),
        |${Spacer(3)}body = serialize(request.body, typeOf<${content.emit()}>()),
        |${Spacer(2)})
        |
        |${Spacer}fun Wirespec.Deserializer<String>.consumeRequest(request: Wirespec.RawRequest): Request =
        |${Spacer(2)}Request(
        |${emitDeserializedParams(endpoint)}
        |${Spacer(2)})
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}data class Response$status(override val body: ${content.emit()}) : Response${status[0]}XX<${content.emit()}> {
        |${Spacer(2)}override val status = ${status.fixStatus()}
        |${Spacer(2)}override val headers = Headers
        |${Spacer(2)}data object Headers : Wirespec.Response.Headers
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.pathParams.joinToString { it.emit() }.orNull(),
        endpoint.queries.joinToString { it.emit() }.orNull(),
        endpoint.headers.joinToString { it.emit() }.orNull(),
        "body: ${content.emit()}"
    ).joinToString()

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.pathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString { it.emitDeserialized("queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserialized("headers") }.orNull(),
        """body = deserialize(requireNotNull(request.body) { "body is null" }, typeOf<${content.emit()}>()),"""
    ).joinToString(",\n") { "${Spacer(3)}$it" }

    private fun Field.emitSerialized(fields: String) =
        """"${identifier.emit()}" to listOf(serialize(request.$fields.${identifier.emit()}, typeOf<${reference.emit()}>()))"""

    private fun Endpoint.Segment.Param.emitDeserialized() =
        """${identifier.emit()} = deserialize(request.path[1], typeOf<${reference.emit()}>())"""

    private fun Field.emitDeserialized(fields: String) =
        """${identifier.emit()} = deserialize(requireNotNull(request.$fields["${identifier.emit()}"]?.get(0)) { "${identifier.emit()} is null" }, typeOf<${reference.emit()}>())"""

    private fun List<Endpoint.Segment.Param>.emitIdentifiers() =
        if (isEmpty()) ""
        else ", ${joinToString { "request.path.${it.identifier.value}.toString()" }}"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Unit"

    private fun Endpoint.Segment.emit() =
        when (this) {
            is Endpoint.Segment.Literal -> value
            is Endpoint.Segment.Param -> "{${identifier.emit()}}"
        }

    private val Endpoint.pathLiterals get() = path.filterIsInstance<Endpoint.Segment.Literal>()
    private val Endpoint.pathParams get() = path.filterIsInstance<Endpoint.Segment.Param>()

    private fun Endpoint.Segment.Param.emit() = "${identifier.emit()}: ${reference.emit()}"

    private fun String.brace() = wrap("(", ")")
    private fun String.wrap(prefix: String, postfix: String) =
        if (isEmpty()) "" else "$prefix$this$postfix"

    private fun Reference.emitWrap(isNullable: Boolean): String = value
        .let { if (isIterable) "List<$it>" else it }
        .let { if (isNullable) "$it?" else it }
        .let { if (isDictionary) "Map<String, $it>" else it }

    private fun String.fixStatus(): String = when (this) {
        "default" -> "200"
        else -> this
    }

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
