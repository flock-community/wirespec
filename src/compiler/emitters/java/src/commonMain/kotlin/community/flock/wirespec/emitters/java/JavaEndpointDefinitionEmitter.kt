package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.concatGenerics
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.isStatusCode
import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.distinctByStatus
import community.flock.wirespec.compiler.core.emit.emit
import community.flock.wirespec.compiler.core.emit.fixStatus
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.indexedPathParams
import community.flock.wirespec.compiler.core.emit.pathParams
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference

interface JavaEndpointDefinitionEmitter: EndpointDefinitionEmitter, HasPackageName, JavaTypeDefinitionEmitter {

    override fun emit(endpoint: Endpoint) = """
        |${endpoint.emitImports()}
        |
        |public interface ${emit(endpoint.identifier)} extends Wirespec.Endpoint {
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Path") { it.emit() }}
        |
        |${endpoint.queries.emitObject("Queries", "Wirespec.Queries") { it.emit() }}
        |
        |${endpoint.headers.emitObject("RequestHeaders", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${endpoint.requests.first().emit(endpoint)}
        |
        |${Spacer}sealed interface Response<T> extends Wirespec.Response<T> {}
        |${endpoint.emitStatusInterfaces()}
        |${endpoint.emitResponseInterfaces()}
        |
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emit() }}
        |
        |${Spacer}interface Handler extends Wirespec.Handler {
        |
        |${endpoint.requests.first().emitRequestFunctions(endpoint)}
        |
        |${Spacer(2)}static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emitSerialized() }}
        |${Spacer(3)}else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
        |${Spacer(3)}switch (response.statusCode()) {
        |${endpoint.responses.distinctByStatus().filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized() }}
        |${Spacer(4)}default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
        |${Spacer(3)}}
        |${Spacer(2)}}
        |
        |${Spacer(2)}${emitHandleFunction(endpoint)}
        |${Spacer(2)}class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
        |${Spacer(3)}@Override public String getPathTemplate() { return "/${endpoint.path.joinToString("/") { it.emit() }}"; }
        |${Spacer(3)}@Override public String getMethod() { return "${endpoint.method}"; }
        |${Spacer(3)}@Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
        |${Spacer(4)}return new Wirespec.ServerEdge<>() {
        |${Spacer(5)}@Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
        |${Spacer(5)}@Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        |${Spacer(4)}};
        |${Spacer(3)}}
        |${Spacer(3)}@Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
        |${Spacer(4)}return new Wirespec.ClientEdge<>() {
        |${Spacer(5)}@Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
        |${Spacer(5)}@Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        |${Spacer(4)}};
        |${Spacer(3)}}
        |${Spacer(2)}}
        |${Spacer}}
        |}
        |
    """.trimMargin()

    open fun emitHandleFunction(endpoint: Endpoint) =
        "java.util.concurrent.CompletableFuture<Response<?>> ${emit(endpoint.identifier).firstToLower()}(Request request);"

    private fun Reference?.emitGetType() = "Wirespec.getType(${emitRoot("Void")}.class, ${emitGetTypeRaw()})"
    private fun Reference?.emitGetTypeRaw() = when {
        this?.isNullable?:false -> "java.util.Optional.class"
        this is Reference.Iterable -> "java.util.List.class"
        else -> null
    }

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |${Spacer}class Request implements Wirespec.Request<${content.emit()}> {
        |${Spacer(2)}private final Path path;
        |${Spacer(2)}private final Wirespec.Method method;
        |${Spacer(2)}private final Queries queries;
        |${Spacer(2)}private final RequestHeaders headers;
        |${Spacer(2)}private final ${content.emit()} body;
        |${Spacer(2)}${emitConstructor(endpoint)}
        |${Spacer(2)}@Override public Path getPath() { return path; }
        |${Spacer(2)}@Override public Wirespec.Method getMethod() { return method; }
        |${Spacer(2)}@Override public Queries getQueries() { return queries; }
        |${Spacer(2)}@Override public RequestHeaders getHeaders() { return headers; }
        |${Spacer(2)}@Override public ${content.emit()} getBody() { return body; }
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Request.emitConstructor(endpoint: Endpoint) =
        "public Request(${
            listOfNotNull(
                endpoint.pathParams.joinToString { it.emit() }.orNull(),
                endpoint.queries.joinToString { it.emit() }.orNull(),
                endpoint.headers.joinToString { it.emit() }.orNull(),
                content?.let { "${it.emit()} body" }
            ).joinToString()
        }) {\n${Spacer(3)}${
            listOfNotNull(
                endpoint.pathParams.joinToString { emit(it.identifier) }.let { "this.path = new Path($it);" },
                "this.method = Wirespec.Method.${endpoint.method.name};",
                endpoint.queries.joinToString { emit(it.identifier) }.let { "this.queries = new Queries($it);" },
                endpoint.headers.joinToString { emit(it.identifier) }
                    .let { "this.headers = new RequestHeaders($it);" },
                "this.body = ${content?.let { "body" } ?: "null"};"
            ).joinToString("\n${Spacer(3)}")
        }\n${Spacer(2)}}"

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString(",\n"){ it.emitDeserializedParams("queries") }.orNull(),
        endpoint.headers.joinToString(",\n"){ it.emitDeserializedParams("headers") }.orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(request.body(), ${it.reference.emitGetType()})""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    fun Endpoint.Response.emit() = """
        |${Spacer}record Response${status.firstToUpper()}(${listOfNotNull(headers.joinToString { it.emit() }.orNull(), content?.let { "${it.emit()} body" }).joinToString()}) implements Response${status.first()}XX<${content.emit()}>, Response${content.emit().concatGenerics()} {
        |${Spacer(2)}@Override public int getStatus() { return ${status.fixStatus()}; }
        |${Spacer(2)}${headers.joinToString { emit(it.identifier) }.let { "@Override public Headers getHeaders() { return new Headers($it); }" }}
        |${Spacer(2)}@Override public ${content.emit()} getBody() { return ${if (content == null) "null" else "body"}; }
        |${Spacer(1)}${headers.emitObject("Headers", "Wirespec.Response.Headers") { it.emit() }}
        |${Spacer}}
    """.trimMargin()

    private fun Endpoint.Response.emitDeserializedParams() = listOfNotNull(
        headers.joinToString(",\n") { """${Spacer(4)}serialization.deserializeParam(response.headers().getOrDefault("${it.identifier.value}", java.util.Collections.emptyList()), ${it.reference.emitGetType()})""" }.orNull(),
        content?.let { """${Spacer(4)}serialization.deserialize(response.body(), ${it.reference.emitGetType()})""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "\n$it\n${Spacer(3)}" }

    private fun Endpoint.Response.emitSerialized() =
        """${Spacer(3)}if (response instanceof Response${status.firstToUpper()} r) { return new Wirespec.RawResponse(r.getStatus(), ${if (headers.isNotEmpty()) "java.util.Map.ofEntries(${headers.joinToString { it.emitSerializedHeader() }})" else EMPTY_MAP}, ${
            if (content != null) "serialization.serialize(r.body, ${content!!.reference.emitGetType()})"
            else "null"}); }"""

    private fun Endpoint.Response.emitDeserialized() =
        """${Spacer(4)}case $status: return new Response${status.firstToUpper()}(${this.emitDeserializedParams()});"""


    private fun Field.emitSerializedParams(fields: String) =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(request.$fields.${emit(identifier)}, ${reference.emitGetType()}))"""

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(4)}serialization.deserialize(request.path().get(${index}), ${value.reference.emitGetType()})"""

    private fun Field.emitDeserializedParams(fields: String) =
        """${Spacer(4)}serialization.deserializeParam(request.$fields().getOrDefault("${identifier.value}", java.util.Collections.emptyList()), ${reference.emitGetType()})"""

    private fun Field.emitSerializedHeader() =
        """java.util.Map.entry("${identifier.value}", serialization.serializeParam(r.getHeaders().${emit(identifier)}(), ${reference.emitGetType()}))"""

    private fun Endpoint.Segment.Param.emitIdentifier() =
        "serialization.serialize(request.path.${emit(identifier).firstToLower()}, ${reference.emitGetType()})"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "Void"

    private fun Endpoint.Segment.Param.emit() = "${reference.emit()} ${emit(identifier)}"

    private val Reference.isIterable get() = this is Reference.Iterable

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { "import ${packageName.value}.model.${it.value};" }.joinToString("\n") { it.trimStart() }

    private fun Endpoint.Request.emitRequestFunctions(endpoint: Endpoint) = """
        |${Spacer(2)}static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
        |${Spacer(3)}return new Wirespec.RawRequest(
        |${Spacer(4)}request.method.name(),
        |${Spacer(4)}java.util.List.of(${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> it.emitIdentifier() } }}),
        |${Spacer(4)}${if (endpoint.queries.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.queries.joinToString { it.emitSerializedParams("queries") }})" else EMPTY_MAP},
        |${Spacer(4)}${if (endpoint.headers.isNotEmpty()) "java.util.Map.ofEntries(${endpoint.headers.joinToString { it.emitSerializedParams("headers") }})" else EMPTY_MAP},
        |${Spacer(4)}${if (content != null) "serialization.serialize(request.getBody(), ${content?.reference?.emitGetType()})" else "null"}
        |${Spacer(3)});
        |${Spacer(2)}}
        |
        |${Spacer(2)}static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
        |${Spacer(3)}return new Request(${emitDeserializedParams(endpoint)});
        |${Spacer(2)}}
    """.trimMargin()

    private fun <E> List<E>.emitObject(name: String, extends: String, block: (E) -> String) =
        if (isEmpty()) "${Spacer}class $name implements $extends {}"
        else """
            |${Spacer}public record $name(
            |${joinToString(",\n") { "${Spacer(2)}${block(it)}" }}
            |${Spacer}) implements $extends {}
        """.trimMargin()

    private fun Endpoint.emitResponseInterfaces() = responses
        .distinctByStatus()
        .map { it.content.emit() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it.concatGenerics()} extends Response<$it> {}" }


    private fun Endpoint.emitStatusInterfaces() = responses
        .map { it.status.first() }
        .distinct()
        .joinToString("\n") { "${Spacer}sealed interface Response${it}XX<T> extends Response<T> {}" }


    companion object {
        private const val EMPTY_MAP = "java.util.Collections.emptyMap()"
    }
}
