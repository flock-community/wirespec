package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.Emitter.Companion.isStatusCode
import community.flock.wirespec.compiler.core.emit.EndpointDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.EndpointEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.ParamEmitter
import community.flock.wirespec.compiler.core.emit.SpaceEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Field

interface PythonEndpointDefinitionEmitter: EndpointDefinitionEmitter, PythonTypeDefinitionEmitter, EndpointEmitter, IdentifierEmitter, TypeDefinitionEmitter, ParamEmitter, ImportEmitter, SpaceEmitter {

    override fun emit(endpoint: Endpoint) = """
        |${endpoint.importReferences().joinToString("\n") { it.emitReferenceCustomImports() }}
        |
        |class ${emit(endpoint.identifier)} (Wirespec.Endpoint):
        |${endpoint.requests.first().emit(endpoint).spacer(1)}
        |${endpoint.responses.distinctByStatus().joinToString("\n") { it.emit(endpoint) }.spacer(1)}
        |${endpoint.emitResponseUnion().spacer(1)}
        |${endpoint.emitHandleClass().spacer(1)}
        |${endpoint.emitConvertClass().spacer(1)}
        |
        """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |@dataclass
        |class Request(Wirespec.Request[${content.emit()}]):
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Request.Path") { it.emit() }}
        |${endpoint.queries.emitObject("Queries", "Wirespec.Request.Queries") { it.emit() }}
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        |
        |${Spacer}@property
        |${Spacer}def body(self) -> ${content.emit()}:
        |${Spacer}${Spacer}return self._body
        |
        |${Spacer}@property
        |${Spacer}def path(self) -> Path:
        |${Spacer}${Spacer}return self._path
        |
        |${Spacer}@property
        |${Spacer}def queries(self) -> Queries:
        |${Spacer}${Spacer}return self._queries
        |
        |${Spacer}@property
        |${Spacer}def headers(self) -> Headers:
        |${Spacer}${Spacer}return self._headers
        |
        |${Spacer}_body:  ${content.emit()}
        |${Spacer}_headers: Headers
        |${Spacer}_queries: Queries
        |${Spacer}_path: Path
        |${Spacer}method: Wirespec.Method = Wirespec.Method.${endpoint.method.name}
        |
        |${Spacer}def __init__(self, ${paramList(endpoint).joinToString(", ") { it.emit() }}):
        |${emitAssignSelf(endpoint).spacer(2)}
        |
        """.trimMargin()

    private fun Endpoint.Request.emitToRawRequest(endpoint: Endpoint) = """
        |@staticmethod
        |def to_raw_request(serialization: Wirespec.Serializer, request: '${emit(endpoint.identifier)}.Request') -> Wirespec.RawRequest:
        |${Spacer}return Wirespec.RawRequest(
        |${Spacer}${Spacer}path = [${endpoint.path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> "str(request.path.${emit(it.identifier)})" } }}],
        |${Spacer}${Spacer}method = request.method.value,
        |${Spacer}${Spacer}queries = ${if (endpoint.queries.isNotEmpty()) endpoint.queries.joinToString(",\n", "{", "}") { it.emitSerializedParams("request", "queries") } else "{}"},
        |${Spacer}${Spacer}headers = ${if (endpoint.headers.isNotEmpty()) endpoint.headers.joinToString(",\n", "{", "}") { it.emitSerializedParams("request", "headers") } else "{}"},
        |${Spacer}${Spacer}body = serialization.serialize(request.body, ${content?.reference?.emitType() ?: NONE}),
        |${Spacer})
        |
    """.trimMargin()

    private fun Endpoint.Request.emitFromRawRequest(endpoint: Endpoint) = """
        |@staticmethod
        |def from_raw_request(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> '${emit(endpoint.identifier)}.Request':
        |${Spacer}return ${emit(endpoint.identifier)}.Request${emitDeserializedParams(endpoint)}
        |
    """.trimMargin()

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString(",\n") { it.emitDeserializedParams("request", "queries") }.orNull(),
        endpoint.headers.joinToString(",\n") { it.emitDeserializedParams("request", "headers") }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserialize(request.body, ${it.reference.emitType()}),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "()" else "(\n$it\n)" }

    private fun Endpoint.emitResponseUnion() = """
        |Response = ${responses.joinToString(" | ") { "Response${it.status}" }}
        |
    """.trimMargin()

    private fun Endpoint.emitHandleClass() = """
        |class Handler(Wirespec.Endpoint.Handler):
        |${Spacer}@abstractmethod
        |${Spacer}def ${identifier.value}(self, req: '${emit(identifier)}.Request') -> '${emit(identifier)}.Response': pass
        |
    """.trimMargin()

    private fun Endpoint.emitConvertClass() = """
        |class Convert(Wirespec.Endpoint.Convert[Request, Response]):
        |${requests.first().emitToRawRequest(this).spacer(1)}
        |${requests.first().emitFromRawRequest(this).spacer(1)}
        |${emitToRawResponse().spacer(1)}
        |${emitFromRawResponse().spacer(1)}
        |
    """.trimMargin()

    private fun ParamEmitter.Param.emit() = "${emit(identifier)}: ${reference.emit()}"
    private fun ParamEmitter.Param.emitAssignSelf() = "${emit(identifier)} = ${emit(identifier)}"
    private fun Endpoint.Request.emitAssignSelf(endpoint: Endpoint) = """
        |self._path = ${emit(endpoint.identifier)}.Request.Path(${paramList(endpoint).filter { it.type == ParamEmitter.Param.ParamType.PATH }.joinToString { it.emitAssignSelf() }})
        |self._queries =${emit(endpoint.identifier)}.Request.Queries(${paramList(endpoint).filter { it.type == ParamEmitter.Param.ParamType.QUERY }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._headers = ${emit(endpoint.identifier)}.Request.Headers(${paramList(endpoint).filter { it.type == ParamEmitter.Param.ParamType.HEADER }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._body = ${content?.let { "body" } ?: "None"}
    """.trimMargin()

    private fun Endpoint.Response.emitAssignSelf(endpoint: Endpoint) = """
        |self._headers = ${emit(endpoint.identifier)}.Response${status}.Headers(${paramList().filter { it.type == ParamEmitter.Param.ParamType.HEADER }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._body = ${content?.let { "body" } ?: "None"}
    """.trimMargin()

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${emit(value.identifier)} = serialization.deserialize(request.path[${index}], ${value.reference.emitType()})"""

    private fun Field.emitDeserializedParams(type: String, fields: String) =
        """${emit(identifier)} = serialization.deserialize_param($type.$fields.get("${identifier.value}".lower()), ${reference.emitType()})"""

    fun Endpoint.Response.emit(endpoint: Endpoint) = """
        |@dataclass
        |class Response${status}(Wirespec.Response[${content.emit()}]):
        |${headers.emitObject("Headers", "Wirespec.Response.Headers") { it.emit() }}
        |
        |${Spacer}@property
        |${Spacer}def headers(self) -> Headers:
        |${Spacer}${Spacer}return self._headers
        |
        |${Spacer}@property
        |${Spacer}def body(self) -> ${content.emit()}:
        |${Spacer}${Spacer}return self._body
        |
        |${Spacer}_body: ${content.emit()}
        |${Spacer}_headers: Headers
        |${Spacer}status: int = $status
        |
        |${Spacer}def __init__(self, ${paramList().joinToString(", ") { it.emit() }}):
        |${emitAssignSelf(endpoint).spacer(2)}
        |
        """.trimMargin()

    private fun Endpoint.emitToRawResponse() = """
        |@staticmethod
        |def to_raw_response(serialization: Wirespec.Serializer, response: '${emit(identifier)}.Response') -> Wirespec.RawResponse:
        |${Spacer}match response:
        |${responses.distinctByStatus().joinToString("\n") { it.emitSerialized(this) }.spacer(2)}
        |${Spacer}${Spacer}case _:
        |${Spacer}${Spacer}${Spacer}raise Exception("Cannot match response with status: " + str(response.status))
        """.trimMargin()

    private fun Endpoint.emitFromRawResponse() = """
        |@staticmethod
        |def from_raw_response(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> '${emit(identifier)}.Response':
        |${Spacer}match response.status_code:
        |${responses.distinctByStatus().filter { it.status.isStatusCode() }.joinToString("\n") { it.emitDeserialized(this) }.spacer(2)}
        |${Spacer(2)}case _:
        |${Spacer(3)}raise Exception("Cannot match response with status: " + str(response.status_code))
        |
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized(endpoint: Endpoint) = listOfNotNull(
        "case $status:",
        "${Spacer}return ${emit(endpoint.identifier)}.Response$status(",
        "${Spacer(2)}body = serialization.deserialize(response.body, ${content?.reference?.emitType() ?: NONE}),",
        headers.joinToString(",\n") { it.emitDeserializedParams("response", "headers") }.orNull()?.spacer(2),
        "${Spacer})"
    ).joinToString("\n")


    private fun Endpoint.Response.emitSerialized(endpoint: Endpoint) = """
        |case ${emit(endpoint.identifier)}.Response$status():
        |${Spacer(1)}return Wirespec.RawResponse(
        |${Spacer(2)}status_code = response.status,
        |${Spacer(2)}headers = ${if (headers.isNotEmpty()) headers.joinToString(", ", "{", "}") { it.emitSerializedParams("response", "headers") } else "{}"},
        |${Spacer(2)}body = ${content?.let { "serialization.serialize(response.body, ${it.reference.emitType()})" } ?: NONE},
        |${Spacer(1)})
    """.trimMargin()

    private fun Field.emitSerializedParams(type: String, fields: String) =
        """"${identifier.value}": serialization.serialize_param($type.$fields.${emit(identifier)}, ${reference.emitType()})"""


    private fun <E> List<E>.emitObject(name: String, extends: String, spaces: Int = 1, block: (E) -> String) =
        if (isEmpty()) """
            |${Spacer(spaces)}@dataclass
            |${Spacer(spaces)}class $name ($extends): pass
        """.trimMargin()
        else """
            |${Spacer(spaces)}@dataclass
            |${Spacer(spaces)}class $name ($extends):
            |${joinToString("\n") { "${Spacer(spaces + 2)}${block(it)}" }}
        """.trimMargin()

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()}"

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "None"

    companion object{
        private const val NONE = "type(None)"
    }
}