package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * Python reads and writes the envelope of a message with the `json` module of its standard library, and hands the
 * JSON of every value to the serialization. The transportation is a coroutine, so the client implements the `Service`
 * of an rpc only when that is one too: with `--sync` it has the same coroutines, without implementing it. With
 * `--sync` the server's `handle` is a plain function.
 */
internal object PythonJsonRpc : JsonRpcDialect {

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir + LanguageFile(
        Name.of(packageName.toDir() + "JsonRpc"),
        listOf(RawElement(listOf(imports(methods), RUNTIME, client(methods), server(methods), serverRuntime(methods.any { it.isAsync })).joinToString("\n\n\n", postfix = "\n"))),
    )

    private fun imports(methods: List<JsonRpcMethod>): String = (
        listOf("from __future__ import annotations", "import json as _json", "from dataclasses import dataclass", "from typing import Any, Callable, Optional", "from .wirespec import Wirespec") +
            methods.flatMap { it.models }.distinct().map { "from .model.${Name.of(it).pascalCase()} import ${Name.of(it).pascalCase()}" } +
            methods.map { "from .rpc.${it.rpcName} import ${it.rpcName}" }
        ).joinToString("\n")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |class JsonRpcClient${methods.filter { it.isAsync }.joinToString(", ") { "${it.rpcName}.Service" }.let { if (it.isEmpty()) "" else "($it)" }}:
        |    ${"\"\"\""}Sends every rpc as a JSON-RPC 2.0 request through the transportation, and reads the values with the serialization.${"\"\"\""}
        |
        |    def __init__(self, serialization: Wirespec.Serialization, transportation: Wirespec.Transportation):
        |        self.serialization = serialization
        |        self.transportation = transportation
        |
        |${methods.joinToString("\n") { it.clientFunction() }.prependIndent("    ")}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |async def ${service.name.value()}(self${params.joinToString("") { ", ${it.identifier()}: ${it.type.python()}" }}) -> ${returnType()}:
        |    return await _call(
        |        self.transportation,
        |        ${name.literal()},
        |        [${params.joinToString(", ") { it.param() }}],
        |        lambda raw: ${resultValue("raw")},
        |        ${error?.let { "lambda raw: $rpcName.Error(value=${it.decode("raw")})" } ?: "None"},
        |    )
    """.trimMargin()

    private fun JsonRpcParam.param(): String = when {
        isNullable -> "None if ${identifier()} is None else (${wireName.literal()}, ${reference.encode(identifier())})"
        else -> "(${wireName.literal()}, ${reference.encode(identifier())})"
    }

    private fun JsonRpcMethod.resultValue(raw: String): String = when (error) {
        null -> result.decodeResult(raw)
        else -> "$rpcName.Result(value=${result.decodeResult(raw)})"
    }

    private fun Reference.decodeResult(raw: String): String = when {
        this is Reference.Unit -> "None"
        isNullable -> "None if $raw == \"null\" else ${copy(isNullable = false).decode(raw)}"
        else -> decode(raw)
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName.Response" } ?: result.valueType().python().let { if (result.isNullable) "Optional[$it]" else it }

    private fun server(methods: List<JsonRpcMethod>): String = (if (methods.any { it.isAsync }) "async " to "await " else "" to "").let { (async, await) ->
        """
        |class JsonRpcServer:
        |    ${"\"\"\""}Answers JSON-RPC 2.0 requests, single or batched, by calling the service, which implements the Service of every rpc, and writes the values with the serialization.${"\"\"\""}
        |
        |    def __init__(self, serialization: Wirespec.Serialization, service: Any):
        |        self.serialization = serialization
        |        self.service = service
        |
        |    ${async}def handle(self, request: Wirespec.RawRequest) -> Wirespec.RawResponse:
        |        ${"\"\"\""}A batch is answered with the answers to its requests; a notification is not answered, so its response has status 204 and no body.${"\"\"\""}
        |        return ${await}_handle(request.body, self._dispatch)
        |
        |    ${async}def _dispatch(self, method: str, params: _Params) -> Optional[_Answer]:
        |${methods.joinToString("\n") { it.serverBranch(if (it.isAsync) "await " else "") }.prependIndent("        ")}
        |        return None
        """.trimMargin()
    }

    private fun JsonRpcMethod.serverBranch(await: String): String = """
        |if method == ${name.literal()}:
        |    response = ${await}self.service.${service.name.value()}(${params.joinToString(", ") { it.read() }})
        |${answer().prependIndent("    ")}
    """.trimMargin()

    private fun JsonRpcParam.read(): String = "params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position, lambda raw: ${reference.copy(isNullable = false).decode("raw")})"

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "return _Answer(${result.encodeResult("response")})"
        else ->
            """
            |if isinstance(response, $rpcName.Error):
            |    return _Answer(None, SERVER_ERROR, "Server error", ${error.encodeResult("response.value")})
            |return _Answer(${result.encodeResult("response.value")})
            """.trimMargin()
    }

    /** The JSON of a value that can be None, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "\"null\""
        isNullable -> "\"null\" if $value is None else ${encode(value)}"
        else -> encode(value)
    }

    private fun Reference.encode(value: String): String = when {
        isString() -> "_json.dumps($value)"
        else -> "self.serialization.serializeBody($value, ${valueType().python()}).decode(\"utf-8\")"
    }

    private fun Reference.decode(raw: String): String = when {
        isString() -> "_decode_string($raw)"
        else -> "self.serialization.deserializeBody($raw.encode(\"utf-8\"), ${valueType().python()})"
    }

    private fun JsonRpcParam.identifier(): String = name.camelCase()

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A type as the Python emitter writes it, which is also how the serialization is told the type. */
    private fun Type.python(): String = when (this) {
        is Type.Integer -> "int"
        is Type.Number -> "float"
        Type.Any -> "Any"
        Type.String -> "str"
        Type.Boolean -> "bool"
        Type.Bytes -> "bytes"
        Type.Unit -> "None"
        Type.Wildcard -> "Any"
        Type.Reflect -> "type"
        is Type.Array -> "list[${elementType.python()}]"
        is Type.Dict -> "dict[${keyType.python()}, ${valueType.python()}]"
        is Type.Custom -> name.referenceName() + generics.takeIf { it.isNotEmpty() }?.joinToString(", ", "[", "]") { it.python() }.orEmpty()
        is Type.Nullable -> "Optional[${type.python()}]"
        is Type.IntegerLiteral -> "int"
        is Type.StringLiteral -> "str"
        is Type.Function -> "Callable[[${parameterTypes.joinToString(", ") { it.python() }}], ${returnType.python()}]"
    }

    private val RUNTIME = """
        |PARSE_ERROR = -32700
        |INVALID_REQUEST = -32600
        |METHOD_NOT_FOUND = -32601
        |INVALID_PARAMS = -32602
        |INTERNAL_ERROR = -32603
        |# The code of the error of an rpc itself, which is the data of the error object.
        |SERVER_ERROR = -32000
        |
        |
        |class JsonRpcError(Exception):
        |    ${"\"\"\""}An error object a JSON-RPC server answered with, other than the error of the rpc itself.${"\"\"\""}
        |
        |    def __init__(self, code: int, message: str):
        |        super().__init__(message)
        |        self.code = code
        |        self.message = message
        |
        |
        |@dataclass
        |class _Answer:
        |    ${"\"\"\""}What a server answers a request with: the JSON of a result, or an error object.${"\"\"\""}
        |    result: Optional[str]
        |    code: int = 0
        |    message: str = ""
        |    data: Optional[str] = None
        |
        |
        |class _InvalidParams(Exception):
        |    pass
        |
        |
        |class _Params:
        |    ${"\"\"\""}The params of a request, each as the JSON of its value, by name or by position.${"\"\"\""}
        |
        |    def __init__(self, by_name: dict[str, str], by_position: list[str]):
        |        self.by_name = by_name
        |        self.by_position = by_position
        |
        |    def required(self, name: str, position: int, read: Callable[[str], Any]) -> Any:
        |        value = self.optional(name, position, read)
        |        if value is None:
        |            raise _InvalidParams()
        |        return value
        |
        |    def optional(self, name: str, position: int, read: Callable[[str], Any]) -> Any:
        |        raw = self.by_name.get(name) if name in self.by_name else (self.by_position[position] if position < len(self.by_position) else None)
        |        if raw is None or raw == "null":
        |            return None
        |        try:
        |            return read(raw)
        |        except Exception:
        |            raise _InvalidParams()
        |
        |
        |def _decode_string(raw: str) -> str:
        |    value = _json.loads(raw)
        |    if not isinstance(value, str):
        |        raise ValueError("No JSON string: " + raw)
        |    return value
        |
        |
        |def _parse(text: str) -> Any:
        |    try:
        |        return _json.loads(text)
        |    except ValueError:
        |        return None
        |
        |
        |async def _call(transportation: Wirespec.Transportation, method: str, params: list[Optional[tuple[str, str]]], on_result: Callable[[str], Any], on_error: Optional[Callable[[str], Any]]) -> Any:
        |    ${"\"\"\""}Every request has id 1: a transportation answers a request with its own response, so the id needs no counter.${"\"\"\""}
        |    members = [_json.dumps(name) + ":" + raw for name, raw in (param for param in params if param is not None)]
        |    request = '{"jsonrpc":"2.0","method":' + _json.dumps(method) + ("" if not members else ',"params":{' + ",".join(members) + "}") + ',"id":1}'
        |    response = await transportation.transport(Wirespec.RawRequest(method="POST", path=[], queries={}, headers={"Content-Type": ["application/json"]}, body=request.encode("utf-8")))
        |    answer = None if response.body is None else _parse(response.body.decode("utf-8"))
        |    if not isinstance(answer, dict):
        |        raise ValueError("The JSON-RPC response to " + method + " is no object, status " + str(response.statusCode))
        |    error = answer.get("error")
        |    if error is None:
        |        if "result" not in answer:
        |            raise ValueError("The JSON-RPC response to " + method + " has no result")
        |        return on_result(_json.dumps(answer["result"]))
        |    if not isinstance(error, dict):
        |        raise ValueError("The JSON-RPC error of " + method + " is no object")
        |    code = error.get("code") if isinstance(error.get("code"), int) else INTERNAL_ERROR
        |    if code == SERVER_ERROR and on_error is not None and "data" in error:
        |        return on_error(_json.dumps(error["data"]))
        |    message = error.get("message")
        |    raise JsonRpcError(code, message if isinstance(message, str) else "")
        |
        |
        |def _write(id: str, answer: _Answer) -> str:
        |    if answer.result is not None:
        |        member = '"result":' + answer.result
        |    else:
        |        member = '"error":{"code":' + str(answer.code) + ',"message":' + _json.dumps(answer.message) + ("" if answer.data is None else ',"data":' + answer.data) + "}"
        |    return '{"jsonrpc":"2.0",' + member + ',"id":' + id + "}"
        |
        |
        |def _response(answer: Optional[str]) -> Wirespec.RawResponse:
        |    if answer is None:
        |        return Wirespec.RawResponse(statusCode=204, headers={}, body=None)
        |    return Wirespec.RawResponse(statusCode=200, headers={"Content-Type": ["application/json"]}, body=answer.encode("utf-8"))
        |
        |
        |def _params(value: Any) -> Optional[_Params]:
        |    if isinstance(value, dict):
        |        return _Params({name: _json.dumps(item) for name, item in value.items()}, [])
        |    if isinstance(value, list):
        |        return _Params({}, [_json.dumps(item) for item in value])
        |    return None
    """.trimMargin()

    /** With `--sync` the server calls the services as plain functions, on the thread that handles the request. */
    private fun serverRuntime(isAsync: Boolean): String = (if (isAsync) "async " to "await " else "" to "").let { (async, await) ->
        """
        |${async}def _handle(body: Optional[bytes], dispatch: Callable[[str, _Params], Any]) -> Wirespec.RawResponse:
        |    try:
        |        message = _json.loads(body.decode("utf-8")) if body is not None else None
        |    except ValueError:
        |        return _response(_write("null", _Answer(None, PARSE_ERROR, "Parse error")))
        |    if message is None:
        |        return _response(_write("null", _Answer(None, PARSE_ERROR, "Parse error")))
        |    if not isinstance(message, list):
        |        return _response(${await}_reply(message, dispatch))
        |    if not message:
        |        return _response(_write("null", _Answer(None, INVALID_REQUEST, "Invalid Request")))
        |    answers = [answer for answer in [${await}_reply(element, dispatch) for element in message] if answer is not None]
        |    return _response("[" + ",".join(answers) + "]" if answers else None)
        |
        |
        |${async}def _reply(message: Any, dispatch: Callable[[str, _Params], Any]) -> Optional[str]:
        |    if not isinstance(message, dict):
        |        return _write("null", _Answer(None, INVALID_REQUEST, "Invalid Request"))
        |    id = _json.dumps(message["id"]) if "id" in message else None
        |    method = message.get("method")
        |    params = _params(message.get("params", {}))
        |    if message.get("jsonrpc") != "2.0" or not isinstance(method, str) or params is None:
        |        return _write("null" if id is None else id, _Answer(None, INVALID_REQUEST, "Invalid Request"))
        |    try:
        |        answer = ${await}dispatch(method, params)
        |        if answer is None:
        |            answer = _Answer(None, METHOD_NOT_FOUND, "Method not found")
        |    except _InvalidParams:
        |        answer = _Answer(None, INVALID_PARAMS, "Invalid params")
        |    except Exception:
        |        answer = _Answer(None, INTERNAL_ERROR, "Internal error")
        |    return None if id is None else _write(id, answer)
        """.trimMargin()
    }
}
