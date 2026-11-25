package community.flock.wirespec.integration.jsonrpc.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.RustGenerator
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.ir.File as LanguageFile

/**
 * The Rust `Service` of an rpc returns its response, so it cannot fail. The client does not implement it: its
 * functions return a `Result`, with a [JSON_RPC_ERROR] when the request could not be sent, the response could not be
 * read, or the server answered with an error object. A service cannot fail either, so the server never answers with
 * an internal error.
 *
 * The file is a module of its own, `json_rpc`, declared in the `mod.rs` of the root.
 */
internal object RustJsonRpc : JsonRpcDialect {

    private const val JSON_RPC_ERROR = "JsonRpcError"

    private const val MODULE = "json_rpc"

    override fun extend(ir: IR, packageName: PackageName, methods: List<JsonRpcMethod>): IR = ir
        .map { element -> (element as? LanguageFile)?.takeIf { it.name.value() == packageName.toDir() + "mod" }?.withModule() ?: element }
        .plus(LanguageFile(Name.of(packageName.toDir() + MODULE), listOf(RawElement(source(methods)))))

    private fun LanguageFile.withModule(): LanguageFile = copy(elements = elements.map<Element, Element> { (it as? RawElement)?.let { raw -> RawElement(raw.code.trimEnd() + "\npub mod $MODULE;") } ?: it })

    private fun source(methods: List<JsonRpcMethod>): String = listOf(
        imports(methods),
        RUNTIME,
        client(methods),
        server(methods),
        SCANNER,
    ).joinToString("\n\n", postfix = "\n")

    private fun imports(methods: List<JsonRpcMethod>): String = (
        listOf("use std::any::TypeId;", "use std::collections::HashMap;", "use super::wirespec::*;") +
            methods.flatMap { it.models }.distinct().map { "use super::model::${Name.of(it).snakeCase()}::${Name.of(it).pascalCase()};" } +
            methods.map { "use super::rpc::${Name.of(it.rpcName).snakeCase()}::${it.rpcName};" }
        ).joinToString("\n")

    private fun client(methods: List<JsonRpcMethod>): String = """
        |/// Sends every rpc as a JSON-RPC 2.0 request through the transportation, and reads the values with the serialization.
        |pub struct JsonRpcClient<S: Serialization, T: Transportation> {
        |    pub serialization: S,
        |    pub transportation: T,
        |}
        |impl<S: Serialization, T: Transportation> JsonRpcClient<S, T> {
        |${methods.joinToString("\n") { it.clientFunction() }.prependIndent("    ")}
        |}
    """.trimMargin()

    private fun JsonRpcMethod.clientFunction(): String = """
        |pub async fn ${service.name.snakeCase().sanitize()}(&self${params.joinToString("") { ", ${it.identifier()}: ${it.type.rust()}" }}) -> Result<${returnType()}, $JSON_RPC_ERROR> {
        |    let mut params: Vec<(&str, String)> = Vec::new();
        |${params.joinToString("\n") { it.push() }.prependIndent("    ")}
        |    match call(&self.transportation, ${name.literal()}, params).await? {
        |        Reply::Result(json) => Ok(${resultValue("json.as_str()")}),
        |${error?.let { "        Reply::Error { code: $errorCode, data: Some(json), .. } => Ok($rpcName::Response::Error($rpcName::Error { value: ${it.decode("json.as_str()")}? })),\n" }.orEmpty()}        Reply::Error { code, message, data } => Err($JSON_RPC_ERROR::Response { code, message, data }),
        |    }
        |}
    """.trimMargin()

    private fun JsonRpcParam.push(): String = when {
        isNullable -> "if let Some(value) = &${identifier()} {\n    params.push((${wireName.literal()}, ${reference.encode("value")}));\n}"
        reference.isString() -> "params.push((${wireName.literal()}, ${reference.encode(identifier())}));"
        else -> "params.push((${wireName.literal()}, ${reference.encode("&${identifier()}")}));"
    }

    private fun JsonRpcMethod.resultValue(json: String): String = when (error) {
        null -> result.decodeResult(json)
        else -> "$rpcName::Response::Result($rpcName::Result { value: ${result.decodeResult(json)} })"
    }

    private fun Reference.decodeResult(json: String): String = when {
        this is Reference.Unit -> "()"
        isNullable -> "if $json == \"null\" { None } else { Some(${copy(isNullable = false).decode(json)}?) }"
        else -> "${decode(json)}?"
    }

    private fun JsonRpcMethod.returnType(): String = error?.let { "$rpcName::Response" } ?: result.convert().rust()

    private fun server(methods: List<JsonRpcMethod>): String = """
        |/// Answers JSON-RPC 2.0 requests, single or batched, by calling the service, which implements the `Service` of every rpc, and writes the values with the serialization.
        |pub struct JsonRpcServer<S: Serialization, Svc> {
        |    pub serialization: S,
        |    pub service: Svc,
        |}
        |impl<S: Serialization, Svc: ${methods.joinToString(" + ") { "${it.rpcName}::Service" }}> JsonRpcServer<S, Svc> {
        |    /// A batch is answered with the answers to its requests; a notification is not answered, and nor is a batch of them, so their response has status 204 and no body.
        |    pub async fn handle(&self, request: &RawRequest) -> RawResponse {
        |        let json = String::from_utf8_lossy(request.body.as_deref().unwrap_or_default()).trim().to_string();
        |        let answer = if !is_json(&json) {
        |            Some(write("null", &Answer::failure(PARSE_ERROR, "Parse error")))
        |        } else {
        |            match parts(&json, '[', ']') {
        |                None => self.reply(&json).await,
        |                Some(batch) if batch.is_empty() => Some(write("null", &Answer::failure(INVALID_REQUEST, "Invalid Request"))),
        |                Some(batch) => {
        |                    let mut replies = Vec::new();
        |                    for element in batch {
        |                        if let Some(reply) = self.reply(&element).await {
        |                            replies.push(reply);
        |                        }
        |                    }
        |                    Some(format!("[{}]", replies.join(","))).filter(|_| !replies.is_empty())
        |                }
        |            }
        |        };
        |        match answer {
        |            Some(json) => RawResponse { status_code: 200, headers: HashMap::from([(String::from("Content-Type"), vec![String::from("application/json")])]), body: Some(json.into_bytes()) },
        |            None => RawResponse { status_code: 204, headers: HashMap::new(), body: None },
        |        }
        |    }
        |
        |    /// The answer to one request, or None for a notification. A request that is invalid is answered with its id, or with null when it has none, or one that is no String, Number or Null.
        |    async fn reply(&self, json: &str) -> Option<String> {
        |        let Some(members) = members_of(json) else {
        |            return Some(write("null", &Answer::failure(INVALID_REQUEST, "Invalid Request")));
        |        };
        |        let id = members.get("id").cloned();
        |        let method = members.get("method").and_then(|method| decode_string(method).ok());
        |        let params = params(members.get("params"));
        |        let version = members.get("jsonrpc").and_then(|version| decode_string(version).ok());
        |        let (Some(method), Some(params), Some("2.0"), true) = (method, params, version.as_deref(), id.as_deref().is_none_or(is_id)) else {
        |            return Some(write(id.as_deref().filter(|id| is_id(id)).unwrap_or("null"), &Answer::failure(INVALID_REQUEST, "Invalid Request")));
        |        };
        |        let answer = self.dispatch(&method, &params).await;
        |        id.map(|id| write(&id, &answer))
        |    }
        |
        |    async fn dispatch(&self, method: &str, params: &Params) -> Answer {
        |        match method {
        |${methods.joinToString("\n") { it.serverBranch() }.prependIndent("            ")}
        |            _ => Answer::failure(METHOD_NOT_FOUND, "Method not found"),
        |        }
        |    }
        |}
    """.trimMargin()

    private fun JsonRpcMethod.serverBranch(): String = """
        |${name.literal()} => {
        |${params.joinToString("\n") { it.read() }.prependIndent("    ")}
        |    let response = $rpcName::Service::${service.name.snakeCase().sanitize()}(&self.service${params.joinToString("") { ", ${it.argument()}" }}).await;
        |${answer().prependIndent("    ")}
        |}
    """.trimMargin()

    private fun JsonRpcParam.read(): String = "let ${identifier()}: ${reference.convert().rust()} = match params.${if (isNullable) "optional" else "required"}(${wireName.literal()}, $position, |json| ${reference.copy(isNullable = false).decode("json")}) {\n    Ok(value) => value,\n    Err(answer) => return answer,\n};"

    /** The `Service` borrows a String, as the emitter writes it. */
    private fun JsonRpcParam.argument(): String = when {
        reference.isString() && isNullable -> "${identifier()}.as_deref()"
        reference.isString() -> "&${identifier()}"
        else -> identifier()
    }

    private fun JsonRpcMethod.answer(): String = when (val error = error) {
        null -> "Answer::Result(${result.encodeResult("response")})"
        else ->
            """
            |match response {
            |    $rpcName::Response::Result(response) => Answer::Result(${result.encodeResult("response.value")}),
            |    $rpcName::Response::Error(response) => Answer::Failure { code: $errorCode, message: String::from(${errorMessage.literal()}), data: Some(${error.encodeResult("response.value")}) },
            |}
            """.trimMargin()
    }

    /** The JSON of a value that can be None or unit, as a result or the data of an error. */
    private fun Reference.encodeResult(value: String): String = when {
        this is Reference.Unit -> "String::from(\"null\")"
        isNullable -> "match &$value { Some(value) => ${copy(isNullable = false).encode("value")}, None => String::from(\"null\") }"
        else -> encode("&$value")
    }

    /** Writes the [value], a reference, as JSON. */
    private fun Reference.encode(value: String): String = when {
        isString() -> "encode($value)"
        else -> "text(self.serialization.serialize_body($value, TypeId::of::<${valueType().rust()}>()))"
    }

    /** Reads the [json], a `&str`, as a `Result` of the value. */
    private fun Reference.decode(json: String): String = when {
        isString() -> "decode_string($json)"
        else -> "self.serialization.deserialize_body::<${valueType().rust()}>($json.as_bytes(), TypeId::of::<${valueType().rust()}>())"
    }

    private fun JsonRpcParam.identifier(): String = name.snakeCase().sanitize()

    private fun String.sanitize(): String = when (this) {
        "self", "Self", "super", "crate" -> "${this}_"
        in RustGenerator.reservedKeywords -> "r#$this"
        else -> this
    }

    private fun String.literal(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /** A type as the Rust emitter writes it; a type the emitter has borrowed, such as `&str`, keeps its name. */
    private fun Type.rust(): String = when (this) {
        is Type.Integer -> when (precision) {
            Precision.P32 -> "i32"
            Precision.P64 -> "i64"
        }
        is Type.Number -> when (precision) {
            Precision.P32 -> "f32"
            Precision.P64 -> "f64"
        }
        Type.Any -> "AnyValue"
        Type.String -> "String"
        Type.Boolean -> "bool"
        Type.Bytes -> "Vec<u8>"
        Type.Unit -> "()"
        Type.Wildcard -> "_"
        Type.Reflect -> "TypeId"
        is Type.Array -> "Vec<${elementType.rust()}>"
        is Type.Dict -> "HashMap<${keyType.rust()}, ${valueType.rust()}>"
        is Type.Custom -> name.referenceName() + generics.takeIf { it.isNotEmpty() }?.joinToString(", ", "<", ">") { it.rust() }.orEmpty()
        is Type.Nullable -> "Option<${type.rust()}>"
        is Type.IntegerLiteral -> "i32"
        is Type.StringLiteral -> "String"
        is Type.Function -> "Box<dyn Fn(${parameterTypes.joinToString(", ") { it.rust() }}) -> ${returnType.rust()}>"
    }

    private val RUNTIME = """
        |// The codes of the errors JSON-RPC 2.0 defines, as the specification writes them: https://www.jsonrpc.org/specification
        |pub const PARSE_ERROR: i64 = -32700;
        |pub const INVALID_REQUEST: i64 = -32600;
        |pub const METHOD_NOT_FOUND: i64 = -32601;
        |pub const INVALID_PARAMS: i64 = -32602;
        |pub const INTERNAL_ERROR: i64 = -32603;
        |
        |/// Why a JSON-RPC call did not give the response of the rpc.
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum JsonRpcError {
        |    /// The server answered with an error object, other than the error of the rpc itself. Its data is JSON.
        |    Response { code: i64, message: String, data: Option<String> },
        |    /// The request could not be sent, or the response could not be read.
        |    Wirespec(WirespecError),
        |}
        |impl From<WirespecError> for JsonRpcError {
        |    fn from(error: WirespecError) -> Self {
        |        JsonRpcError::Wirespec(error)
        |    }
        |}
        |impl std::fmt::Display for JsonRpcError {
        |    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        |        match self {
        |            JsonRpcError::Response { code, message, .. } => write!(f, "JSON-RPC error {}: {}", code, message),
        |            JsonRpcError::Wirespec(error) => write!(f, "{}", error),
        |        }
        |    }
        |}
        |impl std::error::Error for JsonRpcError {}
        |
        |/// What a server answered a request with: the JSON of a result, or an error object.
        |enum Reply {
        |    Result(String),
        |    Error { code: i64, message: String, data: Option<String> },
        |}
        |
        |/// Every request has id 1: a transportation answers a request with its own response, so the id needs no counter.
        |async fn call(transportation: &impl Transportation, method: &str, params: Vec<(&str, String)>) -> Result<Reply, JsonRpcError> {
        |    let params = if params.is_empty() {
        |        String::new()
        |    } else {
        |        format!(",\"params\":{{{}}}", params.iter().map(|(name, value)| format!("{}:{}", encode(name), value)).collect::<Vec<_>>().join(","))
        |    };
        |    let request = RawRequest {
        |        method: String::from("POST"),
        |        path: Vec::new(),
        |        queries: HashMap::new(),
        |        headers: HashMap::from([(String::from("Content-Type"), vec![String::from("application/json")])]),
        |        body: Some(format!("{{\"jsonrpc\":\"2.0\",\"method\":{}{},\"id\":1}}", encode(method), params).into_bytes()),
        |    };
        |    let response = transportation.transport(&request).await?;
        |    response
        |        .body
        |        .as_deref()
        |        .and_then(|body| reply_of(&String::from_utf8_lossy(body)))
        |        .ok_or_else(|| JsonRpcError::Wirespec(WirespecError::Deserialization(format!("The answer to {} is no JSON-RPC 2.0 response, status {}", method, response.status_code))))
        |}
        |
        |/// The reply of a response to a request with id 1: it has either a result or an error object with an integer code and a message, and the id of the request, or null when the server could not read it. None for any other JSON.
        |fn reply_of(json: &str) -> Option<Reply> {
        |    if !is_json(json) {
        |        return None;
        |    }
        |    let members = members_of(json)?;
        |    let version = members.get("jsonrpc").and_then(|version| decode_string(version).ok());
        |    let id = members.get("id").map(String::as_str);
        |    match (version.as_deref(), members.get("result"), members.get("error"), id) {
        |        (Some("2.0"), Some(result), None, Some("1")) => Some(Reply::Result(result.clone())),
        |        (Some("2.0"), None, Some(error), Some("1" | "null")) => {
        |            let error = members_of(error)?;
        |            Some(Reply::Error {
        |                code: error.get("code")?.parse().ok()?,
        |                message: decode_string(error.get("message")?).ok()?,
        |                data: error.get("data").cloned(),
        |            })
        |        }
        |        _ => None,
        |    }
        |}
        |
        |/// What a server answers a request with: the JSON of a result, or an error object.
        |enum Answer {
        |    Result(String),
        |    Failure { code: i64, message: String, data: Option<String> },
        |}
        |impl Answer {
        |    fn failure(code: i64, message: &str) -> Answer {
        |        Answer::Failure { code, message: String::from(message), data: None }
        |    }
        |}
        |
        |fn write(id: &str, answer: &Answer) -> String {
        |    let member = match answer {
        |        Answer::Result(json) => format!("\"result\":{}", json),
        |        Answer::Failure { code, message, data } => format!(
        |            "\"error\":{{\"code\":{},\"message\":{}{}}}",
        |            code,
        |            encode(message),
        |            data.as_ref().map(|data| format!(",\"data\":{}", data)).unwrap_or_default()
        |        ),
        |    };
        |    format!("{{\"jsonrpc\":\"2.0\",{},\"id\":{}}}", member, id)
        |}
        |
        |/// The params of a request, each as the JSON of its value, by name or by position.
        |struct Params {
        |    by_name: HashMap<String, String>,
        |    by_position: Vec<String>,
        |}
        |impl Params {
        |    fn get(&self, name: &str, position: usize) -> Option<&String> {
        |        self.by_name.get(name).or_else(|| self.by_position.get(position)).filter(|json| json.as_str() != "null")
        |    }
        |
        |    fn required<T>(&self, name: &str, position: usize, read: impl Fn(&str) -> Result<T, WirespecError>) -> Result<T, Answer> {
        |        let json = self.get(name, position).ok_or_else(|| Answer::failure(INVALID_PARAMS, "Invalid params"))?;
        |        read(json).map_err(|_| Answer::failure(INVALID_PARAMS, "Invalid params"))
        |    }
        |
        |    fn optional<T>(&self, name: &str, position: usize, read: impl Fn(&str) -> Result<T, WirespecError>) -> Result<Option<T>, Answer> {
        |        self.get(name, position).map(|json| read(json).map_err(|_| Answer::failure(INVALID_PARAMS, "Invalid params"))).transpose()
        |    }
        |}
        |
        |fn params(json: Option<&String>) -> Option<Params> {
        |    match json {
        |        None => Some(Params { by_name: HashMap::new(), by_position: Vec::new() }),
        |        Some(json) if json.starts_with('{') => members_of(json).map(|by_name| Params { by_name, by_position: Vec::new() }),
        |        Some(json) if json.starts_with('[') => parts(json, '[', ']').map(|by_position| Params { by_name: HashMap::new(), by_position }),
        |        Some(_) => None,
        |    }
        |}
        |
        |fn text(bytes: Vec<u8>) -> String {
        |    String::from_utf8_lossy(&bytes).into_owned()
        |}
    """.trimMargin()

    private val SCANNER = """
        |/// An id is a String, a Number or Null.
        |fn is_id(json: &str) -> bool {
        |    json == "null" || json.starts_with(['"', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'])
        |}
        |
        |/// The members of a JSON object, each as the JSON of its value; None when the JSON is no object.
        |fn members_of(json: &str) -> Option<HashMap<String, String>> {
        |    parts(json.trim(), '{', '}')?
        |        .into_iter()
        |        .map(|member| {
        |            let end = string_end(member.as_bytes(), 0)?;
        |            let value = member[end..].trim_start().strip_prefix(':')?;
        |            Some((decode_string(&member[..end]).ok()?, value.trim().to_string()))
        |        })
        |        .collect()
        |}
        |
        |/// The values of the JSON between open and close, split at the commas outside of a nested value. The JSON is valid.
        |fn parts(json: &str, open: char, close: char) -> Option<Vec<String>> {
        |    let inner = json.strip_prefix(open)?.strip_suffix(close)?;
        |    let bytes = inner.as_bytes();
        |    let mut parts = Vec::new();
        |    let (mut depth, mut start, mut index) = (0, 0, 0);
        |    while index < bytes.len() {
        |        match bytes[index] {
        |            b'"' => index = string_end(bytes, index)? - 1,
        |            b'{' | b'[' => depth += 1,
        |            b'}' | b']' => depth -= 1,
        |            b',' if depth == 0 => {
        |                parts.push(inner[start..index].trim().to_string());
        |                start = index + 1;
        |            }
        |            _ => {}
        |        }
        |        index += 1;
        |    }
        |    let last = inner[start..].trim().to_string();
        |    if !parts.is_empty() || !last.is_empty() {
        |        parts.push(last);
        |    }
        |    Some(parts)
        |}
        |
        |/// Whether the JSON is one JSON value, as RFC 8259 writes it. It reads nested values with a stack, so deep JSON cannot overflow.
        |fn is_json(json: &str) -> bool {
        |    let bytes = json.as_bytes();
        |    let mut closing = Vec::new();
        |    let mut index = 0;
        |    // A value comes next, the name of a member, or a comma or the end of an object or array.
        |    let mut next = b'v';
        |    loop {
        |        index = whitespace_end(bytes, index);
        |        let Some(&byte) = bytes.get(index) else {
        |            return next == b',' && closing.is_empty();
        |        };
        |        match (next, byte) {
        |            (b'v', b'{' | b'[') => {
        |                closing.push(if byte == b'{' { b'}' } else { b']' });
        |                index = whitespace_end(bytes, index + 1);
        |                next = if bytes.get(index) == closing.last() {
        |                    closing.pop();
        |                    index += 1;
        |                    b','
        |                } else if byte == b'{' {
        |                    b'k'
        |                } else {
        |                    b'v'
        |                };
        |            }
        |            (b'v', _) => match scalar_end(bytes, index) {
        |                Some(end) => {
        |                    index = end;
        |                    next = b',';
        |                }
        |                None => return false,
        |            },
        |            (b'k', _) => match string_end(bytes, index).map(|end| whitespace_end(bytes, end)) {
        |                Some(end) if bytes.get(end) == Some(&b':') => {
        |                    index = end + 1;
        |                    next = b'v';
        |                }
        |                _ => return false,
        |            },
        |            (_, b',') if !closing.is_empty() => {
        |                index += 1;
        |                next = if closing.last() == Some(&b'}') { b'k' } else { b'v' };
        |            }
        |            (_, _) if closing.last() == Some(&byte) => {
        |                closing.pop();
        |                index += 1;
        |            }
        |            _ => return false,
        |        }
        |    }
        |}
        |
        |fn scalar_end(bytes: &[u8], start: usize) -> Option<usize> {
        |    match bytes[start] {
        |        b'"' => string_end(bytes, start),
        |        _ if bytes[start..].starts_with(b"true") || bytes[start..].starts_with(b"null") => Some(start + 4),
        |        _ if bytes[start..].starts_with(b"false") => Some(start + 5),
        |        _ => number_end(bytes, start),
        |    }
        |}
        |
        |/// The index after the number at start, or None when there is none: a minus, an integer without leading zeros, a fraction and an exponent.
        |fn number_end(bytes: &[u8], start: usize) -> Option<usize> {
        |    let integer = if bytes.get(start) == Some(&b'-') { start + 1 } else { start };
        |    let mut index = match bytes.get(integer)? {
        |        b'0' => integer + 1,
        |        b'1'..=b'9' => digits_end(bytes, integer),
        |        _ => return None,
        |    };
        |    if bytes.get(index) == Some(&b'.') {
        |        index = Some(digits_end(bytes, index + 1)).filter(|end| *end > index + 1)?;
        |    }
        |    if matches!(bytes.get(index), Some(b'e' | b'E')) {
        |        let digits = if matches!(bytes.get(index + 1), Some(b'+' | b'-')) { index + 2 } else { index + 1 };
        |        index = Some(digits_end(bytes, digits)).filter(|end| *end > digits)?;
        |    }
        |    Some(index)
        |}
        |
        |fn digits_end(bytes: &[u8], start: usize) -> usize {
        |    start + bytes[start..].iter().take_while(|byte| byte.is_ascii_digit()).count()
        |}
        |
        |fn whitespace_end(bytes: &[u8], start: usize) -> usize {
        |    start + bytes[start..].iter().take_while(|byte| matches!(**byte, b' ' | b'\t' | b'\n' | b'\r')).count()
        |}
        |
        |/// The index after the JSON string at start, or None when there is none: a control character or an unknown escape ends none.
        |fn string_end(bytes: &[u8], start: usize) -> Option<usize> {
        |    if bytes.get(start) != Some(&b'"') {
        |        return None;
        |    }
        |    let mut index = start + 1;
        |    loop {
        |        match *bytes.get(index)? {
        |            b'"' => return Some(index + 1),
        |            byte if byte < 0x20 => return None,
        |            b'\\' => match *bytes.get(index + 1)? {
        |                b'"' | b'\\' | b'/' | b'b' | b'f' | b'n' | b'r' | b't' => index += 2,
        |                b'u' if bytes.get(index + 2..index + 6).is_some_and(|hex| hex.iter().all(u8::is_ascii_hexdigit)) => index += 6,
        |                _ => return None,
        |            },
        |            _ => index += 1,
        |        }
        |    }
        |}
        |
        |pub fn encode(value: &str) -> String {
        |    let mut json = String::from("\"");
        |    for char in value.chars() {
        |        match char {
        |            '"' => json.push_str("\\\""),
        |            '\\' => json.push_str("\\\\"),
        |            '\n' => json.push_str("\\n"),
        |            '\r' => json.push_str("\\r"),
        |            '\t' => json.push_str("\\t"),
        |            char if (char as u32) < 0x20 => json.push_str(&format!("\\u{:04x}", char as u32)),
        |            char => json.push(char),
        |        }
        |    }
        |    json.push('"');
        |    json
        |}
        |
        |pub fn decode_string(json: &str) -> Result<String, WirespecError> {
        |    let invalid = || WirespecError::Deserialization(format!("{} is no JSON string", json));
        |    if string_end(json.as_bytes(), 0) != Some(json.len()) {
        |        return Err(invalid());
        |    }
        |    let mut value = String::new();
        |    let mut chars = json[1..json.len() - 1].chars();
        |    while let Some(char) = chars.next() {
        |        if char != '\\' {
        |            value.push(char);
        |            continue;
        |        }
        |        match chars.next().ok_or_else(invalid)? {
        |            'b' => value.push('\u{8}'),
        |            'f' => value.push('\u{c}'),
        |            'n' => value.push('\n'),
        |            'r' => value.push('\r'),
        |            't' => value.push('\t'),
        |            'u' => {
        |                let unit = hex(&mut chars).ok_or_else(invalid)?;
        |                let code = if (0xD800..0xDC00).contains(&unit) {
        |                    let low = match (chars.next(), chars.next()) {
        |                        (Some('\\'), Some('u')) => hex(&mut chars).filter(|low| (0xDC00..0xE000).contains(low)).ok_or_else(invalid)?,
        |                        _ => return Err(invalid()),
        |                    };
        |                    0x10000 + ((unit - 0xD800) << 10) + (low - 0xDC00)
        |                } else {
        |                    unit
        |                };
        |                value.push(char::from_u32(code).ok_or_else(invalid)?);
        |            }
        |            escape => value.push(escape),
        |        }
        |    }
        |    Ok(value)
        |}
        |
        |fn hex(chars: &mut std::str::Chars) -> Option<u32> {
        |    let code: String = chars.take(4).collect();
        |    if code.len() == 4 { u32::from_str_radix(&code, 16).ok() } else { None }
        |}
    """.trimMargin()
}
