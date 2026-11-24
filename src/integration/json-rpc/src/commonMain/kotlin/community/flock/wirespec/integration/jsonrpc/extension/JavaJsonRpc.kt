package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.JavaGenerator
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * Java sends through a transportation that returns a `CompletableFuture`, so the client implements the `Service` of an
 * rpc only when that returns one too: with `--sync` it has the same functions, without implementing it. The server
 * calls the services through futures; with `--sync` they are completed ones, so `handle` returns the response on the
 * thread that handles the request.
 *
 * Java has a public class per file: the client, the server, the exception and `JsonRpc`, which holds the codes and
 * reads and writes the messages.
 */
internal object JavaJsonRpc : JsonRpcDialect {

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir + listOf(
        Triple("JsonRpcClient", imports(packageName, methods), client(methods)),
        Triple("JsonRpcServer", imports(packageName, methods), server(methods)),
        Triple("JsonRpcException", emptyList(), EXCEPTION),
        Triple("JsonRpc", RUNTIME_IMPORTS, RUNTIME),
    ).map { (name, imports, code) ->
        LanguageFile(Name.of(packageName.toDir() + name), listOf(RawElement((listOf("package ${packageName.value};") + imports).joinToString("\n", postfix = "\n\n") + code)))
    }

    private fun imports(packageName: PackageName, methods: List<JsonRpcMethod>): List<String> = RUNTIME_IMPORTS +
        methods.flatMap { it.models }.distinct().map { "import ${packageName.value}.model.${Name.of(it).pascalCase()};" } +
        methods.map { "import ${packageName.value}.rpc.${it.rpcName};" }

    private val RUNTIME_IMPORTS = listOf("import community.flock.wirespec.java.Wirespec;", "import java.nio.charset.StandardCharsets;")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |/** Sends every rpc as a JSON-RPC 2.0 request through the transportation, and reads the values with the serialization. */
        |public record JsonRpcClient(
        |  Wirespec.Serialization serialization,
        |  Wirespec.Transportation transportation
        |)${methods.filter { it.isAsync }.joinToString(", ") { "${it.rpcName}.Service" }.let { if (it.isEmpty()) "" else " implements $it" }} {
        |${methods.joinToString("\n") { it.clientFunction() }.prependIndent("  ")}
        |}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |${if (isAsync) "@Override\n" else ""}public java.util.concurrent.CompletableFuture<${returnType()}> ${service.name.camelCase().sanitize()}(${params.joinToString(", ") { "${it.type.java()} ${it.identifier()}" }}) {
        |  return JsonRpc.<${returnType()}>call(
        |    transportation,
        |    ${name.literal()},
        |    ${params.paramsList()},
        |    json -> ${resultValue("json")},
        |    ${error?.let { "json -> new $rpcName.Error(${it.decode("json")})" } ?: "null"}
        |  );
        |}
    """.trimMargin()

    private fun List<JsonRpcParam>.paramsList(): String = if (isEmpty()) {
        "java.util.List.of()"
    } else {
        joinToString(",\n", "java.util.List.of(\n", "\n    )") { param ->
            when {
                param.isNullable -> "${param.identifier()}.map(it -> java.util.Map.entry(${param.wireName.literal()}, ${param.reference.encode("it")}))"
                else -> "java.util.Optional.of(java.util.Map.entry(${param.wireName.literal()}, ${param.reference.encode(param.identifier())}))"
            }.prependIndent("      ")
        }
    }

    private fun JsonRpcMethod.resultValue(json: String): String = when (error) {
        null -> result.decodeResult(json)
        else -> "new $rpcName.Result(${result.decodeResult(json)})"
    }

    /** A unit result is a `Void`, which is null. */
    private fun Reference.decodeResult(json: String): String = when {
        this is Reference.Unit -> "null"
        isNullable -> "$json.equals(\"null\") ? java.util.Optional.<${valueType().java()}>empty() : java.util.Optional.of(${copy(isNullable = false).decode(json)})"
        else -> decode(json)
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName.Response" } ?: result.convert().java()

    private fun server(methods: List<JsonRpcMethod>): String = """
        |/**
        | * Answers JSON-RPC 2.0 requests, single or batched, by calling the service, which implements the `Service` of every
        | * rpc, and writes the values with the serialization.
        | */
        |public final class JsonRpcServer<S extends ${methods.joinToString(" & ") { "${it.rpcName}.Service" }}> {
        |  private final Wirespec.Serialization serialization;
        |  private final S service;
        |
        |  public JsonRpcServer(Wirespec.Serialization serialization, S service) {
        |    this.serialization = serialization;
        |    this.service = service;
        |  }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered, so its response has status 204 and no body. */
        |  ${if (methods.any { it.isAsync }) "public java.util.concurrent.CompletableFuture<Wirespec.RawResponse> handle(Wirespec.RawRequest request) {\n    return JsonRpc.handle(request.body(), this::dispatch);" else "public Wirespec.RawResponse handle(Wirespec.RawRequest request) {\n    return JsonRpc.handle(request.body(), this::dispatch).join();"}
        |  }
        |
        |  private java.util.concurrent.CompletableFuture<JsonRpc.Answer> dispatch(String method, JsonRpc.Params params) {
        |    return switch (method) {
        |${methods.joinToString("\n") { it.serverBranch() }.prependIndent("      ")}
        |      default -> null;
        |    };
        |  }
        |}
    """.trimMargin()

    private fun JsonRpcMethod.serverBranch(): String = "case ${name.literal()} -> ${call()}.thenApply(response -> ${answer()});"

    private fun JsonRpcMethod.call(): String = "service.${service.name.camelCase().sanitize()}(${params.joinToString(", ") { it.read() }})"
        .let { if (isAsync) it else "java.util.concurrent.CompletableFuture.completedFuture($it)" }

    private fun JsonRpcParam.read(): String = "params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position, json -> ${reference.copy(isNullable = false).decode("json")})"

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "JsonRpc.Answer.result(${result.encodeResult("response")})"
        else ->
            "response instanceof $rpcName.Result result" +
                " ? JsonRpc.Answer.result(${result.encodeResult("result.value()")})" +
                " : JsonRpc.Answer.failure(JsonRpc.SERVER_ERROR, \"Server error\", ${error.encodeResult("(($rpcName.Error) response).value()")})"
    }

    /** The JSON of a value that can be empty or `Void`, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "\"null\""
        isNullable -> "$value.map(it -> ${encode("it")}).orElse(\"null\")"
        else -> encode(value)
    }

    private fun Reference.encode(value: String): String = when {
        isString() -> "JsonRpc.encode($value)"
        else -> "new String(serialization.serializeBody($value, ${valueType().descriptor()}), StandardCharsets.UTF_8)"
    }

    private fun Reference.decode(json: String): String = when {
        isString() -> "JsonRpc.decodeString($json)"
        else -> "serialization.<${valueType().java()}>deserializeBody($json.getBytes(StandardCharsets.UTF_8), ${valueType().descriptor()})"
    }

    /** The type as `Wirespec.getType` takes it: the class of what a list, map or optional holds, and the class of the container. */
    private fun Type.descriptor(): String = "Wirespec.getType(${root().java()}.class, ${container()?.let { "$it.class" } ?: "null"})"

    private fun Type.root(): Type = when (this) {
        is Type.Nullable -> type.root()
        is Type.Array -> elementType.root()
        is Type.Dict -> valueType.root()
        else -> this
    }

    private fun Type.container(): String? = when (this) {
        is Type.Nullable -> "java.util.Optional"
        is Type.Array -> "java.util.List"
        is Type.Dict -> "java.util.Map"
        else -> null
    }

    private fun JsonRpcParam.identifier(): String = name.camelCase().sanitize()

    private fun String.sanitize(): String = if (this in JavaGenerator.reservedKeywords) "_$this" else this

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A type as the Java emitter writes it. */
    private fun Type.java(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "Integer"
            Precision.P64 -> "Long"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "Float"
            Precision.P64 -> "Double"
        }
        Type.Any -> "Object"
        Type.String -> "String"
        Type.Boolean -> "Boolean"
        Type.Bytes -> "byte[]"
        Type.Unit -> "Void"
        Type.Wildcard -> "?"
        Type.Reflect -> "java.lang.reflect.Type"
        is Type.Array -> "java.util.List<${elementType.java()}>"
        is Type.Dict -> "java.util.Map<${keyType.java()}, ${valueType.java()}>"
        is Type.Custom -> name.referenceName() + generics.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.java() }.orEmpty()
        is Type.Nullable -> "java.util.Optional<${type.java()}>"
        is Type.IntegerLiteral -> "Integer"
        is Type.StringLiteral -> "String"
        is Type.Function -> "java.util.function.Function<${parameterTypes.joinToString(", ") { it.java() }}, ${returnType.java()}>"
    }

    private val EXCEPTION = """
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. */
        |public class JsonRpcException extends RuntimeException {
        |  private final int code;
        |
        |  public JsonRpcException(int code, String message) {
        |    super(message);
        |    this.code = code;
        |  }
        |
        |  public int getCode() {
        |    return code;
        |  }
        |}
    """.trimMargin()

    private val RUNTIME = """
        |/** The codes of the errors JSON-RPC 2.0 defines, and the reading and writing of its messages. */
        |public final class JsonRpc {
        |  public static final int PARSE_ERROR = -32700;
        |  public static final int INVALID_REQUEST = -32600;
        |  public static final int METHOD_NOT_FOUND = -32601;
        |  public static final int INVALID_PARAMS = -32602;
        |  public static final int INTERNAL_ERROR = -32603;
        |  /** The code of the error of an rpc itself, which is the data of the error object. */
        |  public static final int SERVER_ERROR = -32000;
        |
        |  private JsonRpc() {
        |  }
        |
        |  /** What a server answers a request with: the JSON of a result, or an error object. */
        |  record Answer(String result, int code, String message, String data) {
        |    static Answer result(String json) {
        |      return new Answer(json, 0, null, null);
        |    }
        |
        |    static Answer failure(int code, String message, String data) {
        |      return new Answer(null, code, message, data);
        |    }
        |  }
        |
        |  static final class InvalidParams extends RuntimeException {
        |    InvalidParams() {
        |      super("Invalid params");
        |    }
        |  }
        |
        |  /** The params of a request, each as the JSON of its value, by name or by position. */
        |  static final class Params {
        |    private final java.util.Map<String, String> byName;
        |    private final java.util.List<String> byPosition;
        |
        |    Params(java.util.Map<String, String> byName, java.util.List<String> byPosition) {
        |      this.byName = byName;
        |      this.byPosition = byPosition;
        |    }
        |
        |    <T> T required(String name, int position, java.util.function.Function<String, T> read) {
        |      return optional(name, position, read).orElseThrow(InvalidParams::new);
        |    }
        |
        |    <T> java.util.Optional<T> optional(String name, int position, java.util.function.Function<String, T> read) {
        |      String json = byName.containsKey(name) ? byName.get(name) : position < byPosition.size() ? byPosition.get(position) : null;
        |      if (json == null || json.equals("null")) {
        |        return java.util.Optional.empty();
        |      }
        |      try {
        |        return java.util.Optional.ofNullable(read.apply(json));
        |      } catch (RuntimeException e) {
        |        throw new InvalidParams();
        |      }
        |    }
        |  }
        |
        |  /** Every request has id 1: a transportation answers a request with its own response, so the id needs no counter. */
        |  static <T> java.util.concurrent.CompletableFuture<T> call(
        |    Wirespec.Transportation transportation,
        |    String method,
        |    java.util.List<java.util.Optional<java.util.Map.Entry<String, String>>> params,
        |    java.util.function.Function<String, T> onResult,
        |    java.util.function.Function<String, T> onError
        |  ) {
        |    java.util.List<String> members = params.stream().flatMap(java.util.Optional::stream).map(it -> encode(it.getKey()) + ":" + it.getValue()).toList();
        |    String request = "{\"jsonrpc\":\"2.0\",\"method\":" + encode(method) +
        |      (members.isEmpty() ? "" : ",\"params\":{" + String.join(",", members) + "}") +
        |      ",\"id\":1}";
        |    return transportation.transport(new Wirespec.RawRequest(
        |      "POST",
        |      java.util.List.of(),
        |      java.util.Map.of(),
        |      java.util.Map.of("Content-Type", java.util.List.of("application/json")),
        |      java.util.Optional.of(request.getBytes(StandardCharsets.UTF_8))
        |    )).thenApply(response -> read(method, response, onResult, onError));
        |  }
        |
        |  private static <T> T read(String method, Wirespec.RawResponse response, java.util.function.Function<String, T> onResult, java.util.function.Function<String, T> onError) {
        |    java.util.Map<String, String> members = response.body().map(body -> members(new String(body, StandardCharsets.UTF_8))).orElse(null);
        |    if (members == null) {
        |      throw new IllegalStateException("The JSON-RPC response to " + method + " is no object, status " + response.statusCode());
        |    }
        |    String error = members.get("error");
        |    if (error == null || error.equals("null")) {
        |      String result = members.get("result");
        |      if (result == null) {
        |        throw new IllegalStateException("The JSON-RPC response to " + method + " has no result");
        |      }
        |      return onResult.apply(result);
        |    }
        |    java.util.Map<String, String> failure = members(error);
        |    if (failure == null) {
        |      throw new IllegalStateException("The JSON-RPC error of " + method + " is no object");
        |    }
        |    int code = code(failure.get("code"));
        |    String data = failure.get("data");
        |    if (code == SERVER_ERROR && onError != null && data != null) {
        |      return onError.apply(data);
        |    }
        |    String message = decodeStringOrNull(failure.get("message"));
        |    throw new JsonRpcException(code, message == null ? "" : message);
        |  }
        |
        |  private static int code(String json) {
        |    try {
        |      return json == null ? INTERNAL_ERROR : Integer.parseInt(json);
        |    } catch (NumberFormatException e) {
        |      return INTERNAL_ERROR;
        |    }
        |  }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered. */
        |  static java.util.concurrent.CompletableFuture<Wirespec.RawResponse> handle(
        |    java.util.Optional<byte[]> body,
        |    java.util.function.BiFunction<String, Params, java.util.concurrent.CompletableFuture<Answer>> dispatch
        |  ) {
        |    String json = body.map(it -> new String(it, StandardCharsets.UTF_8)).orElse("").trim();
        |    java.util.List<String> batch = json.startsWith("[") ? parts(json, '[', ']') : null;
        |    java.util.concurrent.CompletableFuture<String> answer;
        |    if (!json.startsWith("[")) {
        |      answer = reply(json, dispatch);
        |    } else if (batch == null) {
        |      answer = java.util.concurrent.CompletableFuture.completedFuture(write("null", Answer.failure(PARSE_ERROR, "Parse error", null)));
        |    } else if (batch.isEmpty()) {
        |      answer = java.util.concurrent.CompletableFuture.completedFuture(write("null", Answer.failure(INVALID_REQUEST, "Invalid Request", null)));
        |    } else {
        |      java.util.List<java.util.concurrent.CompletableFuture<String>> replies = batch.stream().map(it -> reply(it, dispatch)).toList();
        |      answer = java.util.concurrent.CompletableFuture.allOf(replies.toArray(new java.util.concurrent.CompletableFuture<?>[0])).thenApply(done -> {
        |        java.util.List<String> answered = replies.stream().map(java.util.concurrent.CompletableFuture::join).filter(java.util.Objects::nonNull).toList();
        |        return answered.isEmpty() ? null : "[" + String.join(",", answered) + "]";
        |      });
        |    }
        |    return answer.thenApply(it -> it == null
        |      ? new Wirespec.RawResponse(204, java.util.Map.of(), java.util.Optional.empty())
        |      : new Wirespec.RawResponse(200, java.util.Map.of("Content-Type", java.util.List.of("application/json")), java.util.Optional.of(it.getBytes(StandardCharsets.UTF_8))));
        |  }
        |
        |  private static java.util.concurrent.CompletableFuture<String> reply(
        |    String json,
        |    java.util.function.BiFunction<String, Params, java.util.concurrent.CompletableFuture<Answer>> dispatch
        |  ) {
        |    java.util.Map<String, String> members = members(json);
        |    if (members == null) {
        |      boolean unparsable = json.isEmpty() || json.startsWith("{");
        |      return java.util.concurrent.CompletableFuture.completedFuture(write("null", unparsable ? Answer.failure(PARSE_ERROR, "Parse error", null) : Answer.failure(INVALID_REQUEST, "Invalid Request", null)));
        |    }
        |    String id = members.get("id");
        |    String method = decodeStringOrNull(members.get("method"));
        |    Params params = params(members.get("params"));
        |    if (!"\"2.0\"".equals(members.get("jsonrpc")) || method == null || params == null) {
        |      return java.util.concurrent.CompletableFuture.completedFuture(write(id == null ? "null" : id, Answer.failure(INVALID_REQUEST, "Invalid Request", null)));
        |    }
        |    java.util.concurrent.CompletableFuture<Answer> answer;
        |    try {
        |      java.util.concurrent.CompletableFuture<Answer> dispatched = dispatch.apply(method, params);
        |      answer = dispatched == null ? java.util.concurrent.CompletableFuture.completedFuture(Answer.failure(METHOD_NOT_FOUND, "Method not found", null)) : dispatched;
        |    } catch (RuntimeException e) {
        |      answer = java.util.concurrent.CompletableFuture.failedFuture(e);
        |    }
        |    return answer.exceptionally(JsonRpc::failure).thenApply(it -> id == null ? null : write(id, it));
        |  }
        |
        |  private static Answer failure(Throwable error) {
        |    Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null ? error.getCause() : error;
        |    return cause instanceof InvalidParams ? Answer.failure(INVALID_PARAMS, "Invalid params", null) : Answer.failure(INTERNAL_ERROR, "Internal error", null);
        |  }
        |
        |  private static Params params(String json) {
        |    if (json == null) {
        |      return new Params(java.util.Map.of(), java.util.List.of());
        |    }
        |    if (json.startsWith("{")) {
        |      java.util.Map<String, String> byName = members(json);
        |      return byName == null ? null : new Params(byName, java.util.List.of());
        |    }
        |    if (json.startsWith("[")) {
        |      java.util.List<String> byPosition = parts(json, '[', ']');
        |      return byPosition == null ? null : new Params(java.util.Map.of(), byPosition);
        |    }
        |    return null;
        |  }
        |
        |  private static String write(String id, Answer answer) {
        |    String member = answer.result() != null
        |      ? "\"result\":" + answer.result()
        |      : "\"error\":{\"code\":" + answer.code() + ",\"message\":" + encode(answer.message()) + (answer.data() == null ? "" : ",\"data\":" + answer.data()) + "}";
        |    return "{\"jsonrpc\":\"2.0\"," + member + ",\"id\":" + id + "}";
        |  }
        |
        |  /** The members of a JSON object, each as the JSON of its value; null when the JSON is no object. */
        |  static java.util.Map<String, String> members(String json) {
        |    java.util.List<String> parts = parts(json.trim(), '{', '}');
        |    if (parts == null) {
        |      return null;
        |    }
        |    java.util.Map<String, String> members = new java.util.LinkedHashMap<>();
        |    for (String member : parts) {
        |      int end = stringEnd(member);
        |      String value = end < 0 ? "" : member.substring(end).trim();
        |      String key = end < 0 ? null : decodeStringOrNull(member.substring(0, end));
        |      if (key == null || !value.startsWith(":")) {
        |        return null;
        |      }
        |      members.put(key, value.substring(1).trim());
        |    }
        |    return members;
        |  }
        |
        |  /** The JSON between open and close split at the commas outside of a nested value; null when it is no such JSON. */
        |  static java.util.List<String> parts(String json, char open, char close) {
        |    if (json.length() < 2 || json.charAt(0) != open || json.charAt(json.length() - 1) != close) {
        |      return null;
        |    }
        |    java.util.List<String> parts = new java.util.ArrayList<>();
        |    int depth = 0;
        |    boolean inString = false;
        |    boolean escaped = false;
        |    int start = 1;
        |    for (int index = 1; index < json.length() - 1; index++) {
        |      char c = json.charAt(index);
        |      if (inString) {
        |        if (escaped) {
        |          escaped = false;
        |        } else if (c == '\\') {
        |          escaped = true;
        |        } else if (c == '"') {
        |          inString = false;
        |        }
        |      } else if (c == '"') {
        |        inString = true;
        |      } else if (c == '{' || c == '[') {
        |        depth++;
        |      } else if (c == '}' || c == ']') {
        |        if (--depth < 0) {
        |          return null;
        |        }
        |      } else if (c == ',' && depth == 0) {
        |        parts.add(json.substring(start, index).trim());
        |        start = index + 1;
        |      }
        |    }
        |    String last = json.substring(start, json.length() - 1).trim();
        |    if (inString || depth != 0) {
        |      return null;
        |    }
        |    if (parts.isEmpty() && last.isEmpty()) {
        |      return parts;
        |    }
        |    parts.add(last);
        |    return parts.stream().anyMatch(String::isEmpty) ? null : parts;
        |  }
        |
        |  /** The index after the JSON string the JSON starts with, or -1 when it starts with none. */
        |  static int stringEnd(String json) {
        |    if (!json.startsWith("\"")) {
        |      return -1;
        |    }
        |    boolean escaped = false;
        |    for (int index = 1; index < json.length(); index++) {
        |      char c = json.charAt(index);
        |      if (escaped) {
        |        escaped = false;
        |      } else if (c == '\\') {
        |        escaped = true;
        |      } else if (c == '"') {
        |        return index + 1;
        |      }
        |    }
        |    return -1;
        |  }
        |
        |  public static String encode(String value) {
        |    StringBuilder json = new StringBuilder("\"");
        |    for (char c : value.toCharArray()) {
        |      switch (c) {
        |        case '"' -> json.append("\\\"");
        |        case '\\' -> json.append("\\\\");
        |        case '\n' -> json.append("\\n");
        |        case '\r' -> json.append("\\r");
        |        case '\t' -> json.append("\\t");
        |        default -> {
        |          if (c < ' ') {
        |            json.append(String.format("\\u%04x", (int) c));
        |          } else {
        |            json.append(c);
        |          }
        |        }
        |      }
        |    }
        |    return json.append('"').toString();
        |  }
        |
        |  /** The value of a JSON string, or null when the JSON is no string. */
        |  static String decodeStringOrNull(String json) {
        |    try {
        |      return json == null ? null : decodeString(json);
        |    } catch (RuntimeException e) {
        |      return null;
        |    }
        |  }
        |
        |  public static String decodeString(String json) {
        |    if (stringEnd(json) != json.length()) {
        |      throw new IllegalArgumentException("No JSON string: " + json);
        |    }
        |    StringBuilder value = new StringBuilder();
        |    int index = 1;
        |    while (index < json.length() - 1) {
        |      char c = json.charAt(index++);
        |      if (c != '\\') {
        |        value.append(c);
        |        continue;
        |      }
        |      char escape = json.charAt(index++);
        |      switch (escape) {
        |        case 'b' -> value.append('\b');
        |        case 'f' -> value.append('\f');
        |        case 'n' -> value.append('\n');
        |        case 'r' -> value.append('\r');
        |        case 't' -> value.append('\t');
        |        case 'u' -> {
        |          value.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
        |          index += 4;
        |        }
        |        default -> value.append(escape);
        |      }
        |    }
        |    return value.toString();
        |  }
        |}
    """.trimMargin()
}
