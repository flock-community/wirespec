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
        |  onError = ${error?.let { "$errorCode to { json: String -> $rpcName.Error(value = ${it.decode("json")}) }" } ?: "null"},
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
            |  is $rpcName.Error -> JsonRpc.Answer.Failure($errorCode, ${errorMessage.literal()}, ${error.encodeResult("response.value")})
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
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. Its [data] is JSON. */
        |public class JsonRpcException(public val code: Int, message: String, public val data: String?) : RuntimeException(message)
        |
        |/**
        | * The codes of the errors JSON-RPC 2.0 defines, and the reading and writing of its messages, as the specification
        | * writes them: https://www.jsonrpc.org/specification
        | */
        |public object JsonRpc {
        |  public const val PARSE_ERROR: Int = -32700
        |  public const val INVALID_REQUEST: Int = -32600
        |  public const val METHOD_NOT_FOUND: Int = -32601
        |  public const val INVALID_PARAMS: Int = -32602
        |  public const val INTERNAL_ERROR: Int = -32603
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
        |  /**
        |   * Every request has id 1: a transportation answers a request with its own response, so the id needs no counter.
        |   * An error object with the code of [onError] is the error of the rpc, which [onError] reads from its data.
        |   */
        |  internal suspend fun <T> call(
        |    transportation: Wirespec.Transportation,
        |    method: String,
        |    params: List<Pair<String, String>>,
        |    onResult: (String) -> T,
        |    onError: Pair<Int, (String) -> T>?,
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
        |    val members = response.body?.decodeToString()?.let(::response)
        |      ?: error("The answer to " + method + " is no JSON-RPC 2.0 response, status " + response.statusCode)
        |    val failure = members["error"]?.let(::members)
        |    val data = failure?.get("data")
        |    return when {
        |      failure == null -> onResult(members.getValue("result"))
        |      onError != null && data != null && failure.getValue("code").toInt() == onError.first -> onError.second(data)
        |      else -> throw JsonRpcException(failure.getValue("code").toInt(), decodeString(failure.getValue("message")), data)
        |    }
        |  }
        |
        |  /**
        |   * The members of [json] when it is a response to a request with id 1: it has either a result or an error object with
        |   * an integer code and a message, and the id of the request, or null when the server could not read it.
        |   */
        |  internal fun response(json: String): Map<String, String>? = json
        |    .takeIf(::isJson)
        |    ?.let(::members)
        |    ?.takeIf { members ->
        |      val error = members["error"]
        |      val failure = error?.let(::members)
        |      members["jsonrpc"]?.let(::stringOrNull) == "2.0" &&
        |        (members["result"] == null) != (error == null) &&
        |        (error == null || (failure?.get("code")?.toIntOrNull() != null && failure["message"]?.let(::stringOrNull) != null)) &&
        |        (members["id"] == "1" || (members["id"] == "null" && error != null))
        |    }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered, and nor is a batch of them. */
        |  internal inline fun handle(body: ByteArray?, dispatch: (String, Params) -> Answer?): Wirespec.RawResponse {
        |    val json = body?.decodeToString()?.trim().orEmpty()
        |    val valid = isJson(json)
        |    val batch = if (valid) parts(json, '[', ']') else null
        |    val answer = when {
        |      !valid -> write("null", Answer.Failure(PARSE_ERROR, "Parse error", null))
        |      batch == null -> reply(json, dispatch)
        |      batch.isEmpty() -> write("null", Answer.Failure(INVALID_REQUEST, "Invalid Request", null))
        |      else -> batch.mapNotNull { reply(it, dispatch) }.takeIf { it.isNotEmpty() }?.joinToString(",", "[", "]")
        |    }
        |    return when (answer) {
        |      null -> Wirespec.RawResponse(statusCode = 204, headers = emptyMap(), body = null)
        |      else -> Wirespec.RawResponse(statusCode = 200, headers = mapOf("Content-Type" to listOf("application/json")), body = answer.encodeToByteArray())
        |    }
        |  }
        |
        |  /**
        |   * The answer to the request [json], or null for a notification. A request that is invalid is answered with its id,
        |   * or with null when it has none, or one that is no String, Number or Null.
        |   */
        |  internal inline fun reply(json: String, dispatch: (String, Params) -> Answer?): String? {
        |    val members = members(json) ?: return write("null", Answer.Failure(INVALID_REQUEST, "Invalid Request", null))
        |    val id = members["id"]
        |    val method = members["method"]?.let(::stringOrNull)
        |    val params = params(members["params"])
        |    if (members["jsonrpc"]?.let(::stringOrNull) != "2.0" || method == null || params == null || (id != null && !isId(id))) {
        |      return write(id?.takeIf(::isId) ?: "null", Answer.Failure(INVALID_REQUEST, "Invalid Request", null))
        |    }
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
        |  /** Params are an Object, by name, or an Array, by position; a request may leave them out. */
        |  internal fun params(json: String?): Params? = when {
        |    json == null -> Params(emptyMap(), emptyList())
        |    json.startsWith("{") -> members(json)?.let { Params(it, emptyList()) }
        |    json.startsWith("[") -> parts(json, '[', ']')?.let { Params(emptyMap(), it) }
        |    else -> null
        |  }
        |
        |  /** An id is a String, a Number or Null. */
        |  internal fun isId(json: String): Boolean = json == "null" || json.first() == '"' || json.first() == '-' || json.first() in '0'..'9'
        |
        |  internal fun write(id: String, answer: Answer): String = "{\"jsonrpc\":\"2.0\"," +
        |    when (answer) {
        |      is Answer.Result -> "\"result\":" + answer.json
        |      is Answer.Failure -> "\"error\":{\"code\":" + answer.code + ",\"message\":" + encode(answer.message) + (answer.data?.let { ",\"data\":" + it } ?: "") + "}"
        |    } +
        |    ",\"id\":" + id + "}"
        |
        |  /** The members of the JSON object [json], each as the JSON of its value; null when it is no object. */
        |  internal fun members(json: String): Map<String, String>? = parts(json.trim(), '{', '}')?.associate { member ->
        |    val end = stringEnd(member, 0)
        |    decodeString(member.substring(0, end)) to member.substring(end).trimStart().removePrefix(":").trim()
        |  }
        |
        |  /** The values of the JSON [json] between [open] and [close], split at the commas outside of a nested value. */
        |  internal fun parts(json: String, open: Char, close: Char): List<String>? {
        |    if (json.length < 2 || json.first() != open || json.last() != close) return null
        |    val parts = mutableListOf<String>()
        |    var depth = 0
        |    var start = 1
        |    var index = 1
        |    while (index < json.length - 1) {
        |      when (json[index]) {
        |        '"' -> index = stringEnd(json, index) - 1
        |        '{', '[' -> depth++
        |        '}', ']' -> depth--
        |        ',' -> if (depth == 0) {
        |          parts.add(json.substring(start, index).trim())
        |          start = index + 1
        |        }
        |      }
        |      index++
        |    }
        |    return json.substring(start, json.length - 1).trim().let { last -> if (parts.isEmpty() && last.isEmpty()) parts else parts + last }
        |  }
        |
        |  /** Whether [json] is one JSON value, as RFC 8259 writes it. It reads nested values with a stack, so deep JSON cannot overflow. */
        |  internal fun isJson(json: String): Boolean {
        |    val closing = mutableListOf<Char>()
        |    var index = 0
        |    // A value comes next, the name of a member, or a comma or the end of an object or array.
        |    var next = 'v'
        |    while (true) {
        |      index = whitespaceEnd(json, index)
        |      if (index == json.length) return next == ',' && closing.isEmpty()
        |      val char = json[index]
        |      when {
        |        next == 'v' && (char == '{' || char == '[') -> {
        |          closing.add(if (char == '{') '}' else ']')
        |          index = whitespaceEnd(json, index + 1)
        |          next = when {
        |            json.getOrNull(index) == closing.last() -> ','.also { closing.removeAt(closing.lastIndex); index++ }
        |            char == '{' -> 'k'
        |            else -> 'v'
        |          }
        |        }
        |        next == 'v' -> {
        |          index = scalarEnd(json, index)
        |          if (index < 0) return false
        |          next = ','
        |        }
        |        next == 'k' -> {
        |          val end = stringEnd(json, index)
        |          if (end < 0) return false
        |          index = whitespaceEnd(json, end)
        |          if (json.getOrNull(index) != ':') return false
        |          index++
        |          next = 'v'
        |        }
        |        closing.isEmpty() -> return false
        |        char == ',' -> {
        |          index++
        |          next = if (closing.last() == '}') 'k' else 'v'
        |        }
        |        char == closing.last() -> {
        |          closing.removeAt(closing.lastIndex)
        |          index++
        |        }
        |        else -> return false
        |      }
        |    }
        |  }
        |
        |  private fun scalarEnd(json: String, start: Int): Int = when {
        |    json[start] == '"' -> stringEnd(json, start)
        |    json.startsWith("true", start) || json.startsWith("null", start) -> start + 4
        |    json.startsWith("false", start) -> start + 5
        |    else -> numberEnd(json, start)
        |  }
        |
        |  /** The index after the number at [start], or -1 when there is none: a minus, an integer without leading zeros, a fraction and an exponent. */
        |  private fun numberEnd(json: String, start: Int): Int {
        |    var index = if (json.getOrNull(start) == '-') start + 1 else start
        |    val first = json.getOrNull(index) ?: return -1
        |    index = when (first) {
        |      '0' -> index + 1
        |      in '1'..'9' -> digitsEnd(json, index)
        |      else -> return -1
        |    }
        |    if (json.getOrNull(index) == '.') {
        |      val digits = index + 1
        |      index = digitsEnd(json, digits)
        |      if (index == digits) return -1
        |    }
        |    if (json.getOrNull(index) == 'e' || json.getOrNull(index) == 'E') {
        |      val digits = if (json.getOrNull(index + 1) == '+' || json.getOrNull(index + 1) == '-') index + 2 else index + 1
        |      index = digitsEnd(json, digits)
        |      if (index == digits) return -1
        |    }
        |    return index
        |  }
        |
        |  private fun digitsEnd(json: String, start: Int): Int {
        |    var index = start
        |    while (index < json.length && json[index] in '0'..'9') index++
        |    return index
        |  }
        |
        |  private fun whitespaceEnd(json: String, start: Int): Int {
        |    var index = start
        |    while (index < json.length && (json[index] == ' ' || json[index] == '\t' || json[index] == '\n' || json[index] == '\r')) index++
        |    return index
        |  }
        |
        |  /** The index after the JSON string at [start], or -1 when there is none: a control character or an unknown escape ends none. */
        |  internal fun stringEnd(json: String, start: Int): Int {
        |    if (json.getOrNull(start) != '"') return -1
        |    var index = start + 1
        |    while (index < json.length) {
        |      val char = json[index]
        |      when {
        |        char == '"' -> return index + 1
        |        char < ' ' -> return -1
        |        char != '\\' -> index++
        |        else -> when (json.getOrNull(index + 1)) {
        |          '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> index += 2
        |          'u' -> if (index + 6 <= json.length && json.substring(index + 2, index + 6).all { it in "0123456789abcdefABCDEF" }) index += 6 else return -1
        |          else -> return -1
        |        }
        |      }
        |    }
        |    return -1
        |  }
        |
        |  internal fun stringOrNull(json: String): String? = runCatching { decodeString(json) }.getOrNull()
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
        |    require(stringEnd(json, 0) == json.length) { "No JSON string: " + json }
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
