package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.PackageName
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
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class PythonEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
) : Emitter(true) {

    val import = """
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from .shared.Wirespec import T, Wirespec
        |from typing import List, Optional
    """.trimMargin()

    override fun Definition.emitName(): String = when (this) {
        is Endpoint -> "${emit(identifier)}Endpoint"
        is Channel -> "${emit(identifier)}Channel"
        is Enum -> emit(identifier)
        is Refined -> emit(identifier)
        is Type -> emit(identifier)
        is Union -> emit(identifier)
    }

    override val singleLineComment = "#"

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> {
        val list = ast.filterIsInstance<Enum>() + ast.filterIsInstance<Refined>() + ast.filterIsInstance<Type>() + ast.filterIsInstance<Union>() + ast.filterIsInstance<Endpoint>() + ast.filterIsInstance<Channel>()
        return super.emit(list.toNonEmptyListOrNull() ?: error("Non e"), logger).map { (typeName, result) ->
            Emitted(
                typeName = typeName.sanitizeSymbol(),
                result = """
                    |$import
                    |
                    |$result
                """.trimMargin()
            )
        }
    }

    private fun Node.localImports():List<Reference.Custom> = when (this) {
        is Channel -> TODO()
        is Endpoint -> listOf(
            path.filterIsInstance<Param>().map { it.reference },
            headers.map { it.reference },
            queries.map { it.reference },
            requests.map { it.content?.reference },
            responses.flatMap { listOf(it.content?.reference) + it.headers.map { it.reference }}
        ).flatten().filterNotNull().map { it.flattenListDict() }.filterIsInstance<Reference.Custom>().distinct()
        is Enum -> TODO()
        is Refined -> TODO()
        is Type -> shape.value.map { it.reference.flattenListDict() }.filterIsInstance<Reference.Custom>().distinct()
        is Union -> TODO()
    }

    private fun Reference.flattenListDict():Reference = when (this) {
        is Reference.Dict -> reference.flattenListDict()
        is Reference.Iterable -> reference.flattenListDict()
        else -> this
    }

    private fun Reference.Custom.emitReferenceCustomImports() = "from .${value} import ${value}"

    override fun emit(type: Type, ast: AST): String =
        if (type.shape.value.isEmpty()) """
            |${type.localImports().joinToString ("\n"){ it.emitReferenceCustomImports() }}
            |
            |@dataclass
            |class ${type.emitName()}:
            |${Spacer}pass
            |
        """.trimMargin()
        else """
            |${type.localImports().joinToString ("\n"){ it.emitReferenceCustomImports() }}
            |
            |@dataclass
            |class ${type.emitName()}${type.extends.run { if (isEmpty()) "" else "(${joinToString(", ") { it.emit() }})" }}:
            |${type.shape.emit()}
            |
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}${it.emit()}" }

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()} "

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}"

    private fun Param.emit() = "${emit(identifier)}: ${reference.emit()}"
    private fun Param.emitAssignSelf() = "${emit(identifier)} = ${emit(identifier)}"
    private fun Endpoint.Request.emitAssignSelf(endpoint: Endpoint) = """
        |self.path = ${endpoint.emitName()}.Request.Path(${paramList(endpoint).filter { it.type == Param.ParamType.PATH }.joinToString { it.emitAssignSelf() }})
        |self.queries = ${endpoint.emitName()}.Request.Queries(${paramList(endpoint).filter { it.type == Param.ParamType.QUERY }.joinToString { it.emitAssignSelf() }})
        |self.headers = ${endpoint.emitName()}.Request.Headers(${paramList(endpoint).filter { it.type == Param.ParamType.HEADER }.joinToString { it.emitAssignSelf() }})
        |self.body = body
    """.trimMargin()

    private fun Endpoint.Response.emitAssignSelf(endpoint: Endpoint) = """
        |self.headers = ${endpoint.emitName()}.Response${status}.Headers(${paramList().filter { it.type == Param.ParamType.HEADER }.joinToString { it.emitAssignSelf() }})
        |self.body = body
    """.trimMargin()

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Dict[str, ${reference.emit()}]"
        is Reference.Iterable -> "List[${reference.emit()}]"
        is Reference.Unit -> "None"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (type) {
            is Reference.Primitive.Type.String -> "str"
            is Reference.Primitive.Type.Integer -> "int"
            is Reference.Primitive.Type.Number -> "float"
            is Reference.Primitive.Type.Boolean -> "bool"
            is Reference.Primitive.Type.Bytes -> "bytes"
        }
    }.let { if (isNullable) "Optional[$it]" else it }

    override fun emit(identifier: Identifier) = when (identifier) {
        is DefinitionIdentifier -> identifier.value.sanitizeSymbol()
        is FieldIdentifier -> identifier.value.sanitizeSymbol().sanitizeKeywords()
    }

    override fun emit(refined: Refined) = """
        |@dataclass
        |class ${refined.identifier.value.sanitizeSymbol()}(Wirespec.Refined):
        |${Spacer}value: str
        |
        |${Spacer}def validate(self) -> bool:
        |${Spacer(2)}return bool(re.match(r"${refined.validator.expression}", self.value))
        |
        |${Spacer}def __str__(self) -> str:
        |${Spacer(2)}return self.value
    """.trimMargin()

    override fun emit(endpoint: Endpoint) = """
        |${endpoint.localImports().joinToString ("\n"){ it.emitReferenceCustomImports() }}
        |
        |class ${emit(endpoint.identifier)}Endpoint (Wirespec.Endpoint):
        |${endpoint.requests.first().emit(endpoint).spacer(1)}
        |${endpoint.emitToRawRequest().spacer(1)}
        |${endpoint.requests.first().emitFromRawRequest(endpoint).spacer(1)}
        |${endpoint.responses.joinToString ("\n"){it.emit(endpoint)}.spacer(1)}
        |${endpoint.emitResponseUnion().spacer(1)}
        |${endpoint.emitToRawResponse().spacer(1)}
        |${endpoint.emitFromRawResponse().spacer(1)}
        |${endpoint.emitHandleClass().spacer(1)}
        |
        """.trimMargin()

    private fun Endpoint.emitResponseUnion() = """
        |Response = ${responses.joinToString(" | "){ "Response${it.status}" }}
        |
    """.trimMargin()

    private fun Endpoint.emitHandleClass() = """
        |@abstractmethod
        |def ${identifier.value}(self, req: Request) -> Response: pass
    """.trimMargin()

    fun Endpoint.Request.emit(endpoint: Endpoint) = """
        |@dataclass
        |class Request(Wirespec.Request[${content.emit()}]):
        |${endpoint.pathParams.emitObject("Path", "Wirespec.Request.Path") { it.emit() }}
        |${endpoint.queries.emitObject("Queries", "Wirespec.Request.Queries") { it.emit() }}
        |${endpoint.headers.emitObject("Headers", "Wirespec.Request.Headers") { it.emit() }}
        | 
        |${Spacer}body: ${content.emit()} = None
        |${Spacer}method: Wirespec.Method = Wirespec.Method.${endpoint.method.name}
        |${Spacer}path: Path = None
        |${Spacer}queries: Queries = None
        |${Spacer}headers: Headers = None
        |
        |${Spacer}def __init__(self, ${paramList(endpoint).joinToString (", "){ it.emit() }}):
        |${emitAssignSelf(endpoint).spacer(2)}
        |
        """.trimMargin()

    private fun Endpoint.emitToRawRequest() = """
        |@staticmethod
        |def to_raw_request(serialization: Wirespec.Serializer, request: Request) -> Wirespec.RawRequest:
        |${Spacer}return Wirespec.RawRequest(
        |${Spacer}${Spacer}path = [${path.joinToString { when (it) {is Endpoint.Segment.Literal -> """"${it.value}""""; is Endpoint.Segment.Param -> "request.path.${it.identifier.value}"} }}],
        |${Spacer}${Spacer}method = request.method.value,
        |${Spacer}${Spacer}queries = ${if (queries.isNotEmpty()) queries.joinToString(", ", "{", "}") { it.emitSerializedParams("request", "queries") } else "{}"},
        |${Spacer}${Spacer}headers = ${if (headers.isNotEmpty()) headers.joinToString(", ", "{", "}") { it.emitSerializedParams("request", "headers") } else "{}"},
        |${Spacer}${Spacer}body = serialization.serialize(request.body, ${this.requests.first().content.emit()}),
        |${Spacer})
        |
    """.trimMargin()

    private fun Endpoint.Request.emitFromRawRequest(endpoint: Endpoint) = """
        |@staticmethod
        |def from_raw_request(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> Request:
        |${Spacer}return ${endpoint.emitName()}.Request${emitDeserializedParams(endpoint)}
        |
    """.trimMargin()

    private fun Endpoint.Request.emitDeserializedParams(endpoint: Endpoint) = listOfNotNull(
        endpoint.indexedPathParams.joinToString { it.emitDeserialized() }.orNull(),
        endpoint.queries.joinToString (",\n"){ it.emitDeserializedParams("request", "queries") }.orNull(),
        endpoint.headers.joinToString (",\n"){ it.emitDeserializedParams("request", "headers") }.orNull(),
        content?.let { """${Spacer(3)}body = serialization.deserialize(request.body, ${it.emit()}),""" }
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n)" }

    private fun IndexedValue<Endpoint.Segment.Param>.emitDeserialized() =
        """${Spacer(3)}${emit(value.identifier)} = serialization.deserialize(request.path[${index}], ${value.reference.emit()})"""

    private fun Field.emitDeserializedParams(type: String, fields: String) =
        if (reference.isNullable)
            """${emit(identifier)} = serialization.deserialize_param($type.$fields["${identifier.value}"], ${reference.emit()})"""
        else
            """${emit(identifier)} = serialization.deserialize_param($type.$fields["${identifier.value}"], ${reference.emit()})"""

    fun Endpoint.Response.emit(endpoint: Endpoint) = """
        |@dataclass
        |class Response${status}(Wirespec.Response[${content.emit()}]):
        |${headers.emitObject("Headers", "Wirespec.Response.Headers") { it.emit() }}
        |
        |${Spacer}body: ${content.emit()} = None
        |${Spacer}status: int = $status
        |${Spacer}headers: Headers = None
        |
        |${Spacer}def __init__(self, ${paramList().joinToString (", "){ it.emit() }}):
        |${emitAssignSelf(endpoint).spacer(2)}
        |
        """.trimMargin()

    private fun Endpoint.emitToRawResponse() = """
        |@staticmethod
        |def to_raw_response(serialization: Wirespec.Serializer, response: Response) -> Wirespec.RawResponse:
        |${Spacer}match response:
        |${responses.distinctBy { it.status }.joinToString("\n") { it.emitSerialized(this) }.spacer(2)}
        |
        """.trimMargin()

    private fun Endpoint.emitFromRawResponse() = """
        |@staticmethod
        |def from_raw_response(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> Response:
        |${Spacer}match response.status_code:
        |${responses.filter { it.status.isStatusCode() }.distinctBy { it.status }.joinToString("\n") { it.emitDeserialized(this) }.spacer(2)}
        |${Spacer(2)}case _: 
        |${Spacer(3)}raise Exception("Cannot match response with status: " + str(response.status_code))
        |
    """.trimMargin()

    private fun Endpoint.Response.emitDeserialized(endpoint: Endpoint) = listOfNotNull(
        "case $status:",
        "${Spacer}return ${endpoint.emitName()}.Response$status(",
        "${Spacer(2)}body = serialization.deserialize(response.body, ${content.emit()}),",
        headers.joinToString(",\n") { it.emitDeserializedParams("response", "headers") }.orNull()?.spacer(2),
        "${Spacer})"
    ).joinToString("\n")


    private fun Endpoint.Response.emitSerialized(endpoint: Endpoint) = """
        |case ${endpoint.emitName()}.Response$status():
        |${Spacer(1)}return Wirespec.RawResponse(
        |${Spacer(2)}status_code = response.status,
        |${Spacer(2)}headers = ${if (headers.isNotEmpty()) headers.joinToString(", ", "{", "}") { it.emitSerializedParams("response", "headers") } else "{}"},
        |${Spacer(2)}body = ${if (content != null) "serialization.serialize(response.body, ${content.emit()})" else "null"},
        |${Spacer(1)})
    """.trimMargin()

    private fun Field.emitSerializedParams(type: String, fields: String) =
        """"${emit(identifier)}": serialization.serialize_param($type.$fields.${emit(identifier)}, ${reference.emit()})"""


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

    override fun emit(channel: Channel) = ""

    override fun Refined.Validator.emit() = "re.compile(r\"${expression}\").match(value) is not None"

    override fun emit(enum: Enum, ast: AST) = """
        |class ${enum.identifier.value.sanitizeSymbol()}(str, Enum):
        |${enum.entries.joinToString("\n") { "${Spacer}${it.sanitizeEnum().sanitizeKeywords()} = \"$it\"" }}
        |
        |${Spacer}@property
        |${Spacer}def label(self) -> str:
        |${Spacer(2)}return self.value
        |
        |${Spacer}def __str__(self) -> str:
        |${Spacer(2)}return self.value
    """.trimMargin()

    override fun emit(union: Union) = """
        |class ${union.emitName()}(ABC):
        |${Spacer}pass
    """.trimMargin()

    private fun Endpoint.Content?.emit() = this?.reference?.emit() ?: "None"

    private fun String.sanitizeSymbol() = this
        .split(".", " ")
        .mapIndexed { index, s -> if (index > 0) s.firstToUpper() else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    fun String.sanitizeEnum() = split("-", ", ", ".", " ", "//").joinToString("_")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    fun String.sanitizeKeywords() = if (this in reservedKeywords) "_$this" else this

    companion object : Keywords {
        override val reservedKeywords = setOf(
            "False", "None", "True", "and", "as", "assert",
            "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for",
            "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or",
            "pass", "raise", "return", "try", "while",
            "with", "yield"
        )
    }
}
