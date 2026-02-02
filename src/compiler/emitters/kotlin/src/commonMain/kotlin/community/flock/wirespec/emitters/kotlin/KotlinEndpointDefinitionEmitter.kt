package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.isStatusCode
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.distinctByStatus
import community.flock.wirespec.compiler.core.emit.emit
import community.flock.wirespec.compiler.core.emit.fixStatus
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.indexedPathParams
import community.flock.wirespec.compiler.core.emit.pathParams
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.removeQuestionMark

interface KotlinEndpointDefinitionEmitter: EndpointDefinitionEmitter, HasPackageName, KotlinTypeDefinitionEmitter {

    override fun emit(endpoint: Endpoint) = """
        |${endpoint.importReferences().map { "import ${packageName.value}.model.${it.value}" }.joinToString("\n") { it.trimStart() }}
        |
        |object ${emit(endpoint.identifier)} : Wirespec.Endpoint {
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
        |${endpoint.responses.distinctByStatus().joinToString("\n\n") { it.emit() }}
        |
        |${Spacer}fun toResponse(serialization: Wirespec.Serializer, response: Response<*>): Wirespec.RawResponse =
        |${Spacer(2)}when(response) {
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitSerialized() }}
        |${Spacer(2)}}
        |
        |${Spacer}fun fromResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<*> =
        |${Spacer(2)}when (response.statusCode) {
        |${endpoint.responses.distinctByStatus().filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(3)}else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
        |${Spacer(2)}}
        |
        |${Spacer}interface Handler: Wirespec.Handler {
        |${Spacer(2)}${emitHandleFunction(endpoint)}
        |${Spacer(2)}companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
        |${Spacer(3)}override val pathTemplate = "/${endpoint.path.joinToString("/") { it.emit() }}"
        |${Spacer(3)}override val method = "${endpoint.method}"
        |${Spacer(3)}override fun server(serialization: Wirespec.Serialization) = object : Wirespec.ServerEdge<Request, Response<*>> {
        |${Spacer(4)}override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
        |${Spacer(4)}override fun to(response: Response<*>) = toResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(3)}override fun client(serialization: Wirespec.Serialization) = object : Wirespec.ClientEdge<Request, Response<*>> {
        |${Spacer(4)}override fun to(request: Request) = toRequest(serialization, request)
        |${Spacer(4)}override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
        |
    """.trimMargin()

    private fun Endpoint.emitStatusInterfaces() = responses
        .map { it.status[0] }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it}XX<T: Any> : Response<T>" }

    private fun Endpoint.emitResponseInterfaces() = responses
        .map { it.content.emit() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it.concatGenerics()} : Response<$it>" }

    open fun emitHandleFunction(endpoint: Endpoint): String =
        "suspend fun ${emit(endpoint.identifier).firstToLower()}(request: Request): Response<*>"

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}${emitConstructor(endpoint)}
        |${Spacer(2)}override val path = Path${endpoint.pathParams.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val method = Wirespec.Method.${endpoint.method.name}
        |${Spacer(2)}override val queries = Queries${endpoint.queries.joinToString { emit(it.identifier) }.brace()}
        |${Spacer(2)}override val headers = Headers${endpoint.headers.joinToString { emit(it.identifier) }.brace()}${if (content == null) "\n${Spacer(2)}override val body = Unit" else ""}
        |${Spacer}}
        |
        |${Spacer}fun toRequest(serialization: Wirespec.Serializer, request: Request): Wirespec.RawRequest =
        |${Spacer(2)}Wirespec.RawRequest(
        |${Spacer(3)}path = listOf(${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier() } }}),
        |${Spacer(3)}method = request.method.name,
        |${Spacer(3)}queries = ${if (endpoint.queries.isNotEmpty()) endpoint.queries.joinToString(" + ") { "(${it.emitSerializedParams("request", "queries", caseSensitive = true)})" } else EMPTY_MAP},
        |${Spacer(3)}headers = ${if (endpoint.headers.isNotEmpty()) endpoint.headers.joinToString(" + ") { "(${it.emitSerializedParams("request", "headers", caseSensitive = false)})" } else EMPTY_MAP},
        |${Spacer(3)}body = ${if(content != null) "serialization.serializeBody(request.body, typeOf<${content.emit()}>())" else "null"},
        |${Spacer(2)})
        |
        |${Spacer}fun fromRequest(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest): Request =
        |${Spacer(2)}Request${emitDeserializedParams(endpoint)}
    """.trimMargin()

    fun Endpoint.Response.emit() = """
        |${Spacer}data class Response$status(override val body: ${content.emit()}${headers.joinToString(", ") { "val ${it.emit()}" }.let { if (it.isBlank()) "" else ", $it"}}) : Response${status[0]}XX<${content.emit()}>, Response${content.emit().concatGenerics()} {
        |${Spacer(2)}override val status = ${status.fixStatus()}
        |${Spacer(2)}override val headers = ResponseHeaders${headers.joinToString { emit(it.identifier) }.brace()}
        |${headers.emitObject("ResponseHeaders", "Wirespec.Response.Headers", 2) { it.emit() }}
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
        endpoint.queries.joinToString { it.emitDeserializedParams("request", "queries") }.orNull(),
        endpoint.headers.joinToString { it.emitDeserializedParams("request", "headers", caseSensitive = false) }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserializeBody(requireNotNull(request.body) { "body is null" }, typeOf<${it.emit()}>()),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n${Spacer(2)})" }

    private fun Endpoint.Response.emitSerialized() = """
        |${Spacer(3)}is Response$status -> Wirespec.RawResponse(
        |${Spacer(4)}statusCode = response.status,
        |${Spacer(4)}headers = ${if (headers.isNotEmpty()) headers.joinToString(" + ") { "(${it.emitSerializedParams("response", "headers", caseSensitive = false)})" } else EMPTY_MAP},
        |${Spacer(4)}body = ${if (content != null) "serialization.serializeBody(response.body, typeOf<${content.emit()}>())" else "null"},
        |${Spacer(3)})
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized() = listOfNotNull(
        "${Spacer(3)}$status -> Response$status(",
        if (content != null) {
            "${Spacer(4)}body = serialization.deserializeBody(requireNotNull(response.body) { \"body is null\" }, typeOf<${content.emit()}>()),"
        } else {
            "${Spacer(4)}body = Unit,"
        },
        headers.joinToString(",\n") { it.emitDeserializedParams("response", "headers", 4, caseSensitive = false) }.orNull(),
        "${Spacer(3)})"
    ).joinToString("\n")

    private fun Field.emitSerializedParams(type: String, fields: String, caseSensitive: Boolean) =
        // Use lowercase for header names (RFC 7230 - headers are case-insensitive)
        """mapOf("${identifier.value.let{if(caseSensitive)it else it.lowercase()}}" to ($type.$fields.${emit(identifier)}?.let{ serialization.serializeParam(it, typeOf<${reference.emit()}>()) } ?: emptyList()))"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${emit(value.identifier)} = serialization.deserializePath(request.path[${index}], typeOf<${value.reference.emit()}>())"""

    private fun Field.emitDeserializedParams(type: String, fields: String, spaces: Int = 3, caseSensitive: Boolean = true) =
        // Use lowercase for header names (RFC 7230 - headers are case-insensitive)
        if (reference.isNullable)
            """${Spacer(spaces)}${emit(identifier)} = $type.$fields["${identifier.value.let{if(caseSensitive)it else it.lowercase()}}"]?.let{ serialization.deserializeParam(it, typeOf<${reference.emit()}>()) }"""
        else
            """${Spacer(spaces)}${emit(identifier)} = serialization.deserializeParam(requireNotNull($type.$fields["${identifier.value.let{if(caseSensitive)it else it.lowercase()}}"]) { "${emit(identifier)} is null" }, typeOf<${reference.emit()}>())"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "request.path.${emit(identifier)}.let{serialization.serializePath(it, typeOf<${reference.emit()}>())}"

    private fun Endpoint.Content?.emit() = this?.reference?.emit()?.removeQuestionMark() ?: "Unit"

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()}"

    private fun String.brace() = wrap("(", ")")

    private fun String.wrap(prefix: String, postfix: String) = if (isEmpty()) "" else "$prefix$this$postfix"

    private fun <E> List<E>.emitObject(name: String, extends: String, spaces: Int = 1, block: (E) -> String) =
        if (isEmpty()) "${Spacer(spaces)}data object $name : $extends"
        else """
            |${Spacer(spaces)}data class $name(
            |${joinToString(",\n") { "${Spacer(spaces + 1)}val ${block(it)}" }},
            |${Spacer(spaces)}) : $extends
        """.trimMargin()

    companion object {
        private const val EMPTY_MAP = "emptyMap()"
    }
}
