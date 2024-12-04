package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.addBackticks
import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.DefinitionModelEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger

open class KotlinEmitter(
    private val packageName: String = DEFAULT_GENERATED_PACKAGE_STRING,
    logger: Logger = noLogger,
) : DefinitionModelEmitter, Emitter(logger, false) {

    open val import = """
        |
        |import $DEFAULT_SHARED_PACKAGE_STRING.kotlin.Wirespec
        |import kotlin.reflect.typeOf
        |
    """.trimMargin()

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${emit(identifier)}Endpoint"
        is Channel -> "${emit(identifier)}Channel"
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
    }

    override val singleLineComment = "//"

    override fun emit(ast: AST): List<Emitted> =
        super.emit(ast).map { (typeName, result) ->
            Emitted(
                typeName = typeName.sanitizeSymbol(),
                result = """
                    |${if (packageName.isBlank()) "" else "package $packageName"}
                    |${if (ast.needImports()) import else ""}
                    |$result
                """.trimMargin().trimStart()
            )
        }

    override fun emit(type: Type, ast: AST) =
        if (type.shape.value.isEmpty()) "${Spacer}data object ${type.emitName()}"
        else """
            |data class ${type.emitName()}(
            |${type.shape.emit()}
            |)${type.extends.run { if (isEmpty()) "" else " : ${joinToString(", ") { it.emit() }}" }}
            |
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}val ${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}${if (isNullable) "?" else ""}"

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

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.value.sanitizeSymbol().firstToUpper()
        is FieldIdentifier -> identifier.value.sanitizeSymbol().firstToLower().sanitizeKeywords()
    }

    override fun emit(refined: Refined) = """
        |data class ${refined.identifier.value.sanitizeSymbol()}(override val value: String): Wirespec.Refined {
        |${Spacer}override fun toString() = value
        |}
        |
        |fun ${refined.identifier.value}.validate() = ${refined.validator.emit()}
        |
    """.trimMargin()

    override fun Refined.Validator.emit() = "Regex(\"\"\"${expression}\"\"\").matches(value)"

    override fun emit(enum: Enum) = """
        |enum class ${enum.identifier.value.sanitizeSymbol()} (override val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |}
        |
    """.trimMargin()

    override fun emit(union: Union) = """
        |sealed interface ${union.emitName()}
        |
    """.trimMargin()

    override fun emit(channel: Channel) = """
        |interface ${emit(channel.identifier)}Channel {
        |   operator fun invoke(message: ${channel.reference.emitWrap(channel.isNullable)})
        |}
        |
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = """
        |object ${emit(endpoint.identifier)}Endpoint : Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.first().emit(endpoint)}
        |
        |${Spacer}sealed interface Response<T: Any> : Wirespec.Response<T>
        |
        |${endpoint.emitStatusInterfaces()}
        |
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.distinctBy { it.status }.joinToString("\n\n") { it.emit() }}
        |
        |${Spacer}fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
        |${Spacer(2)}when(response) {
        |${endpoint.responses.distinctBy { it.status }.joinToString("\n") { it.emitSerialized() }}
        |${Spacer(2)}}
        |
        |${Spacer}fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
        |${Spacer(2)}when (response.statusCode) {
        |${endpoint.responses.filter { it.status.isStatusCode() }.distinctBy { it.status }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(3)}else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
        |${Spacer(2)}}
        |
        |${Spacer}interface Handler: Wirespec.Handler {
        |${Spacer(2)}${emitHandleFunction(endpoint)}
        |${Spacer(2)}companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        |${Spacer(3)}override val pathTemplate = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer(3)}override val method = "${endpoint.method}"
        |${Spacer(3)}override fun server(serialization: Wirespec.Serialization<String>) = object : Wirespec.ServerEdge<Request, Response<*>> {
        |${Spacer(4)}override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        |${Spacer(4)}override fun to(response: Response<*>) = toResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(3)}override fun client(serialization: Wirespec.Serialization<String>) = object : Wirespec.ClientEdge<Request, Response<*>> {
        |${Spacer(4)}override fun to(request: Request) = toRequest(serialization, request)
        |${Spacer(4)}override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
        |
    """.trimMargin()

    open fun emitHandleFunction(endpoint: Endpoint): String =
        "suspend fun ${emit(endpoint.identifier).firstToLower()}(request: Request): Response<*>"

    private fun Endpoint.emitStatusInterfaces() = responses
        .map { it.status[0] }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it}XX<T: Any> : Response<T>" }

    private fun Endpoint.emitResponseInterfaces() = responses
        .map { it.content.emit() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it.concatGenerics()} : Response<$it>" }

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}data object $name : $extends"
        else """
            |${Spacer}data class $name(
            |${joinToString(",\n") { "${Spacer(2)}val ${block(it)}" }},
            |${Spacer}) : $extends
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}${emitConstructor(endpoint)}
        |${Spacer(2)}override val path = Path${endpoint.pathParams.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val method = Wirespec.Method.${endpoint.method.name}
        |${Spacer(2)}override val queries = Queries${endpoint.queries.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val headers = Headers${endpoint.headers.joinToString { emit(it.identifier) }.brace()}${if (content == null) "\n${Spacer(2)}override val body = Unit" else ""}
        |${Spacer}}
        |
        |${Spacer}fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
        |${Spacer(2)}Wirespec.RawRequest(
        |${Spacer(3)}path = listOf(${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier() } }}),
        |${Spacer(3)}method = request.method.name,
        |${Spacer(3)}queries = ${if (endpoint.queries.isNotEmpty()) "listOf(${endpoint.queries.joinToString { it.emitSerialized("queries") }}).filterNotNull().toMap()" else "emptyMap()"},
        |${Spacer(3)}headers = ${if (endpoint.headers.isNotEmpty()) "listOf(${endpoint.headers.joinToString { it.emitSerialized("headers") }}).filterNotNull().toMap()" else "emptyMap()"},
        |${Spacer(3)}body = serialization.serialize(request.body, typeOf<${content.emit()}>()),
        |${Spacer(2)})
        |
        |${Spacer}fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
        |${Spacer(2)}Request${emitDeserializedParams(endpoint)}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}data class Response$status(override val body: ${content.emit()}) : Response${status[0]}XX<${content.emit()}>, Response${content.emit().concatGenerics()} {
        |${Spacer(2)}override val status = ${status.fixStatus()}
        |${Spacer(2)}override val headers = Headers
        |${Spacer(2)}data object Headers : Wirespec.Response.Headers
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitConstructor(endpoint: Endpoint) = listOfNotNull(
        endpoint.pathParams.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.queries.joinToString { Spacer(2) + it.emit() }.orNull(),
        endpoint.headers.joinToString { Spacer(2) + it.emit() }.orNull(),
        content?.let { "${Spacer(2)}override val body: ${it.emit()}," }
    ).joinToString(",\n")
        .let { if (it.isBlank()) "object Request : Wirespec.Request<${content.emit()}> {" else "class Request(\n$it\n${Spacer}) : Wirespec.Request<${content.emit()}> {" }

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString { it.emitDeserialized("queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserialized("headers") }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<${it.emit()}>()),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n${Spacer(2)})" }

    private fun Endpoint.Response.emitSerialized() = """
        |${Spacer(3)}is Response$status -> Wirespec.RawResponse(
        |${Spacer(4)}statusCode = response.status,
        |${Spacer(4)}headers = mapOf(),
        |${Spacer(4)}body = ${if (content != null) "serialization.serialize(response.body, typeOf<${content.emit()}>())" else "null"},
        |${Spacer(3)})
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized() = """
        |${Spacer(3)}$status -> Response$status(
        |${Spacer(4)}body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<${content.emit()}>()),
        |${Spacer(3)})
    """.trimMargin()

    private fun Field.emitSerialized(fields: String) =
        """request.$fields.${emit(identifier)}?.let{"${identifier.value}" to serialization.serialize(it, typeOf<${reference.emit()}>())}"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${emit(value.identifier)} = serialization.deserialize(request.path[${index}], typeOf<${value.reference.emit()}>())"""

    private fun Field.emitDeserialized(fields: String) =
        if (isNullable)
            """${Spacer(3)}${emit(identifier)} = request.$fields["${identifier.value}"]?.let{ serialization.deserialize(it, typeOf<${reference.emit()}>()) }"""
        else
            """${Spacer(3)}${emit(identifier)} = serialization.deserialize(requireNotNull(request.$fields["${identifier.value}"]) { "${emit(identifier)} is null" }, typeOf<${reference.emit()}>())"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "request.path.${emit(identifier)}.let{serialization.serialize(it, typeOf<${reference.emit()}>())}"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Unit"

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()}"

    private fun String.brace() = wrap("(", ")")
    private fun String.wrap(prefix: String, postfix: String) = if (isEmpty()) "" else "$prefix$this$postfix"

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

    private fun String.sanitizeKeywords() = if (this in reservedKeywords) addBackticks() else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "as", "break", "class", "continue", "do",
            "else", "false", "for", "fun", "if",
            "in", "interface", "internal", "is", "null",
            "object", "open", "package", "return", "super",
            "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while", "private", "public"
        )
    }

}
