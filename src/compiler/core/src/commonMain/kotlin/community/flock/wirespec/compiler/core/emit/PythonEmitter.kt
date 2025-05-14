package community.flock.wirespec.compiler.core.emit

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.emit.common.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.EmitShared
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.emit.common.FileExtension
import community.flock.wirespec.compiler.core.emit.common.Keywords
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.emit.common.plus
import community.flock.wirespec.compiler.core.emit.shared.PythonShared
import community.flock.wirespec.compiler.core.orNull
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Model
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.compiler.utils.Logger

open class PythonEmitter(
    private val packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared()
) : Emitter() {

    val import = """
        |import re
        |
        |from abc import abstractmethod
        |from dataclasses import dataclass
        |from typing import List, Optional
        |from enum import Enum
        |
        |from ..wirespec import T, Wirespec
        |
    """.trimMargin()

    override val extension = FileExtension.Python

    override val shared = PythonShared

    override val singleLineComment = "#"

    fun sort(definition: Definition) = when (definition) {
        is Enum -> 1
        is Refined -> 2
        is Type -> 3
        is Union -> 4
        is Endpoint -> 5
        is Channel -> 6
    }

    override fun emit(module: Module, logger: Logger): NonEmptyList<Emitted> {
        val statements = module.statements.sortedBy(::sort).toNonEmptyListOrNull()!!
        return super.emit(module.copy(statements = statements), logger).let {
            fun emitInit(def: Definition) = "from .${emit(def.identifier)} import ${emit(def.identifier)}"
            val init = Emitted(
                packageName.toDir() + "__init__",
                "from . import model\nfrom . import endpoint\nfrom . import wirespec\n"
            )
            val initEndpoint = Emitted(
                packageName.toDir() + "endpoint/" + "__init__",
                module.statements.filter { it is Endpoint }.map { stmt -> emitInit(stmt) }.joinToString("\n")
            )
            val initModel = Emitted(
                packageName.toDir() + "model/" + "__init__",
                module.statements.filter { it is Model }.map { stmt -> emitInit(stmt) }.joinToString("\n")
            )
            val shared = Emitted(packageName.toDir() + "wirespec", shared.source)
            if (emitShared.value)
                it + init + initEndpoint + initModel + shared
            else
                it + init
        }
    }

    override fun emit(definition: Definition, module: Module, logger: Logger): Emitted {
        val subPackageName = packageName + definition
        return super.emit(definition, module, logger).let {
            Emitted(
                file = subPackageName.toDir() + it.file.sanitizeSymbol(),
                result = """
                    |${import}
                    |${it.result}
                """.trimMargin().trimStart()
            )
        }
    }

    private fun Reference.Custom.emitReferenceCustomImports() = "from ..model.${value} import $value"

    override fun emit(type: Type, module: Module): String =
        if (type.shape.value.isEmpty()) """
            |@dataclass
            |class ${emit(type.identifier)}:
            |${Spacer}pass
            |
            |${type.importReferences().joinToString("\n") { it.emitReferenceCustomImports() }}
            |
        """.trimMargin()
        else """
            |@dataclass
            |class ${emit(type.identifier)}:
            |${type.shape.emit()}
            |
            |${type.importReferences().joinToString("\n") { it.emitReferenceCustomImports() }}
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}${it.emit()}" }

    private fun Endpoint.Segment.Param.emit() = "${emit(identifier)}: ${reference.emit()} "

    override fun Field.emit() = "${emit(identifier)}: '${reference.emit()}'"

    private fun Param.emit() = "${emit(identifier)}: ${reference.emit()}"
    private fun Param.emitAssignSelf() = "${emit(identifier)} = ${emit(identifier)}"
    private fun Endpoint.Request.emitAssignSelf(endpoint: Endpoint) = """
        |self._path = ${emit(endpoint.identifier)}.Request.Path(${paramList(endpoint).filter { it.type == Param.ParamType.PATH }.joinToString { it.emitAssignSelf() }})
        |self._queries =${emit(endpoint.identifier)}.Request.Queries(${paramList(endpoint).filter { it.type == Param.ParamType.QUERY }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._headers = ${emit(endpoint.identifier)}.Request.Headers(${paramList(endpoint).filter { it.type == Param.ParamType.HEADER }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._body = ${content?.let { "body" } ?: "None"}
    """.trimMargin()

    private fun Endpoint.Response.emitAssignSelf(endpoint: Endpoint) = """
        |self._headers = ${emit(endpoint.identifier)}.Response${status}.Headers(${paramList().filter { it.type == Param.ParamType.HEADER }.joinToString(",\n") { it.emitAssignSelf() }.spacer(1)})
        |self._body = ${content?.let { "body" } ?: "None"}
    """.trimMargin()

    override fun Reference.emit() = emitType().let { if (isNullable) "Optional[$it]" else it }

    private fun Reference.emitType(): String = when (this) {
        is Reference.Dict -> "Dict[str, ${reference.emit()}]"
        is Reference.Iterable -> "List[${reference.emit()}]"
        is Reference.Unit -> "None"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> this.type.emit()
    }

    private fun Reference.emitRoot(): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Any -> emitType()
        is Reference.Custom -> emitType()
        is Reference.Primitive -> emitType()
        is Reference.Unit -> emitType()
    }

    fun Reference.Primitive.Type.emit() = when (this) {
        is Reference.Primitive.Type.String -> "str"
        is Reference.Primitive.Type.Integer -> "int"
        is Reference.Primitive.Type.Number -> "float"
        is Reference.Primitive.Type.Boolean -> "bool"
        is Reference.Primitive.Type.Bytes -> "bytes"
    }

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
    ).joinToString(",\n").let { if (it.isBlank()) "" else "(\n$it\n)" }

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
        |${Spacer(2)}body = ${content?.let { "serialization.serialize(response.body, ${content.reference.emitType()})" } ?: NONE},
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

    override fun emit(channel: Channel) = ""

    override fun Refined.Validator.emit() = "re.compile(r\"${expression}\").match(value) is not None"

    override fun emit(enum: Enum, module: Module) = """
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
        |class ${emit(union.identifier)}(ABC):
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

        private const val NONE = "type(None)"
    }
}
