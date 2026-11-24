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
        |${error?.let { "        Reply::Error { code: SERVER_ERROR, data: Some(json), .. } => Ok($rpcName::Response::Error($rpcName::Error { value: ${it.decode("json.as_str()")}? })),\n" }.orEmpty()}        Reply::Error { code, message, .. } => Err($JSON_RPC_ERROR::Response { code, message }),
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
        |    /// A batch is answered with the answers to its requests; a notification is not answered, so its response has status 204 and no body.
        |    pub async fn handle(&self, request: &RawRequest) -> RawResponse {
        |        let json = String::from_utf8_lossy(request.body.as_deref().unwrap_or_default()).trim().to_string();
        |        let answer = if !json.starts_with('[') {
        |            self.reply(&json).await
        |        } else {
        |            match parts(&json, '[', ']') {
        |                None => Some(write("null", &Answer::failure(PARSE_ERROR, "Parse error"))),
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
        |    async fn reply(&self, json: &str) -> Option<String> {
        |        let Some(members) = members_of(json) else {
        |            let answer = if json.is_empty() || json.starts_with('{') { Answer::failure(PARSE_ERROR, "Parse error") } else { Answer::failure(INVALID_REQUEST, "Invalid Request") };
        |            return Some(write("null", &answer));
        |        };
        |        let id = members.get("id").cloned();
        |        let method = members.get("method").and_then(|method| decode_string(method).ok());
        |        let params = params(members.get("params"));
        |        let (Some(method), Some(params), Some("\"2.0\"")) = (method, params, members.get("jsonrpc").map(String::as_str)) else {
        |            return Some(write(id.as_deref().unwrap_or("null"), &Answer::failure(INVALID_REQUEST, "Invalid Request")));
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
            |    $rpcName::Response::Error(response) => Answer::Failure { code: SERVER_ERROR, message: String::from("Server error"), data: Some(${error.encodeResult("response.value")}) },
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
        |pub const PARSE_ERROR: i64 = -32700;
        |pub const INVALID_REQUEST: i64 = -32600;
        |pub const METHOD_NOT_FOUND: i64 = -32601;
        |pub const INVALID_PARAMS: i64 = -32602;
        |pub const INTERNAL_ERROR: i64 = -32603;
        |/// The code of the error of an rpc itself, which is the data of the error object.
        |pub const SERVER_ERROR: i64 = -32000;
        |
        |/// Why a JSON-RPC call did not give the response of the rpc.
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum JsonRpcError {
        |    /// The server answered with an error object, other than the error of the rpc itself.
        |    Response { code: i64, message: String },
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
        |            JsonRpcError::Response { code, message } => write!(f, "JSON-RPC error {}: {}", code, message),
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
        |    let invalid = |problem: &str| WirespecError::Deserialization(format!("The JSON-RPC response to {} {}, status {}", method, problem, response.status_code));
        |    let members = response.body.as_deref().and_then(|body| members_of(&String::from_utf8_lossy(body))).ok_or_else(|| invalid("is no object"))?;
        |    match members.get("error").filter(|error| error.as_str() != "null") {
        |        None => Ok(Reply::Result(members.get("result").cloned().ok_or_else(|| invalid("has no result"))?)),
        |        Some(error) => {
        |            let error = members_of(error).ok_or_else(|| invalid("has an error that is no object"))?;
        |            Ok(Reply::Error {
        |                code: error.get("code").and_then(|code| code.parse().ok()).unwrap_or(INTERNAL_ERROR),
        |                message: error.get("message").and_then(|message| decode_string(message).ok()).unwrap_or_default(),
        |                data: error.get("data").cloned(),
        |            })
        |        }
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
        |/// The members of a JSON object, each as the JSON of its value; None when the JSON is no object.
        |fn members_of(json: &str) -> Option<HashMap<String, String>> {
        |    parts(json.trim(), '{', '}')?
        |        .into_iter()
        |        .map(|member| {
        |            let end = string_end(&member)?;
        |            let value = member[end..].trim_start().strip_prefix(':')?;
        |            Some((decode_string(&member[..end]).ok()?, value.trim().to_string()))
        |        })
        |        .collect()
        |}
        |
        |/// The JSON between open and close split at the commas outside of a nested value; None when it is no such JSON.
        |fn parts(json: &str, open: char, close: char) -> Option<Vec<String>> {
        |    let inner = json.strip_prefix(open)?.strip_suffix(close)?;
        |    let mut parts = Vec::new();
        |    let (mut depth, mut in_string, mut escaped, mut start) = (0, false, false, 0);
        |    for (index, char) in inner.char_indices() {
        |        match char {
        |            _ if in_string && escaped => escaped = false,
        |            '\\' if in_string => escaped = true,
        |            '"' if in_string => in_string = false,
        |            _ if in_string => {}
        |            '"' => in_string = true,
        |            '{' | '[' => depth += 1,
        |            '}' | ']' => {
        |                depth -= 1;
        |                if depth < 0 {
        |                    return None;
        |                }
        |            }
        |            ',' if depth == 0 => {
        |                parts.push(inner[start..index].trim().to_string());
        |                start = index + 1;
        |            }
        |            _ => {}
        |        }
        |    }
        |    let last = inner[start..].trim().to_string();
        |    if in_string || depth != 0 {
        |        return None;
        |    }
        |    if parts.is_empty() && last.is_empty() {
        |        return Some(parts);
        |    }
        |    parts.push(last);
        |    Some(parts).filter(|parts| parts.iter().all(|part| !part.is_empty()))
        |}
        |
        |/// The index after the JSON string the JSON starts with, or None when it starts with none.
        |fn string_end(json: &str) -> Option<usize> {
        |    if !json.starts_with('"') {
        |        return None;
        |    }
        |    let mut escaped = false;
        |    for (index, char) in json.char_indices().skip(1) {
        |        match char {
        |            _ if escaped => escaped = false,
        |            '\\' => escaped = true,
        |            '"' => return Some(index + 1),
        |            _ => {}
        |        }
        |    }
        |    None
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
        |    if string_end(json) != Some(json.len()) {
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
