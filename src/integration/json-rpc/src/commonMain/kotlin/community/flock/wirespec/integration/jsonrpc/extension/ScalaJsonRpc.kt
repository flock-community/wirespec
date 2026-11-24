package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.ScalaGenerator
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * The Scala `Service` and `Transportation` are synchronous, so the client implements the `Service` of every rpc and
 * the server answers on the thread that handles the request. The scanner loops instead of returning from a closure,
 * which Scala 3 no longer allows.
 */
internal object ScalaJsonRpc : JsonRpcDialect {

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir + LanguageFile(
        Name.of(packageName.toDir() + "JsonRpc"),
        listOf(RawElement(listOf(header(packageName, methods), client(methods), server(methods), runtime(packageName)).joinToString("\n\n", postfix = "\n"))),
    )

    private fun header(packageName: PackageName, methods: List<JsonRpcMethod>): String = (
        listOf("package ${packageName.value}", "", "import community.flock.wirespec.scala.Wirespec", "import java.nio.charset.StandardCharsets") +
            methods.flatMap { it.models }.distinct().map { "import ${packageName.value}.model.${Name.of(it).pascalCase()}" } +
            methods.map { "import ${packageName.value}.rpc.${it.rpcName}" }
        ).joinToString("\n")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |/** Sends every rpc as a JSON-RPC 2.0 request through the transportation, and reads the values with the serialization. */
        |case class JsonRpcClient(serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) extends ${methods.joinToString(" with ") { "${it.rpcName}.Service" }} {
        |${methods.joinToString("\n") { it.clientFunction() }.prependIndent("  ")}
        |}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |override def ${service.name.camelCase().sanitize()}(${params.joinToString(", ") { "${it.identifier()}: ${it.type.scala()}" }}): ${returnType()} = JsonRpc.call[${returnType()}](
        |  transportation,
        |  ${name.literal()},
        |  ${params.paramsList()},
        |  json => ${resultValue("json")},
        |  ${error?.let { "Some(json => $rpcName.Error(${it.decode("json")}))" } ?: "None"}
        |)
    """.trimMargin()

    private fun List<JsonRpcParam>.paramsList(): String = if (isEmpty()) {
        "Nil"
    } else {
        joinToString(",\n", "List(\n", "\n  ).flatten") { param ->
            when {
                param.isNullable -> "${param.identifier()}.map(it => ${param.wireName.literal()} -> ${param.reference.encode("it")})"
                else -> "Some(${param.wireName.literal()} -> ${param.reference.encode(param.identifier())})"
            }.prependIndent("    ")
        }
    }

    private fun JsonRpcMethod.resultValue(json: String): String = when (error) {
        null -> result.decodeResult(json)
        else -> "$rpcName.Result(${result.decodeResult(json)})"
    }

    private fun Reference.decodeResult(json: String): String = when {
        this is Reference.Unit -> "()"
        isNullable -> "if ($json == \"null\") None else Some(${copy(isNullable = false).decode(json)})"
        else -> decode(json)
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName.Response" } ?: result.convert().scala()

    private fun server(methods: List<JsonRpcMethod>): String = """
        |/**
        | * Answers JSON-RPC 2.0 requests, single or batched, by calling the service, which implements the `Service` of every
        | * rpc, and writes the values with the serialization.
        | */
        |class JsonRpcServer[S <: ${methods.joinToString(" with ") { "${it.rpcName}.Service" }}](serialization: Wirespec.Serialization, service: S) {
        |  /** A batch is answered with the answers to its requests; a notification is not answered, so its response has status 204 and no body. */
        |  def handle(request: Wirespec.RawRequest): Wirespec.RawResponse = JsonRpc.handle(request.body, (method, params) => method match {
        |${methods.joinToString("\n") { it.serverBranch() }.prependIndent("    ")}
        |    case _ => None
        |  })
        |}
    """.trimMargin()

    private fun JsonRpcMethod.serverBranch(): String = """
        |case ${name.literal()} =>
        |  val response = service.${service.name.camelCase().sanitize()}(${params.joinToString(", ") { it.read() }})
        |${answer().prependIndent("  ")}
    """.trimMargin()

    private fun JsonRpcParam.read(): String = "params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position, json => ${reference.copy(isNullable = false).decode("json")})"

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "Some(JsonRpc.Answer.Result(${result.encodeResult("response")}))"
        else ->
            """
            |Some(response match {
            |  case result: $rpcName.Result => JsonRpc.Answer.Result(${result.encodeResult("result.value")})
            |  case error: $rpcName.Error => JsonRpc.Answer.Failure(JsonRpc.SERVER_ERROR, "Server error", Some(${error.encodeResult("error.value")}))
            |})
            """.trimMargin()
    }

    /** The JSON of a value that can be None or Unit, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "\"null\""
        isNullable -> "$value.map(it => ${encode("it")}).getOrElse(\"null\")"
        else -> encode(value)
    }

    private fun Reference.encode(value: String): String = when {
        isString() -> "JsonRpc.encode($value)"
        else -> "new String(serialization.serializeBody[${valueType().scala()}]($value, scala.reflect.classTag[${valueType().scala()}]), StandardCharsets.UTF_8)"
    }

    private fun Reference.decode(json: String): String = when {
        isString() -> "JsonRpc.decodeString($json)"
        else -> "serialization.deserializeBody[${valueType().scala()}]($json.getBytes(StandardCharsets.UTF_8), scala.reflect.classTag[${valueType().scala()}])"
    }

    private fun JsonRpcParam.identifier(): String = name.camelCase().sanitize()

    private fun String.sanitize(): String = if (this in ScalaGenerator.reservedKeywords) "`$this`" else this

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A type as the Scala emitter writes it. */
    private fun Type.scala(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "Int"
            Precision.P64 -> "Long"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "Float"
            Precision.P64 -> "Double"
        }
        Type.Any -> "Any"
        Type.String -> "String"
        Type.Boolean -> "Boolean"
        Type.Bytes -> "Array[Byte]"
        Type.Unit -> "Unit"
        Type.Wildcard -> "?"
        Type.Reflect -> "scala.reflect.ClassTag[?]"
        is Type.Array -> "List[${elementType.scala()}]"
        is Type.Dict -> "Map[${keyType.scala()}, ${valueType.scala()}]"
        is Type.Custom -> name.referenceName() + generics.takeIf { it.isNotEmpty() }?.joinToString(", ", "[", "]") { it.scala() }.orEmpty()
        is Type.Nullable -> "Option[${type.scala()}]"
        is Type.IntegerLiteral -> "Int"
        is Type.StringLiteral -> "String"
        is Type.Function -> "(${parameterTypes.joinToString(", ") { it.scala() }}) => ${returnType.scala()}"
    }

    /** The runtime is only for the generated code: it is private to the package, when there is one. */
    private fun runtime(packageName: PackageName): String = packageName.value.substringAfterLast('.')
        .takeIf { it.isNotEmpty() }
        .let { scope -> RUNTIME.replace("INTERNAL ", scope?.let { "private[$it] " }.orEmpty()) }

    private val RUNTIME = """
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. */
        |class JsonRpcException(val code: Int, message: String) extends RuntimeException(message)
        |
        |/** The codes of the errors JSON-RPC 2.0 defines, and the reading and writing of its messages. */
        |object JsonRpc {
        |  val PARSE_ERROR: Int = -32700
        |  val INVALID_REQUEST: Int = -32600
        |  val METHOD_NOT_FOUND: Int = -32601
        |  val INVALID_PARAMS: Int = -32602
        |  val INTERNAL_ERROR: Int = -32603
        |  /** The code of the error of an rpc itself, which is the data of the error object. */
        |  val SERVER_ERROR: Int = -32000
        |
        |  /** What a server answers a request with: the JSON of a result, or an error object. */
        |  INTERNAL sealed trait Answer
        |  INTERNAL object Answer {
        |    final case class Result(json: String) extends Answer
        |    final case class Failure(code: Int, message: String, data: Option[String]) extends Answer
        |  }
        |
        |  INTERNAL final class InvalidParams extends RuntimeException("Invalid params")
        |
        |  /** The params of a request, each as the JSON of its value, by name or by position. */
        |  INTERNAL final class Params(byName: Map[String, String], byPosition: List[String]) {
        |    def required[T](name: String, position: Int, read: String => T): T = optional(name, position, read).getOrElse(throw new InvalidParams)
        |
        |    def optional[T](name: String, position: Int, read: String => T): Option[T] = byName.get(name).orElse(byPosition.lift(position))
        |      .filter(_ != "null")
        |      .map(json => try read(json) catch { case _: RuntimeException => throw new InvalidParams })
        |  }
        |
        |  /** Every request has id 1: a transportation answers a request with its own response, so the id needs no counter. */
        |  INTERNAL def call[T](transportation: Wirespec.Transportation, method: String, params: List[(String, String)], onResult: String => T, onError: Option[String => T]): T = {
        |    val request = "{\"jsonrpc\":\"2.0\",\"method\":" + encode(method) +
        |      (if (params.isEmpty) "" else params.map { case (name, json) => encode(name) + ":" + json }.mkString(",\"params\":{", ",", "}")) +
        |      ",\"id\":1}"
        |    val response = transportation.transport(
        |      Wirespec.RawRequest("POST", Nil, Map.empty, Map("Content-Type" -> List("application/json")), Some(request.getBytes(StandardCharsets.UTF_8)))
        |    )
        |    val answer = response.body.flatMap(body => members(new String(body, StandardCharsets.UTF_8)))
        |      .getOrElse(throw new IllegalStateException("The JSON-RPC response to " + method + " is no object, status " + response.statusCode))
        |    answer.get("error").filter(_ != "null") match {
        |      case None => onResult(answer.getOrElse("result", throw new IllegalStateException("The JSON-RPC response to " + method + " has no result")))
        |      case Some(error) =>
        |        val failure = members(error).getOrElse(throw new IllegalStateException("The JSON-RPC error of " + method + " is no object"))
        |        val code = failure.get("code").flatMap(_.toIntOption).getOrElse(INTERNAL_ERROR)
        |        (onError, failure.get("data")) match {
        |          case (Some(read), Some(data)) if code == SERVER_ERROR => read(data)
        |          case _ => throw new JsonRpcException(code, failure.get("message").flatMap(decodeStringOption).getOrElse(""))
        |        }
        |    }
        |  }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered. */
        |  INTERNAL def handle(body: Option[Array[Byte]], dispatch: (String, Params) => Option[Answer]): Wirespec.RawResponse = {
        |    val json = body.map(new String(_, StandardCharsets.UTF_8)).getOrElse("").trim
        |    val answer =
        |      if (!json.startsWith("[")) reply(json, dispatch)
        |      else parts(json, '[', ']') match {
        |        case None => Some(write("null", Answer.Failure(PARSE_ERROR, "Parse error", None)))
        |        case Some(Nil) => Some(write("null", Answer.Failure(INVALID_REQUEST, "Invalid Request", None)))
        |        case Some(batch) => Some(batch.flatMap(reply(_, dispatch))).filter(_.nonEmpty).map(_.mkString("[", ",", "]"))
        |      }
        |    answer match {
        |      case Some(it) => Wirespec.RawResponse(200, Map("Content-Type" -> List("application/json")), Some(it.getBytes(StandardCharsets.UTF_8)))
        |      case None => Wirespec.RawResponse(204, Map.empty, None)
        |    }
        |  }
        |
        |  private def reply(json: String, dispatch: (String, Params) => Option[Answer]): Option[String] = members(json) match {
        |    case None =>
        |      val unparsable = json.isEmpty || json.startsWith("{")
        |      Some(write("null", if (unparsable) Answer.Failure(PARSE_ERROR, "Parse error", None) else Answer.Failure(INVALID_REQUEST, "Invalid Request", None)))
        |    case Some(members) =>
        |      val id = members.get("id")
        |      (members.get("method").flatMap(decodeStringOption), params(members.get("params"))) match {
        |        case (Some(method), Some(params)) if members.get("jsonrpc").contains("\"2.0\"") =>
        |          val answer =
        |            try dispatch(method, params).getOrElse(Answer.Failure(METHOD_NOT_FOUND, "Method not found", None))
        |            catch {
        |              case _: InvalidParams => Answer.Failure(INVALID_PARAMS, "Invalid params", None)
        |              case scala.util.control.NonFatal(_) => Answer.Failure(INTERNAL_ERROR, "Internal error", None)
        |            }
        |          id.map(write(_, answer))
        |        case _ => Some(write(id.getOrElse("null"), Answer.Failure(INVALID_REQUEST, "Invalid Request", None)))
        |      }
        |  }
        |
        |  private def params(json: Option[String]): Option[Params] = json match {
        |    case None => Some(new Params(Map.empty, Nil))
        |    case Some(json) if json.startsWith("{") => members(json).map(new Params(_, Nil))
        |    case Some(json) if json.startsWith("[") => parts(json, '[', ']').map(new Params(Map.empty, _))
        |    case Some(_) => None
        |  }
        |
        |  private def write(id: String, answer: Answer): String = {
        |    val member = answer match {
        |      case Answer.Result(json) => "\"result\":" + json
        |      case Answer.Failure(code, message, data) => "\"error\":{\"code\":" + code + ",\"message\":" + encode(message) + data.map(",\"data\":" + _).getOrElse("") + "}"
        |    }
        |    "{\"jsonrpc\":\"2.0\"," + member + ",\"id\":" + id + "}"
        |  }
        |
        |  /** The members of a JSON object, each as the JSON of its value; None when the JSON is no object. */
        |  private def members(json: String): Option[Map[String, String]] = parts(json.trim, '{', '}').flatMap { parts =>
        |    val members = parts.map { member =>
        |      val end = stringEnd(member)
        |      val value = if (end < 0) "" else member.substring(end).trim
        |      (if (end < 0) None else decodeStringOption(member.substring(0, end))).filter(_ => value.startsWith(":")).map(_ -> value.substring(1).trim)
        |    }
        |    if (members.forall(_.isDefined)) Some(members.flatten.toMap) else None
        |  }
        |
        |  /** The JSON between open and close split at the commas outside of a nested value; None when it is no such JSON. */
        |  private def parts(json: String, open: Char, close: Char): Option[List[String]] =
        |    if (json.length < 2 || json.head != open || json.last != close) None
        |    else {
        |      val parts = scala.collection.mutable.ListBuffer.empty[String]
        |      var depth = 0
        |      var inString = false
        |      var escaped = false
        |      var start = 1
        |      var index = 1
        |      while (index < json.length - 1 && depth >= 0) {
        |        val c = json.charAt(index)
        |        if (inString) {
        |          if (escaped) escaped = false
        |          else if (c == '\\') escaped = true
        |          else if (c == '"') inString = false
        |        } else if (c == '"') inString = true
        |        else if (c == '{' || c == '[') depth += 1
        |        else if (c == '}' || c == ']') depth -= 1
        |        else if (c == ',' && depth == 0) {
        |          parts += json.substring(start, index).trim
        |          start = index + 1
        |        }
        |        index += 1
        |      }
        |      val last = json.substring(start, json.length - 1).trim
        |      if (inString || depth != 0) None
        |      else if (parts.isEmpty && last.isEmpty) Some(Nil)
        |      else Some((parts += last).toList).filter(_.forall(_.nonEmpty))
        |    }
        |
        |  /** The index after the JSON string the JSON starts with, or -1 when it starts with none. */
        |  private def stringEnd(json: String): Int = {
        |    var end = -1
        |    var escaped = false
        |    var index = 1
        |    while (json.startsWith("\"") && end < 0 && index < json.length) {
        |      val c = json.charAt(index)
        |      if (escaped) escaped = false
        |      else if (c == '\\') escaped = true
        |      else if (c == '"') end = index + 1
        |      index += 1
        |    }
        |    end
        |  }
        |
        |  def encode(value: String): String = {
        |    val json = new StringBuilder("\"")
        |    value.foreach {
        |      case '"' => json.append("\\\"")
        |      case '\\' => json.append("\\\\")
        |      case '\n' => json.append("\\n")
        |      case '\r' => json.append("\\r")
        |      case '\t' => json.append("\\t")
        |      case c if c < ' ' => json.append(String.format("\\u%04x", Integer.valueOf(c.toInt)))
        |      case c => json.append(c)
        |    }
        |    json.append('"').toString
        |  }
        |
        |  private def decodeStringOption(json: String): Option[String] = scala.util.Try(decodeString(json)).toOption
        |
        |  def decodeString(json: String): String = {
        |    require(stringEnd(json) == json.length, "No JSON string: " + json)
        |    val value = new StringBuilder
        |    var index = 1
        |    while (index < json.length - 1) {
        |      val c = json.charAt(index)
        |      index += 1
        |      if (c != '\\') value.append(c)
        |      else {
        |        val escape = json.charAt(index)
        |        index += 1
        |        escape match {
        |          case 'b' => value.append('\b')
        |          case 'f' => value.append('\f')
        |          case 'n' => value.append('\n')
        |          case 'r' => value.append('\r')
        |          case 't' => value.append('\t')
        |          case 'u' =>
        |            value.append(Integer.parseInt(json.substring(index, index + 4), 16).toChar)
        |            index += 4
        |          case other => value.append(other)
        |        }
        |      }
        |    }
        |    value.toString
        |  }
        |}
    """.trimMargin()
}
