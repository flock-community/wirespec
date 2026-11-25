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
        |        ${error?.let { "($errorCode, lambda raw: $rpcName.Error(value=${it.decode("raw")}))" } ?: "None"},
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
            |    return _Answer(None, $errorCode, ${errorMessage.literal()}, ${error.encodeResult("response.value")})
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
        |# The codes of the errors JSON-RPC 2.0 defines, as the specification writes them: https://www.jsonrpc.org/specification
        |PARSE_ERROR = -32700
        |INVALID_REQUEST = -32600
        |METHOD_NOT_FOUND = -32601
        |INVALID_PARAMS = -32602
        |INTERNAL_ERROR = -32603
        |
        |
        |class JsonRpcError(Exception):
        |    ${"\"\"\""}An error object a JSON-RPC server answered with, other than the error of the rpc itself. Its data is JSON.${"\"\"\""}
        |
        |    def __init__(self, code: int, message: str, data: Optional[str]):
        |        super().__init__(message)
        |        self.code = code
        |        self.message = message
        |        self.data = data
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
        |    value = _parse(raw)
        |    if not isinstance(value, str):
        |        raise ValueError("No JSON string: " + raw)
        |    return value
        |
        |
        |def _reject(constant: str) -> Any:
        |    raise ValueError(constant + " is no JSON")
        |
        |
        |def _parse(text: str) -> Any:
        |    ${"\"\"\""}Reads JSON as RFC 8259 writes it: without NaN and Infinity, which the json module reads by default.${"\"\"\""}
        |    return _json.loads(text, parse_constant=_reject)
        |
        |
        |def _is_integer(value: Any) -> bool:
        |    return isinstance(value, int) and not isinstance(value, bool)
        |
        |
        |def _is_id(value: Any) -> bool:
        |    ${"\"\"\""}An id is a String, a Number or Null.${"\"\"\""}
        |    return value is None or isinstance(value, (str, float)) or _is_integer(value)
        |
        |
        |def _response(text: str) -> Optional[dict[str, Any]]:
        |    ${"\"\"\""}A response to a request with id 1: it has either a result or an error object with an integer code and a message, and the id of the request, or null when the server could not read it.${"\"\"\""}
        |    try:
        |        answer = _parse(text)
        |    except (ValueError, RecursionError):
        |        return None
        |    if not isinstance(answer, dict) or answer.get("jsonrpc") != "2.0" or ("result" in answer) == ("error" in answer):
        |        return None
        |    error = answer.get("error")
        |    if "error" in answer and not (isinstance(error, dict) and _is_integer(error.get("code")) and isinstance(error.get("message"), str)):
        |        return None
        |    if not ((_is_integer(answer.get("id")) and answer.get("id") == 1) or (answer.get("id") is None and "id" in answer and "error" in answer)):
        |        return None
        |    return answer
        |
        |
        |async def _call(transportation: Wirespec.Transportation, method: str, params: list[Optional[tuple[str, str]]], on_result: Callable[[str], Any], on_error: Optional[tuple[int, Callable[[str], Any]]]) -> Any:
        |    ${"\"\"\""}Every request has id 1: a transportation answers a request with its own response, so the id needs no counter. An error object with the code of on_error is the error of the rpc, which on_error reads from its data.${"\"\"\""}
        |    members = [_json.dumps(name) + ":" + raw for name, raw in (param for param in params if param is not None)]
        |    request = '{"jsonrpc":"2.0","method":' + _json.dumps(method) + ("" if not members else ',"params":{' + ",".join(members) + "}") + ',"id":1}'
        |    response = await transportation.transport(Wirespec.RawRequest(method="POST", path=[], queries={}, headers={"Content-Type": ["application/json"]}, body=request.encode("utf-8")))
        |    answer = None if response.body is None else _response(response.body.decode("utf-8", errors="replace"))
        |    if answer is None:
        |        raise ValueError("The answer to " + method + " is no JSON-RPC 2.0 response, status " + str(response.statusCode))
        |    if "error" not in answer:
        |        return on_result(_json.dumps(answer["result"]))
        |    error = answer["error"]
        |    data = _json.dumps(error["data"]) if "data" in error else None
        |    if on_error is not None and data is not None and error["code"] == on_error[0]:
        |        return on_error[1](data)
        |    raise JsonRpcError(error["code"], error["message"], data)
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
        |def _http(answer: Optional[str]) -> Wirespec.RawResponse:
        |    if answer is None:
        |        return Wirespec.RawResponse(statusCode=204, headers={}, body=None)
        |    return Wirespec.RawResponse(statusCode=200, headers={"Content-Type": ["application/json"]}, body=answer.encode("utf-8"))
        |
        |
        |def _params(value: Any) -> Optional[_Params]:
        |    ${"\"\"\""}Params are an Object, by name, or an Array, by position; a request may leave them out.${"\"\"\""}
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
        |    ${"\"\"\""}A batch is answered with the answers to its requests; a notification is not answered, and nor is a batch of them.${"\"\"\""}
        |    try:
        |        message = _parse(body.decode("utf-8") if body is not None else "")
        |    except (ValueError, RecursionError):
        |        return _http(_write("null", _Answer(None, PARSE_ERROR, "Parse error")))
        |    if not isinstance(message, list):
        |        return _http(${await}_reply(message, dispatch))
        |    if not message:
        |        return _http(_write("null", _Answer(None, INVALID_REQUEST, "Invalid Request")))
        |    answers = [answer for answer in [${await}_reply(element, dispatch) for element in message] if answer is not None]
        |    return _http("[" + ",".join(answers) + "]" if answers else None)
        |
        |
        |${async}def _reply(message: Any, dispatch: Callable[[str, _Params], Any]) -> Optional[str]:
        |    ${"\"\"\""}The answer to one request, or None for a notification. A request that is invalid is answered with its id, or with null when it has none, or one that is no String, Number or Null.${"\"\"\""}
        |    if not isinstance(message, dict):
        |        return _write("null", _Answer(None, INVALID_REQUEST, "Invalid Request"))
        |    has_id = "id" in message and _is_id(message["id"])
        |    id = _json.dumps(message["id"]) if has_id else None
        |    method = message.get("method")
        |    params = _params(message.get("params", {}))
        |    if message.get("jsonrpc") != "2.0" or not isinstance(method, str) or params is None or ("id" in message and not has_id):
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
