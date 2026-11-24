package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.KotlinGenerator
import community.flock.wirespec.compiler.core.ir.generator.escapeKotlinIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * Kotlin sends through a `suspend` transportation, so the client implements the `Service` of an rpc only when that is
 * `suspend` too: with `--sync` it has the same functions, without implementing it. The server's `handle` is `suspend`
 * when the services are, and the runtime functions it calls are `inline`, so a synchronous service runs on the thread
 * that handles the request.
 */
internal object KotlinJsonRpc : JsonRpcDialect {

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir + LanguageFile(
        Name.of(packageName.toDir() + "JsonRpc"),
        listOf(RawElement(source(packageName, methods))),
    )

    private fun source(packageName: PackageName, methods: List<JsonRpcMethod>): String = listOf(
        header(packageName, methods),
        client(methods),
        server(methods),
        RUNTIME,
    ).joinToString("\n\n", postfix = "\n")

    private fun header(packageName: PackageName, methods: List<JsonRpcMethod>): String = (
        listOf("package ${packageName.value}", "", "import community.flock.wirespec.kotlin.Wirespec", "import kotlin.coroutines.cancellation.CancellationException", "import kotlin.reflect.typeOf") +
            methods.flatMap { it.models }.distinct().map { "import ${packageName.value}.model.${Name.of(it).pascalCase()}" } +
            methods.map { "import ${packageName.value}.rpc.${it.rpcName}" }
        ).joinToString("\n")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |/** Sends every rpc as a JSON-RPC 2.0 request through the [transportation], and reads the values with the [serialization]. */
        |public data class JsonRpcClient(
        |  val serialization: Wirespec.Serialization,
        |  val transportation: Wirespec.Transportation,
        |)${methods.filter { it.isAsync }.joinToString(", ") { "${it.rpcName}.Service" }.let { if (it.isEmpty()) "" else " : $it" }} {
        |${methods.joinToString("\n") { it.clientFunction() }.prependIndent("  ")}
        |}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |${if (isAsync) "override " else "public "}suspend fun ${service.name.camelCase().escapeKotlinIdentifier()}(${params.joinToString(", ") { "${it.identifier()}: ${KotlinGenerator.generateType(it.type)}" }}): ${returnType()} = JsonRpc.call<${returnType()}>(
        |  transportation = transportation,
        |  method = ${name.literal()},
        |  params = ${params.paramsList()},
        |  onResult = { json -> ${resultValue("json")} },
        |  onError = ${error?.let { "{ json -> $rpcName.Error(value = ${it.decode("json")}) }" } ?: "null"},
        |)
    """.trimMargin()

    private fun List<JsonRpcParam>.paramsList(): String = if (isEmpty()) {
        "emptyList()"
    } else {
        joinToString(",\n", "listOfNotNull(\n", ",\n  )") { param ->
            when {
                param.isNullable -> "${param.identifier()}?.let { ${param.wireName.literal()} to ${param.reference.encode("it")} }"
                else -> "${param.wireName.literal()} to ${param.reference.encode(param.identifier())}"
            }.prependIndent("    ")
        }
    }

    private fun JsonRpcMethod.resultValue(json: String): String = when (error) {
        null -> result.decode(json)
        else -> "$rpcName.Result(value = ${result.decode(json)})"
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName.Response" } ?: KotlinGenerator.generateType(result.convert())

    private fun server(methods: List<JsonRpcMethod>): String = (if (methods.any { it.isAsync }) "suspend " else "").let { suspend ->
        """
        |/**
        | * Answers JSON-RPC 2.0 requests, single or batched, by calling the [service], and writes the values with the
        | * [serialization]. The [service] implements the `Service` of every rpc.
        | */
        |public class JsonRpcServer<S>(
        |  private val serialization: Wirespec.Serialization,
        |  private val service: S,
        |) where ${methods.joinToString(", ") { "S : ${it.rpcName}.Service" }} {
        |  public ${suspend}fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse = JsonRpc.handle(request.body) { method, params ->
        |    when (method) {
        |${methods.joinToString("\n") { it.serverBranch() }.prependIndent("      ")}
        |      else -> null
        |    }
        |  }
        |}
        """.trimMargin()
    }

    private fun JsonRpcMethod.serverBranch(): String = """
        |${name.literal()} -> service.${service.name.camelCase().escapeKotlinIdentifier()}(${params.joinToString(", ") { it.read() }}).let { response ->
        |${answer().prependIndent("  ")}
        |}
    """.trimMargin()

    private fun JsonRpcParam.read(): String = "params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position) { json -> ${reference.copy(isNullable = false).decode("json")} }"

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "JsonRpc.Answer.Result(${result.encodeResult("response")})"
        else ->
            """
            |when (response) {
            |  is $rpcName.Result -> JsonRpc.Answer.Result(${result.encodeResult("response.value")})
            |  is $rpcName.Error -> JsonRpc.Answer.Failure(JsonRpc.SERVER_ERROR, "Server error", ${error.encodeResult("response.value")})
            |}
            """.trimMargin()
    }

    /** The JSON of a value that can be null or Unit, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "\"null\""
        isNullable -> "$value?.let { ${encode("it")} } ?: \"null\""
        else -> encode(value)
    }

    private fun Reference.encode(value: String): String = when {
        isString() -> "JsonRpc.encode($value)"
        else -> "serialization.serializeBody($value, typeOf<${KotlinGenerator.generateType(valueType())}>()).decodeToString()"
    }

    private fun Reference.decode(json: String): String = when {
        this is Reference.Unit -> "Unit"
        isNullable -> "$json.takeIf { it != \"null\" }?.let { ${copy(isNullable = false).decode("it")} }"
        isString() -> "JsonRpc.decodeString($json)"
        else -> "serialization.deserializeBody<${KotlinGenerator.generateType(valueType())}>($json.encodeToByteArray(), typeOf<${KotlinGenerator.generateType(valueType())}>())"
    }

    private fun JsonRpcParam.identifier(): String = name.value().escapeKotlinIdentifier()

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""

    private val RUNTIME = """
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. */
        |public class JsonRpcException(public val code: Int, message: String) : RuntimeException(message)
        |
        |/** The codes of the errors JSON-RPC 2.0 defines, and the reading and writing of its messages. */
        |public object JsonRpc {
        |  public const val PARSE_ERROR: Int = -32700
        |  public const val INVALID_REQUEST: Int = -32600
        |  public const val METHOD_NOT_FOUND: Int = -32601
        |  public const val INVALID_PARAMS: Int = -32602
        |  public const val INTERNAL_ERROR: Int = -32603
        |
        |  /** The code of the error of an rpc itself, which is the data of the error object. */
        |  public const val SERVER_ERROR: Int = -32000
        |
        |  /** What a server answers a request with: the JSON of a result, or an error object. */
        |  internal sealed interface Answer {
        |    class Result(val json: String) : Answer
        |    class Failure(val code: Int, val message: String, val data: String?) : Answer
        |  }
        |
        |  internal class InvalidParams : RuntimeException("Invalid params")
        |
        |  /** The params of a request, each as the JSON of its value, by name or by position. */
        |  internal class Params(private val byName: Map<String, String>, private val byPosition: List<String>) {
        |    fun <T : Any> required(name: String, position: Int, read: (String) -> T): T = optional(name, position, read) ?: throw InvalidParams()
        |
        |    fun <T : Any> optional(name: String, position: Int, read: (String) -> T): T? = (byName[name] ?: byPosition.getOrNull(position))
        |      ?.takeIf { it != "null" }
        |      ?.let { json -> try { read(json) } catch (e: Exception) { throw InvalidParams() } }
        |  }
        |
        |  /** Every request has id 1: a transportation answers a request with its own response, so the id needs no counter. */
        |  internal suspend fun <T> call(
        |    transportation: Wirespec.Transportation,
        |    method: String,
        |    params: List<Pair<String, String>>,
        |    onResult: (String) -> T,
        |    onError: ((String) -> T)?,
        |  ): T {
        |    val request = "{\"jsonrpc\":\"2.0\",\"method\":" + encode(method) +
        |      (if (params.isEmpty()) "" else params.joinToString(",", ",\"params\":{", "}") { encode(it.first) + ":" + it.second }) +
        |      ",\"id\":1}"
        |    val response = transportation.transport(
        |      Wirespec.RawRequest(
        |        method = "POST",
        |        path = emptyList(),
        |        queries = emptyMap(),
        |        headers = mapOf("Content-Type" to listOf("application/json")),
        |        body = request.encodeToByteArray(),
        |      ),
        |    )
        |    val members = response.body?.decodeToString()?.let(::members)
        |      ?: error("The JSON-RPC response to " + method + " is no object, status " + response.statusCode)
        |    val failure = members["error"]?.takeIf { it != "null" }
        |      ?.let { members(it) ?: error("The JSON-RPC error of " + method + " is no object") }
        |    val code = failure?.get("code")?.toIntOrNull() ?: INTERNAL_ERROR
        |    val data = failure?.get("data")
        |    return when {
        |      failure == null -> onResult(members["result"] ?: error("The JSON-RPC response to " + method + " has no result"))
        |      code == SERVER_ERROR && onError != null && data != null -> onError(data)
        |      else -> throw JsonRpcException(code, failure["message"]?.let { runCatching { decodeString(it) }.getOrNull() } ?: "")
        |    }
        |  }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered. */
        |  internal inline fun handle(body: ByteArray?, dispatch: (String, Params) -> Answer?): Wirespec.RawResponse {
        |    val json = body?.decodeToString()?.trim().orEmpty()
        |    val batch = if (json.startsWith("[")) parts(json, '[', ']') else null
        |    val answer = when {
        |      !json.startsWith("[") -> reply(json, dispatch)
        |      batch == null -> write("null", Answer.Failure(PARSE_ERROR, "Parse error", null))
        |      batch.isEmpty() -> write("null", Answer.Failure(INVALID_REQUEST, "Invalid Request", null))
        |      else -> batch.mapNotNull { reply(it, dispatch) }.takeIf { it.isNotEmpty() }?.joinToString(",", "[", "]")
        |    }
        |    return when (answer) {
        |      null -> Wirespec.RawResponse(statusCode = 204, headers = emptyMap(), body = null)
        |      else -> Wirespec.RawResponse(statusCode = 200, headers = mapOf("Content-Type" to listOf("application/json")), body = answer.encodeToByteArray())
        |    }
        |  }
        |
        |  internal inline fun reply(json: String, dispatch: (String, Params) -> Answer?): String? {
        |    val members = members(json)
        |      ?: return write("null", Answer.Failure(if (json.isEmpty() || json.startsWith("{")) PARSE_ERROR else INVALID_REQUEST, if (json.isEmpty() || json.startsWith("{")) "Parse error" else "Invalid Request", null))
        |    val id = members["id"]
        |    val method = members["method"]?.let { runCatching { decodeString(it) }.getOrNull() }
        |    val params = params(members["params"])
        |    if (members["jsonrpc"] != "\"2.0\"" || method == null || params == null) return write(id ?: "null", Answer.Failure(INVALID_REQUEST, "Invalid Request", null))
        |    val answer = try {
        |      dispatch(method, params) ?: Answer.Failure(METHOD_NOT_FOUND, "Method not found", null)
        |    } catch (e: InvalidParams) {
        |      Answer.Failure(INVALID_PARAMS, "Invalid params", null)
        |    } catch (e: CancellationException) {
        |      throw e
        |    } catch (e: Exception) {
        |      Answer.Failure(INTERNAL_ERROR, "Internal error", null)
        |    }
        |    return id?.let { write(it, answer) }
        |  }
        |
        |  internal fun params(json: String?): Params? = when {
        |    json == null -> Params(emptyMap(), emptyList())
        |    json.startsWith("{") -> members(json)?.let { Params(it, emptyList()) }
        |    json.startsWith("[") -> parts(json, '[', ']')?.let { Params(emptyMap(), it) }
        |    else -> null
        |  }
        |
        |  internal fun write(id: String, answer: Answer): String = "{\"jsonrpc\":\"2.0\"," +
        |    when (answer) {
        |      is Answer.Result -> "\"result\":" + answer.json
        |      is Answer.Failure -> "\"error\":{\"code\":" + answer.code + ",\"message\":" + encode(answer.message) + (answer.data?.let { ",\"data\":" + it } ?: "") + "}"
        |    } +
        |    ",\"id\":" + id + "}"
        |
        |  /** The members of a JSON object, each as the JSON of its value; null when [json] is no object. */
        |  internal fun members(json: String): Map<String, String>? {
        |    val members = parts(json.trim(), '{', '}') ?: return null
        |    return members.associate { member ->
        |      val end = stringEnd(member)
        |      val key = if (end < 0) null else runCatching { decodeString(member.substring(0, end)) }.getOrNull()
        |      val value = if (end < 0) "" else member.substring(end).trimStart()
        |      if (key == null || !value.startsWith(":")) return null
        |      key to value.substring(1).trim()
        |    }
        |  }
        |
        |  /** The JSON between [open] and [close] split at the commas outside of a nested value; null when it is no such JSON. */
        |  internal fun parts(json: String, open: Char, close: Char): List<String>? {
        |    if (json.length < 2 || json.first() != open || json.last() != close) return null
        |    val parts = mutableListOf<String>()
        |    var depth = 0
        |    var inString = false
        |    var escaped = false
        |    var start = 1
        |    for (index in 1 until json.length - 1) {
        |      val char = json[index]
        |      when {
        |        inString && escaped -> escaped = false
        |        inString && char == '\\' -> escaped = true
        |        inString && char == '"' -> inString = false
        |        inString -> Unit
        |        char == '"' -> inString = true
        |        char == '{' || char == '[' -> depth++
        |        char == '}' || char == ']' -> if (--depth < 0) return null
        |        char == ',' && depth == 0 -> {
        |          parts.add(json.substring(start, index).trim())
        |          start = index + 1
        |        }
        |      }
        |    }
        |    val last = json.substring(start, json.length - 1).trim()
        |    return when {
        |      inString || depth != 0 -> null
        |      parts.isEmpty() && last.isEmpty() -> emptyList()
        |      else -> (parts + last).takeIf { all -> all.none { it.isEmpty() } }
        |    }
        |  }
        |
        |  /** The index after the JSON string that [json] starts with, or -1 when it starts with none. */
        |  internal fun stringEnd(json: String): Int {
        |    if (!json.startsWith("\"")) return -1
        |    var escaped = false
        |    for (index in 1 until json.length) {
        |      when {
        |        escaped -> escaped = false
        |        json[index] == '\\' -> escaped = true
        |        json[index] == '"' -> return index + 1
        |      }
        |    }
        |    return -1
        |  }
        |
        |  public fun encode(value: String): String = buildString {
        |    append('"')
        |    value.forEach { char ->
        |      when {
        |        char == '"' -> append("\\\"")
        |        char == '\\' -> append("\\\\")
        |        char == '\n' -> append("\\n")
        |        char == '\r' -> append("\\r")
        |        char == '\t' -> append("\\t")
        |        char < ' ' -> append("\\u").append(char.code.toString(16).padStart(4, '0'))
        |        else -> append(char)
        |      }
        |    }
        |    append('"')
        |  }
        |
        |  public fun decodeString(json: String): String {
        |    require(stringEnd(json) == json.length) { "No JSON string: " + json }
        |    val value = StringBuilder()
        |    var index = 1
        |    while (index < json.length - 1) {
        |      val char = json[index++]
        |      if (char != '\\') {
        |        value.append(char)
        |      } else {
        |        when (val escape = json[index++]) {
        |          'b' -> value.append('\b')
        |          'f' -> value.append('\u000C')
        |          'n' -> value.append('\n')
        |          'r' -> value.append('\r')
        |          't' -> value.append('\t')
        |          'u' -> value.append(json.substring(index, index + 4).toInt(16).toChar()).also { index += 4 }
        |          else -> value.append(escape)
        |        }
        |      }
        |    }
        |    return value.toString()
        |  }
        |}
    """.trimMargin()
}
