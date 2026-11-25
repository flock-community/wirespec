package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * The `kind` of a response tells the `Result` of an rpc from its `Error`, so the server answers an `Error` with an
 * error object, and the client returns an error object of the rpc as an `Error`.
 *
 * The client and the server always return a promise; with `--sync` a service may return the value itself. The file
 * reads and writes the bytes of a message with `TextEncoder` and `TextDecoder`, which every runtime has.
 */
internal object TypeScriptJsonRpc : JsonRpcDialect {

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir + LanguageFile(
        Name.of("JsonRpc"),
        listOf(RawElement(listOf(imports(methods), RUNTIME, client(methods), server(methods), SCANNER).joinToString("\n\n", postfix = "\n"))),
    )

    private fun imports(methods: List<JsonRpcMethod>): String = (
        listOf("import {Wirespec} from './Wirespec'") +
            methods.flatMap { it.models }.distinct().map { "import {type ${Name.of(it).pascalCase()}} from './model/${Name.of(it).pascalCase()}'" } +
            methods.map { "import {${it.rpcName}} from './rpc/${it.rpcName}'" }
        ).joinToString("\n")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |/** Sends every rpc as a JSON-RPC 2.0 request through the transportation, and reads the values with the serialization. */
        |export class JsonRpcClient${methods.filter { it.isAsync }.joinToString(", ") { "${it.rpcName}.Service" }.let { if (it.isEmpty()) "" else " implements $it" }} {
        |  constructor(readonly serialization: Wirespec.Serialization, readonly transportation: Wirespec.Transportation) {}
        |
        |${methods.joinToString("\n\n") { it.clientFunction() }.prependIndent("  ")}
        |}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |${service.name.camelCase()}(${params.joinToString(", ") { "${it.identifier()}: ${it.type.typeScript()}" }}): Promise<${returnType()}> {
        |  return call<${returnType()}>(
        |    this.transportation,
        |    ${name.literal()},
        |    [${params.joinToString(", ") { it.param() }}],
        |    (json) => ${resultValue("json")},
        |    ${error?.let { "[$errorCode, (json) => ({ kind: \"Error\", value: ${it.decode("json")} })]" } ?: "undefined"},
        |  )
        |}
    """.trimMargin()

    private fun JsonRpcParam.param(): String = when {
        isNullable -> "${identifier()} == null ? undefined : [${wireName.literal()}, ${reference.encode(identifier())}]"
        else -> "[${wireName.literal()}, ${reference.encode(identifier())}]"
    }

    private fun JsonRpcMethod.resultValue(json: String): String = when (error) {
        null -> result.decodeResult(json)
        else -> "({ kind: \"Result\", value: ${result.decodeResult(json)} })"
    }

    private fun Reference.decodeResult(json: String): String = when {
        this is Reference.Unit -> "undefined"
        isNullable -> "$json === \"null\" ? undefined : ${copy(isNullable = false).decode(json)}"
        else -> decode(json)
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName.Response" } ?: result.convert().typeScript()

    private fun server(methods: List<JsonRpcMethod>): String = """
        |/**
        | * Answers JSON-RPC 2.0 requests, single or batched, by calling the service, which implements the `Service` of every
        | * rpc, and writes the values with the serialization.
        | */
        |export class JsonRpcServer {
        |  constructor(readonly serialization: Wirespec.Serialization, readonly service: ${methods.joinToString(" & ") { "${it.rpcName}.Service" }}) {}
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered, so its response has status 204 and no body. */
        |  handle(request: Wirespec.RawRequest): Promise<Wirespec.RawResponse> {
        |    return handle(request.body, (method, params) => this.dispatch(method, params))
        |  }
        |
        |  private async dispatch(method: string, params: Params): Promise<Answer | undefined> {
        |    switch (method) {
        |${methods.joinToString("\n") { it.serverBranch() }.prependIndent("      ")}
        |      default:
        |        return undefined
        |    }
        |  }
        |}
    """.trimMargin()

    private fun JsonRpcMethod.serverBranch(): String = """
        |case ${name.literal()}: {
        |  const response = await this.service.${service.name.camelCase()}(${params.joinToString(", ") { it.read() }})
        |  return ${answer()}
        |}
    """.trimMargin()

    private fun JsonRpcParam.read(): String = "params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position, (json) => ${reference.copy(isNullable = false).decode("json")})"

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "result(${result.encodeResult("response")})"
        else -> "response.kind === \"Error\" ? failure($errorCode, ${errorMessage.literal()}, ${error.encodeResult("response.value")}) : result(${result.encodeResult("response.value")})"
    }

    /** The JSON of a value that can be undefined or void, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "\"null\""
        isNullable -> "$value == null ? \"null\" : ${encode(value)}"
        else -> encode(value)
    }

    private fun Reference.encode(value: String): String = when {
        isString() -> "encode($value)"
        else -> "text(this.serialization.serializeBody<${valueType().typeScript()}>($value, ${valueType().typeScript().literal()}))"
    }

    private fun Reference.decode(json: String): String = when {
        isString() -> "decodeString($json)"
        else -> "this.serialization.deserializeBody<${valueType().typeScript()}>(bytes($json), ${valueType().typeScript().literal()})"
    }

    private fun JsonRpcParam.identifier(): String = name.camelCase()

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A type as the TypeScript emitter writes it, which is also how the serialization is told the type. */
    private fun Type.typeScript(): String = when (this) {
        is Type.Integer, is Type.Number -> "number"
        Type.Any -> "any"
        Type.String -> "string"
        Type.Boolean -> "boolean"
        Type.Bytes -> "Uint8Array"
        Type.Unit -> "void"
        Type.Wildcard -> "unknown"
        Type.Reflect -> "Wirespec.Type"
        is Type.Array -> if (elementType is Type.Nullable) "(${elementType.typeScript()})[]" else "${elementType.typeScript()}[]"
        is Type.Dict -> "Record<${keyType.typeScript()}, ${valueType.typeScript()}>"
        is Type.Custom -> name.referenceName() + generics.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.typeScript() }.orEmpty()
        is Type.Nullable -> "${type.typeScript()} | undefined"
        is Type.IntegerLiteral -> value.toString()
        is Type.StringLiteral -> "\"$value\""
        is Type.Function -> "(${parameterTypes.mapIndexed { index, type -> "p$index: ${type.typeScript()}" }.joinToString(", ")}) => ${returnType.typeScript()}"
    }

    private val RUNTIME = """
        |/**
        | * The codes of the errors JSON-RPC 2.0 defines, as the specification writes them:
        | * https://www.jsonrpc.org/specification
        | */
        |export const JsonRpc = {
        |  PARSE_ERROR: -32700,
        |  INVALID_REQUEST: -32600,
        |  METHOD_NOT_FOUND: -32601,
        |  INVALID_PARAMS: -32602,
        |  INTERNAL_ERROR: -32603,
        |} as const
        |
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. Its data is JSON. */
        |export class JsonRpcError extends Error {
        |  constructor(readonly code: number, message: string, readonly data: string | undefined) {
        |    super(message)
        |  }
        |}
        |
        |/** What a server answers a request with: the JSON of a result, or an error object. */
        |type Answer = { result: string } | { code: number, message: string, data: string | undefined }
        |
        |const result = (json: string): Answer => ({ result: json })
        |
        |const failure = (code: number, message: string, data?: string): Answer => ({ code, message, data })
        |
        |class InvalidParams extends Error {}
        |
        |/** The params of a request, each as the JSON of its value, by name or by position. */
        |class Params {
        |  constructor(private readonly byName: Record<string, string>, private readonly byPosition: string[]) {}
        |
        |  required<T>(name: string, position: number, read: (json: string) => T): T {
        |    const value = this.optional(name, position, read)
        |    if (value === undefined) throw new InvalidParams()
        |    return value
        |  }
        |
        |  optional<T>(name: string, position: number, read: (json: string) => T): T | undefined {
        |    const json = Object.prototype.hasOwnProperty.call(this.byName, name) ? this.byName[name] : this.byPosition[position]
        |    if (json === undefined || json === "null") return undefined
        |    try {
        |      return read(json)
        |    } catch {
        |      throw new InvalidParams()
        |    }
        |  }
        |}
        |
        |const bytes = (json: string): Uint8Array => new TextEncoder().encode(json)
        |
        |const text = (bytes: Uint8Array): string => new TextDecoder().decode(bytes)
        |
        |/**
        | * Every request has id 1: a transportation answers a request with its own response, so the id needs no counter.
        | * An error object with the code of onError is the error of the rpc, which onError reads from its data.
        | */
        |async function call<T>(
        |  transportation: Wirespec.Transportation,
        |  method: string,
        |  params: ([string, string] | undefined)[],
        |  onResult: (json: string) => T,
        |  onError: [number, (json: string) => T] | undefined,
        |): Promise<T> {
        |  const members = params.filter((param): param is [string, string] => param !== undefined).map(([name, json]) => encode(name) + ":" + json)
        |  const request = "{\"jsonrpc\":\"2.0\",\"method\":" + encode(method) + (members.length === 0 ? "" : ",\"params\":{" + members.join(",") + "}") + ",\"id\":1}"
        |  const response = await transportation.transport({
        |    method: "POST",
        |    path: [],
        |    queries: {},
        |    headers: { "Content-Type": ["application/json"] },
        |    body: bytes(request),
        |  })
        |  const answer = response.body === undefined ? undefined : responseOf(text(response.body))
        |  if (answer === undefined) throw new Error("The answer to " + method + " is no JSON-RPC 2.0 response, status " + response.statusCode)
        |  const failure = answer["error"] === undefined ? undefined : membersOf(answer["error"])
        |  if (failure === undefined) return onResult(answer["result"])
        |  const code = Number(failure["code"])
        |  const data = failure["data"]
        |  if (onError !== undefined && data !== undefined && code === onError[0]) return onError[1](data)
        |  throw new JsonRpcError(code, decodeString(failure["message"]), data)
        |}
        |
        |/**
        | * The members of a response to a request with id 1: it has either a result or an error object with an integer code
        | * and a message, and the id of the request, or null when the server could not read it. Undefined for any other JSON.
        | */
        |function responseOf(json: string): Record<string, string> | undefined {
        |  const members = isJson(json) ? membersOf(json) : undefined
        |  if (members === undefined || stringOrUndefined(members["jsonrpc"]) !== "2.0" || (members["result"] === undefined) === (members["error"] === undefined)) return undefined
        |  const failure = members["error"] === undefined ? undefined : membersOf(members["error"])
        |  const validError = members["error"] === undefined || (failure !== undefined && /^-?(0|[1-9][0-9]*)$/.test(failure["code"] ?? "") && stringOrUndefined(failure["message"]) !== undefined)
        |  const validId = members["id"] === "1" || (members["id"] === "null" && members["error"] !== undefined)
        |  return validError && validId ? members : undefined
        |}
        |
        |/** A batch is answered with the answers to its requests; a notification is not answered, and nor is a batch of them. */
        |async function handle(body: Uint8Array | undefined, dispatch: (method: string, params: Params) => Promise<Answer | undefined>): Promise<Wirespec.RawResponse> {
        |  const json = body === undefined ? "" : text(body).trim()
        |  const batch = isJson(json) ? parts(json, "[", "]") : undefined
        |  let answer: string | undefined
        |  if (!isJson(json)) {
        |    answer = write("null", failure(JsonRpc.PARSE_ERROR, "Parse error"))
        |  } else if (batch === undefined) {
        |    answer = await reply(json, dispatch)
        |  } else if (batch.length === 0) {
        |    answer = write("null", failure(JsonRpc.INVALID_REQUEST, "Invalid Request"))
        |  } else {
        |    const answered = (await Promise.all(batch.map((it) => reply(it, dispatch)))).filter((it): it is string => it !== undefined)
        |    answer = answered.length === 0 ? undefined : "[" + answered.join(",") + "]"
        |  }
        |  return answer === undefined
        |    ? { statusCode: 204, headers: {}, body: undefined }
        |    : { statusCode: 200, headers: { "Content-Type": ["application/json"] }, body: bytes(answer) }
        |}
        |
        |/**
        | * The answer to one request, or undefined for a notification. A request that is invalid is answered with its id, or
        | * with null when it has none, or one that is no String, Number or Null.
        | */
        |async function reply(json: string, dispatch: (method: string, params: Params) => Promise<Answer | undefined>): Promise<string | undefined> {
        |  const members = membersOf(json)
        |  if (members === undefined) return write("null", failure(JsonRpc.INVALID_REQUEST, "Invalid Request"))
        |  const id = members["id"]
        |  const method = stringOrUndefined(members["method"])
        |  const params = paramsOf(members["params"])
        |  if (stringOrUndefined(members["jsonrpc"]) !== "2.0" || method === undefined || params === undefined || (id !== undefined && !isId(id))) {
        |    return write(id !== undefined && isId(id) ? id : "null", failure(JsonRpc.INVALID_REQUEST, "Invalid Request"))
        |  }
        |  let answer: Answer
        |  try {
        |    answer = (await dispatch(method, params)) ?? failure(JsonRpc.METHOD_NOT_FOUND, "Method not found")
        |  } catch (error) {
        |    answer = error instanceof InvalidParams ? failure(JsonRpc.INVALID_PARAMS, "Invalid params") : failure(JsonRpc.INTERNAL_ERROR, "Internal error")
        |  }
        |  return id === undefined ? undefined : write(id, answer)
        |}
        |
        |/** Params are an Object, by name, or an Array, by position; a request may leave them out. */
        |function paramsOf(json: string | undefined): Params | undefined {
        |  if (json === undefined) return new Params({}, [])
        |  if (json.startsWith("{")) return new Params(membersOf(json) ?? {}, [])
        |  if (json.startsWith("[")) return new Params({}, parts(json, "[", "]") ?? [])
        |  return undefined
        |}
        |
        |/** An id is a String, a Number or Null. */
        |function isId(json: string): boolean {
        |  return json === "null" || json[0] === "\"" || json[0] === "-" || (json[0] >= "0" && json[0] <= "9")
        |}
        |
        |function write(id: string, answer: Answer): string {
        |  const member = "result" in answer
        |    ? "\"result\":" + answer.result
        |    : "\"error\":{\"code\":" + answer.code + ",\"message\":" + encode(answer.message) + (answer.data === undefined ? "" : ",\"data\":" + answer.data) + "}"
        |  return "{\"jsonrpc\":\"2.0\"," + member + ",\"id\":" + id + "}"
        |}
    """.trimMargin()

    private val SCANNER = """
        |/** Whether the JSON is one JSON value: JSON.parse reads the grammar of RFC 8259, and fails on JSON too deep to read. */
        |function isJson(json: string): boolean {
        |  try {
        |    JSON.parse(json)
        |    return true
        |  } catch {
        |    return false
        |  }
        |}
        |
        |/** The members of a JSON object, each as the JSON of its value; undefined when the JSON is no object. */
        |function membersOf(json: string): Record<string, string> | undefined {
        |  const members = parts(json.trim(), "{", "}")
        |  if (members === undefined) return undefined
        |  const object: Record<string, string> = {}
        |  for (const member of members) {
        |    const end = stringEnd(member)
        |    object[decodeString(member.substring(0, end))] = member.substring(end).trim().substring(1).trim()
        |  }
        |  return object
        |}
        |
        |/** The values of the JSON between open and close, split at the commas outside of a nested value. */
        |function parts(json: string, open: string, close: string): string[] | undefined {
        |  if (json.length < 2 || json[0] !== open || json[json.length - 1] !== close) return undefined
        |  const parts: string[] = []
        |  let depth = 0
        |  let start = 1
        |  for (let index = 1; index < json.length - 1; index++) {
        |    const c = json[index]
        |    if (c === "\"") index = stringEnd(json, index) - 1
        |    else if (c === "{" || c === "[") depth++
        |    else if (c === "}" || c === "]") depth--
        |    else if (c === "," && depth === 0) {
        |      parts.push(json.substring(start, index).trim())
        |      start = index + 1
        |    }
        |  }
        |  const last = json.substring(start, json.length - 1).trim()
        |  if (parts.length > 0 || last !== "") parts.push(last)
        |  return parts
        |}
        |
        |/** The index after the JSON string at start, which is valid JSON; -1 when there is none. */
        |function stringEnd(json: string, start: number = 0): number {
        |  if (json[start] !== "\"") return -1
        |  for (let index = start + 1; index < json.length; index++) {
        |    if (json[index] === "\\") index++
        |    else if (json[index] === "\"") return index + 1
        |  }
        |  return -1
        |}
        |
        |export function encode(value: string): string {
        |  return JSON.stringify(value)
        |}
        |
        |export function decodeString(json: string): string {
        |  const value: unknown = JSON.parse(json)
        |  if (typeof value !== "string") throw new Error("No JSON string: " + json)
        |  return value
        |}
        |
        |function stringOrUndefined(json: string | undefined): string | undefined {
        |  try {
        |    return json === undefined ? undefined : decodeString(json)
        |  } catch {
        |    return undefined
        |  }
        |}
    """.trimMargin()
}
