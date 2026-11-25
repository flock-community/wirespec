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
        |    ${error?.let { "new JsonRpc.RpcError<>($errorCode, json -> new $rpcName.Error(${it.decode("json")}))" } ?: "null"}
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
                " : JsonRpc.Answer.failure($errorCode, ${errorMessage.literal()}, ${error.encodeResult("(($rpcName.Error) response).value()")})"
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
        |/** An error object a JSON-RPC server answered with, other than the error of the rpc itself. Its data is JSON. */
        |public class JsonRpcException extends RuntimeException {
        |  private final int code;
        |  private final String data;
        |
        |  public JsonRpcException(int code, String message, String data) {
        |    super(message);
        |    this.code = code;
        |    this.data = data;
        |  }
        |
        |  public int getCode() {
        |    return code;
        |  }
        |
        |  public java.util.Optional<String> getData() {
        |    return java.util.Optional.ofNullable(data);
        |  }
        |}
    """.trimMargin()

    private val RUNTIME = """
        |/**
        | * The codes of the errors JSON-RPC 2.0 defines, and the reading and writing of its messages, as the specification
        | * writes them: https://www.jsonrpc.org/specification
        | */
        |public final class JsonRpc {
        |  public static final int PARSE_ERROR = -32700;
        |  public static final int INVALID_REQUEST = -32600;
        |  public static final int METHOD_NOT_FOUND = -32601;
        |  public static final int INVALID_PARAMS = -32602;
        |  public static final int INTERNAL_ERROR = -32603;
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
        |  /** The error of an rpc: the code of its error object, and how to read the error from its data. */
        |  record RpcError<T>(int code, java.util.function.Function<String, T> read) {
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
        |    RpcError<T> onError
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
        |  /** An error object with the code of the error of the rpc is that error, which is read from its data. */
        |  private static <T> T read(String method, Wirespec.RawResponse response, java.util.function.Function<String, T> onResult, RpcError<T> onError) {
        |    java.util.Map<String, String> members = response.body().map(body -> response(new String(body, StandardCharsets.UTF_8))).orElse(null);
        |    if (members == null) {
        |      throw new IllegalStateException("The answer to " + method + " is no JSON-RPC 2.0 response, status " + response.statusCode());
        |    }
        |    if (!members.containsKey("error")) {
        |      return onResult.apply(members.get("result"));
        |    }
        |    java.util.Map<String, String> failure = members(members.get("error"));
        |    int code = Integer.parseInt(failure.get("code"));
        |    String data = failure.get("data");
        |    if (onError != null && data != null && code == onError.code()) {
        |      return onError.read().apply(data);
        |    }
        |    throw new JsonRpcException(code, decodeString(failure.get("message")), data);
        |  }
        |
        |  /**
        |   * The members of a response to a request with id 1: it has either a result or an error object with an integer code
        |   * and a message, and the id of the request, or null when the server could not read it. Null for any other JSON.
        |   */
        |  static java.util.Map<String, String> response(String json) {
        |    java.util.Map<String, String> members = isJson(json) ? members(json) : null;
        |    if (members == null || !"2.0".equals(stringOrNull(members.get("jsonrpc"))) || members.containsKey("result") == members.containsKey("error")) {
        |      return null;
        |    }
        |    java.util.Map<String, String> failure = members.containsKey("error") ? members(members.get("error")) : null;
        |    boolean validError = !members.containsKey("error") || (failure != null && isInteger(failure.get("code")) && stringOrNull(failure.get("message")) != null);
        |    boolean validId = "1".equals(members.get("id")) || ("null".equals(members.get("id")) && members.containsKey("error"));
        |    return validError && validId ? members : null;
        |  }
        |
        |  private static boolean isInteger(String json) {
        |    try {
        |      Integer.parseInt(json);
        |      return true;
        |    } catch (NumberFormatException e) {
        |      return false;
        |    }
        |  }
        |
        |  /** A batch is answered with the answers to its requests; a notification is not answered, and nor is a batch of them. */
        |  static java.util.concurrent.CompletableFuture<Wirespec.RawResponse> handle(
        |    java.util.Optional<byte[]> body,
        |    java.util.function.BiFunction<String, Params, java.util.concurrent.CompletableFuture<Answer>> dispatch
        |  ) {
        |    String json = body.map(it -> new String(it, StandardCharsets.UTF_8)).orElse("").trim();
        |    boolean valid = isJson(json);
        |    java.util.List<String> batch = valid ? parts(json, '[', ']') : null;
        |    java.util.concurrent.CompletableFuture<String> answer;
        |    if (!valid) {
        |      answer = java.util.concurrent.CompletableFuture.completedFuture(write("null", Answer.failure(PARSE_ERROR, "Parse error", null)));
        |    } else if (batch == null) {
        |      answer = reply(json, dispatch);
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
        |  /**
        |   * The answer to one request, or null for a notification. A request that is invalid is answered with its id, or with
        |   * null when it has none, or one that is no String, Number or Null.
        |   */
        |  private static java.util.concurrent.CompletableFuture<String> reply(
        |    String json,
        |    java.util.function.BiFunction<String, Params, java.util.concurrent.CompletableFuture<Answer>> dispatch
        |  ) {
        |    java.util.Map<String, String> members = members(json);
        |    if (members == null) {
        |      return java.util.concurrent.CompletableFuture.completedFuture(write("null", Answer.failure(INVALID_REQUEST, "Invalid Request", null)));
        |    }
        |    String id = members.get("id");
        |    String method = stringOrNull(members.get("method"));
        |    Params params = params(members.get("params"));
        |    if (!"2.0".equals(stringOrNull(members.get("jsonrpc"))) || method == null || params == null || (id != null && !isId(id))) {
        |      return java.util.concurrent.CompletableFuture.completedFuture(write(id != null && isId(id) ? id : "null", Answer.failure(INVALID_REQUEST, "Invalid Request", null)));
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
        |  /** Params are an Object, by name, or an Array, by position; a request may leave them out. */
        |  private static Params params(String json) {
        |    if (json == null) {
        |      return new Params(java.util.Map.of(), java.util.List.of());
        |    }
        |    if (json.startsWith("{")) {
        |      return new Params(members(json), java.util.List.of());
        |    }
        |    if (json.startsWith("[")) {
        |      return new Params(java.util.Map.of(), parts(json, '[', ']'));
        |    }
        |    return null;
        |  }
        |
        |  /** An id is a String, a Number or Null. */
        |  private static boolean isId(String json) {
        |    char first = json.charAt(0);
        |    return json.equals("null") || first == '"' || first == '-' || (first >= '0' && first <= '9');
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
        |      int end = stringEnd(member, 0);
        |      String value = member.substring(end).trim();
        |      members.put(decodeString(member.substring(0, end)), value.substring(1).trim());
        |    }
        |    return members;
        |  }
        |
        |  /** The values of the JSON between open and close, split at the commas outside of a nested value. */
        |  static java.util.List<String> parts(String json, char open, char close) {
        |    if (json.length() < 2 || json.charAt(0) != open || json.charAt(json.length() - 1) != close) {
        |      return null;
        |    }
        |    java.util.List<String> parts = new java.util.ArrayList<>();
        |    int depth = 0;
        |    int start = 1;
        |    for (int index = 1; index < json.length() - 1; index++) {
        |      char c = json.charAt(index);
        |      if (c == '"') {
        |        index = stringEnd(json, index) - 1;
        |      } else if (c == '{' || c == '[') {
        |        depth++;
        |      } else if (c == '}' || c == ']') {
        |        depth--;
        |      } else if (c == ',' && depth == 0) {
        |        parts.add(json.substring(start, index).trim());
        |        start = index + 1;
        |      }
        |    }
        |    String last = json.substring(start, json.length() - 1).trim();
        |    if (!parts.isEmpty() || !last.isEmpty()) {
        |      parts.add(last);
        |    }
        |    return parts;
        |  }
        |
        |  /** Whether the JSON is one JSON value, as RFC 8259 writes it. It reads nested values with a stack, so deep JSON cannot overflow. */
        |  static boolean isJson(String json) {
        |    java.util.Deque<Character> closing = new java.util.ArrayDeque<>();
        |    int index = 0;
        |    // A value comes next, the name of a member, or a comma or the end of an object or array.
        |    char next = 'v';
        |    while (true) {
        |      index = whitespaceEnd(json, index);
        |      if (index == json.length()) {
        |        return next == ',' && closing.isEmpty();
        |      }
        |      char c = json.charAt(index);
        |      if (next == 'v' && (c == '{' || c == '[')) {
        |        closing.push(c == '{' ? '}' : ']');
        |        index = whitespaceEnd(json, index + 1);
        |        if (index < json.length() && json.charAt(index) == closing.peek()) {
        |          closing.pop();
        |          index++;
        |          next = ',';
        |        } else {
        |          next = c == '{' ? 'k' : 'v';
        |        }
        |      } else if (next == 'v') {
        |        index = scalarEnd(json, index);
        |        if (index < 0) {
        |          return false;
        |        }
        |        next = ',';
        |      } else if (next == 'k') {
        |        int end = stringEnd(json, index);
        |        if (end < 0) {
        |          return false;
        |        }
        |        index = whitespaceEnd(json, end);
        |        if (index == json.length() || json.charAt(index) != ':') {
        |          return false;
        |        }
        |        index++;
        |        next = 'v';
        |      } else if (closing.isEmpty()) {
        |        return false;
        |      } else if (c == ',') {
        |        index++;
        |        next = closing.peek() == '}' ? 'k' : 'v';
        |      } else if (c == closing.peek()) {
        |        closing.pop();
        |        index++;
        |      } else {
        |        return false;
        |      }
        |    }
        |  }
        |
        |  private static int scalarEnd(String json, int start) {
        |    if (json.charAt(start) == '"') {
        |      return stringEnd(json, start);
        |    }
        |    if (json.startsWith("true", start) || json.startsWith("null", start)) {
        |      return start + 4;
        |    }
        |    if (json.startsWith("false", start)) {
        |      return start + 5;
        |    }
        |    return numberEnd(json, start);
        |  }
        |
        |  /** The index after the number at start, or -1 when there is none: a minus, an integer without leading zeros, a fraction and an exponent. */
        |  private static int numberEnd(String json, int start) {
        |    int index = start < json.length() && json.charAt(start) == '-' ? start + 1 : start;
        |    if (index == json.length()) {
        |      return -1;
        |    }
        |    char first = json.charAt(index);
        |    if (first == '0') {
        |      index++;
        |    } else if (first >= '1' && first <= '9') {
        |      index = digitsEnd(json, index);
        |    } else {
        |      return -1;
        |    }
        |    if (index < json.length() && json.charAt(index) == '.') {
        |      int digits = index + 1;
        |      index = digitsEnd(json, digits);
        |      if (index == digits) {
        |        return -1;
        |      }
        |    }
        |    if (index < json.length() && (json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
        |      int digits = index + 1 < json.length() && (json.charAt(index + 1) == '+' || json.charAt(index + 1) == '-') ? index + 2 : index + 1;
        |      index = digitsEnd(json, digits);
        |      if (index == digits) {
        |        return -1;
        |      }
        |    }
        |    return index;
        |  }
        |
        |  private static int digitsEnd(String json, int start) {
        |    int index = start;
        |    while (index < json.length() && json.charAt(index) >= '0' && json.charAt(index) <= '9') {
        |      index++;
        |    }
        |    return index;
        |  }
        |
        |  private static int whitespaceEnd(String json, int start) {
        |    int index = start;
        |    while (index < json.length() && " \t\n\r".indexOf(json.charAt(index)) >= 0) {
        |      index++;
        |    }
        |    return index;
        |  }
        |
        |  /** The index after the JSON string at start, or -1 when there is none: a control character or an unknown escape ends none. */
        |  static int stringEnd(String json, int start) {
        |    if (start >= json.length() || json.charAt(start) != '"') {
        |      return -1;
        |    }
        |    int index = start + 1;
        |    while (index < json.length()) {
        |      char c = json.charAt(index);
        |      if (c == '"') {
        |        return index + 1;
        |      } else if (c < ' ') {
        |        return -1;
        |      } else if (c != '\\') {
        |        index++;
        |      } else if (index + 1 < json.length() && "\"\\/bfnrt".indexOf(json.charAt(index + 1)) >= 0) {
        |        index += 2;
        |      } else if (index + 5 < json.length() && json.charAt(index + 1) == 'u' && json.substring(index + 2, index + 6).chars().allMatch(it -> Character.digit(it, 16) >= 0)) {
        |        index += 6;
        |      } else {
        |        return -1;
        |      }
        |    }
        |    return -1;
        |  }
        |
        |  /** The value of a JSON string, or null when the JSON is no string. */
        |  static String stringOrNull(String json) {
        |    return json != null && stringEnd(json, 0) == json.length() ? decodeString(json) : null;
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
        |  public static String decodeString(String json) {
        |    if (stringEnd(json, 0) != json.length()) {
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
