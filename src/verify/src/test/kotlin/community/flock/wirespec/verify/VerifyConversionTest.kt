package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.AssertStatement
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionBuilder
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.Main
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import io.kotest.core.spec.style.FunSpec
import community.flock.wirespec.ir.core.File as AstFile

class VerifyConversionTest : FunSpec({

    languages.forEach { (name, lang) ->
        test("conversion functions - $name") {
            val testFile = when (lang.emitter) {
                is RustIrEmitter -> rustConversionTest()
                else -> conversionTest(lang.emitter)
            }
            lang.start(name = "conversion-test", fixture = CompileMinimalEndpointTest)
            lang.run(testFile)
        }
    }
})

/** Create a VariableReference with preserved casing (for class/namespace names like GetTodos). */
private fun classRef(name: String) = VariableReference(Name(listOf(name), preserveCase = true))

/**
 * Builds the shared, language-neutral test body.
 * Uses direct IR constructors (not builder methods) for expressions inside assertThat
 * to avoid the builder's add-to-body side effects.
 */
private fun conversionTestBody(): List<Statement> {
    val builder = FunctionBuilder("test")
    val getTodos = classRef("GetTodos")
    builder.apply {
        assign("request", construct(type("Request")))

        assign("rawRequest", functionCall("toRawRequest", receiver = getTodos) {
            arg("serialization", VariableReference("serialization"))
            arg("request", VariableReference("request"))
        })

        assertThat(
            BinaryOp(
                FieldCall(VariableReference("rawRequest"), Name.of("method")),
                BinaryOp.Operator.EQUALS,
                Literal("GET", Type.String),
            ),
            "Method should be GET",
        )

        assertThat(
            BinaryOp(
                FieldCall(VariableReference("rawRequest"), Name.of("path")),
                BinaryOp.Operator.EQUALS,
                LiteralList(listOf(Literal("todos", Type.String)), Type.String),
            ),
            "Path should be [todos]",
        )

        assign("fromRaw", functionCall("fromRawRequest", receiver = getTodos) {
            arg("serialization", VariableReference("serialization"))
            arg("rawRequest", VariableReference("rawRequest"))
        })

        assertThat(
            BinaryOp(VariableReference("fromRaw"), BinaryOp.Operator.NOT_EQUALS, NullLiteral),
            "fromRawRequest should return non-null",
        )

        assign("response200", construct(type("Response200")) {
            arg("body", LiteralList(
                listOf(ConstructorStatement(Type.Custom("TodoDto"), mapOf(Name.of("description") to Literal("test", Type.String)))),
                Type.Custom("TodoDto"),
            ))
        })

        assign("rawResponse", functionCall("toRawResponse", receiver = getTodos) {
            arg("serialization", VariableReference("serialization"))
            arg("response200", VariableReference("response200"))
        })

        assertThat(
            BinaryOp(
                FieldCall(VariableReference("rawResponse"), Name.of("statusCode")),
                BinaryOp.Operator.EQUALS,
                Literal(200, Type.Integer()),
            ),
            "Status should be 200",
        )

        assertThat(
            BinaryOp(
                FieldCall(VariableReference("rawResponse"), Name.of("body")),
                BinaryOp.Operator.NOT_EQUALS,
                NullLiteral,
            ),
            "Body should be present",
        )

        assign("fromRawResp", functionCall("fromRawResponse", receiver = getTodos) {
            arg("serialization", VariableReference("serialization"))
            arg("rawResponse", VariableReference("rawResponse"))
        })

        switch(VariableReference("fromRawResp"), "r") {
            case(type("Response200")) {
                assertThat(
                    BinaryOp(
                        FieldCall(VariableReference("r"), Name.of("status")),
                        BinaryOp.Operator.EQUALS,
                        Literal(200, Type.Integer()),
                    ),
                    "Status should be 200",
                )
            }
            default {
                error(Literal("Should be Response200", Type.String))
            }
        }
    }
    return builder.build().body
}

private fun conversionTest(emitter: community.flock.wirespec.compiler.core.emit.Emitter): AstFile =
    when (emitter) {
        is JavaIrEmitter -> javaConversionFile()
        is KotlinIrEmitter -> kotlinConversionFile()
        is TypeScriptIrEmitter -> typeScriptConversionFile()
        is PythonIrEmitter -> pythonConversionFile()
        else -> error("Unsupported: ${emitter::class.simpleName}")
    }

// --- Java ---

private fun javaConversionFile(): AstFile {
    val mock = listOf<Statement>(
        RawExpression(
            """Wirespec.Serialization serialization = new Wirespec.Serialization() {
    private final java.util.Map<String, Object> store = new java.util.HashMap<>();
    private String randomKey() { return java.util.UUID.randomUUID().toString(); }
    @Override public <T> byte[] serializeBody(T t, java.lang.reflect.Type type) { String key = randomKey(); store.put(key, t); return key.getBytes(); }
    @Override public <T> T deserializeBody(byte[] raw, java.lang.reflect.Type type) { return (T) store.get(new String(raw)); }
    @Override public <T> String serializePath(T t, java.lang.reflect.Type type) { return t.toString(); }
    @Override public <T> T deserializePath(String raw, java.lang.reflect.Type type) { return (T) raw; }
    @Override public <T> java.util.List<String> serializeParam(T value, java.lang.reflect.Type type) { return java.util.List.of(value.toString()); }
    @Override public <T> T deserializeParam(java.util.List<String> values, java.lang.reflect.Type type) { return (T) values.get(0); }
}"""
        )
    )
    val body = mock + conversionTestBody()
    val raw = file("ConversionTest") {
        import("community.flock.wirespec.java", "Wirespec")
        import("community.flock.wirespec.generated.endpoint", "GetTodos")
        import("community.flock.wirespec.generated.model", "TodoDto")
        main {}
    }
    val fileWithBody = raw.copy(elements = raw.elements.map { if (it is Main) Main(body) else it })
    return javaTransform(fileWithBody)
}

private fun javaTransform(file: AstFile): AstFile = file.transform {
    renameType("Request", "GetTodos.Request")
    renameType("Response200", "GetTodos.Response200")
    apply(transformer(
        transformStatement = { stmt, tr ->
            if (stmt is AssertStatement && isBodyNotNullCheck(stmt.expression)) {
                AssertStatement(
                    FunctionCall(
                        receiver = FieldCall(VariableReference("rawResponse"), Name.of("body")),
                        name = Name.of("isPresent"),
                    ),
                    stmt.message,
                )
            } else {
                stmt.transformChildren(tr)
            }
        }
    ))
}

private fun isBodyNotNullCheck(expression: Expression): Boolean {
    if (expression !is BinaryOp) return false
    if (expression.operator != BinaryOp.Operator.NOT_EQUALS) return false
    if (expression.right !is NullLiteral) return false
    val left = expression.left
    return left is FieldCall && left.field == Name.of("body") &&
        left.receiver is VariableReference && (left.receiver as VariableReference).name == Name.of("rawResponse")
}

// --- Kotlin ---

private fun kotlinConversionFile(): AstFile {
    val mock = listOf<Statement>(
        RawExpression(
            """val serialization = object : Wirespec.Serialization {
    private val store = mutableMapOf<String, Any>()
    private fun randomKey() = java.util.UUID.randomUUID().toString()
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> serializeBody(t: T, type: kotlin.reflect.KType): ByteArray { val key = randomKey(); store[key] = t; return key.toByteArray() }
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeBody(raw: ByteArray, type: kotlin.reflect.KType): T = store[String(raw)] as T
    override fun <T : Any> serializePath(t: T, type: kotlin.reflect.KType): String = t.toString()
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializePath(raw: String, type: kotlin.reflect.KType): T = raw as T
    override fun <T : Any> serializeParam(value: T, type: kotlin.reflect.KType): List<String> = listOf(value.toString())
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeParam(values: List<String>, type: kotlin.reflect.KType): T = values[0] as T
}"""
        )
    )
    val body = mock + conversionTestBody()
    val raw = file("ConversionTest") {
        import("community.flock.wirespec.kotlin", "Wirespec")
        import("community.flock.wirespec.generated.endpoint", "GetTodos")
        import("community.flock.wirespec.generated.model", "TodoDto")
        main {}
    }
    val fileWithBody = raw.copy(elements = raw.elements.map { if (it is Main) Main(body) else it })
    return kotlinTransform(fileWithBody)
}

private fun kotlinTransform(file: AstFile): AstFile = file.transform {
    renameType("Request", "GetTodos.Request")
    renameType("Response200", "GetTodos.Response200")
    apply(transformer(
        transformExpression = { expr, tr ->
            // GetTodos.Request is a data object in Kotlin, not a constructor call
            if (expr is ConstructorStatement &&
                (expr.type as? Type.Custom)?.name == "GetTodos.Request" &&
                expr.namedArguments.isEmpty()
            ) {
                classRef("GetTodos.Request")
            } else {
                expr.transformChildren(tr)
            }
        }
    ))
}

// --- TypeScript ---

private fun typeScriptConversionFile(): AstFile {
    val mock = listOf<Statement>(
        RawExpression(
            """const store = new Map<string, unknown>()
const serialization: Wirespec.Serialization = {
    serializeBody<T>(t: T, type: Wirespec.Type): Uint8Array { const key = Math.random().toString(); store.set(key, t); return new TextEncoder().encode(key) },
    deserializeBody<T>(raw: Uint8Array, type: Wirespec.Type): T { return store.get(new TextDecoder().decode(raw)) as T },
    serializePath<T>(t: T, type: Wirespec.Type): string { return String(t) },
    deserializePath<T>(raw: string, type: Wirespec.Type): T { return raw as unknown as T },
    serializeParam<T>(value: T, type: Wirespec.Type): string[] { return [String(value)] },
    deserializeParam<T>(values: string[], type: Wirespec.Type): T { return values[0] as unknown as T },
}"""
        )
    )
    val body = mock + conversionTestBody()
    val raw = file("ConversionTest") {
        import("./Wirespec", "Wirespec")
        import("./endpoint/GetTodos", "GetTodos")
        main {}
    }
    val fileWithBody = raw.copy(elements = raw.elements.map { if (it is Main) Main(body) else it })
    return typeScriptTransform(fileWithBody)
}

private fun typeScriptTransform(file: AstFile): AstFile = file.transform {
    apply(transformer(
        transformExpression = { expr, tr ->
            when {
                // Request() → GetTodos.request() (factory function)
                expr is ConstructorStatement &&
                    (expr.type as? Type.Custom)?.name == "Request" &&
                    expr.namedArguments.isEmpty() ->
                    FunctionCall(receiver = classRef("GetTodos"), name = Name.of("request"))

                // Response200({body: [...]}) → GetTodos.response200({body: [...]})
                expr is ConstructorStatement &&
                    (expr.type as? Type.Custom)?.name == "Response200" -> {
                    val transformedArgs = expr.namedArguments.mapValues { tr.transformExpression(it.value) }
                    FunctionCall(
                        receiver = classRef("GetTodos"),
                        name = Name.of("response200"),
                        arguments = mapOf(Name.of("_") to ConstructorStatement(Type.Custom("__"), transformedArgs)),
                    )
                }

                // Array equality: wrap both sides in JSON.stringify
                expr is BinaryOp && (expr.left is LiteralList || expr.right is LiteralList) -> {
                    fun jsonStringify(e: Expression): FunctionCall = FunctionCall(
                        receiver = classRef("JSON"),
                        name = Name.of("stringify"),
                        arguments = mapOf(Name.of("_") to e),
                    )
                    BinaryOp(
                        jsonStringify(tr.transformExpression(expr.left)),
                        expr.operator,
                        jsonStringify(tr.transformExpression(expr.right)),
                    )
                }

                else -> expr.transformChildren(tr)
            }
        },
        transformStatement = { stmt, tr ->
            // Replace Switch with pattern matching → simple assert on .status
            if (stmt is Switch && stmt.cases.any { it.type != null }) {
                AssertStatement(
                    BinaryOp(
                        FieldCall(stmt.expression, Name.of("status")),
                        BinaryOp.Operator.EQUALS,
                        Literal(200, Type.Integer()),
                    ),
                    "Should be Response200",
                )
            } else {
                stmt.transformChildren(tr)
            }
        }
    ))
}

// --- Python ---

private fun pythonConversionFile(): AstFile {
    val mock = listOf<Statement>(RawExpression("serialization = MockSer()"))
    val body = mock + conversionTestBody()
    val raw = file("ConversionTest") {
        import("community.flock.wirespec.generated.wirespec", "Wirespec")
        import("community.flock.wirespec.generated.endpoint.GetTodos", "GetTodos")
        import("community.flock.wirespec.generated.endpoint.GetTodos", "Request")
        import("community.flock.wirespec.generated.endpoint.GetTodos", "Response200")
        import("community.flock.wirespec.generated.model.TodoDto", "TodoDto")
        raw(
            """class MockSer(Wirespec.Serialization):
    def __init__(self):
        self._store = {}
        self._counter = 0
    def serializeBody(self, t, type):
        key = str(self._counter); self._counter += 1; self._store[key] = t; return key.encode()
    def deserializeBody(self, raw, type):
        return self._store[raw.decode()]
    def serializePath(self, t, type):
        return str(t)
    def deserializePath(self, raw, type):
        return raw
    def serializeParam(self, value, type):
        return [str(value)]
    def deserializeParam(self, values, type):
        return values[0]"""
        )
        main {}
    }
    return raw.copy(elements = raw.elements.map { if (it is Main) Main(body) else it })
}

// --- Rust (kept as raw) ---

private fun rustConversionTest(): AstFile = file("ConversionTest") {
    main {
        raw("use generated::wirespec::{BodySerializer, BodyDeserializer, PathSerializer, PathDeserializer, ParamSerializer, ParamDeserializer, Serializer, Deserializer, Serialization}")
        raw("use generated::endpoint::get_todos::{Request, Response, Response200, GetTodos}")
        raw("use generated::model::todo_dto::TodoDto")
        raw("struct MockSer")
        raw("impl BodySerializer for MockSer { fn serialize_body<T>(&self, _t: &T, _type: &str) -> Vec<u8> { b\"mock\".to_vec() } }")
        raw(
            """impl BodyDeserializer for MockSer { fn deserialize_body<T>(&self, _raw: &[u8], _type: &str) -> T {
    let v: Vec<TodoDto> = vec![TodoDto { description: String::from("test") }];
    unsafe { let r = std::ptr::read(&v as *const Vec<TodoDto> as *const T); std::mem::forget(v); r }
} }"""
        )
        raw("impl PathSerializer for MockSer { fn serialize_path<T>(&self, _t: &T, _type: &str) -> String { String::new() } }")
        raw("impl PathDeserializer for MockSer { fn deserialize_path<T>(&self, _raw: &str, _type: &str) -> T { unimplemented!() } }")
        raw("impl ParamSerializer for MockSer { fn serialize_param<T>(&self, _value: &T, _type: &str) -> Vec<String> { vec![] } }")
        raw("impl ParamDeserializer for MockSer { fn deserialize_param<T>(&self, _values: &[String], _type: &str) -> T { unimplemented!() } }")
        raw("impl Serializer for MockSer {}")
        raw("impl Deserializer for MockSer {}")
        raw("impl Serialization for MockSer {}")
        raw("let ser = MockSer")
        raw("let request = Request::new()")
        raw("let raw_request = GetTodos::to_raw_request(&ser, request)")
        raw("assert_eq!(raw_request.method, \"GET\", \"Method should be GET\")")
        raw("assert_eq!(raw_request.path, vec![String::from(\"todos\")], \"Path should be [todos]\")")
        raw("let from_raw = GetTodos::from_raw_request(&ser, raw_request)")
        raw("let _ = from_raw")
        raw("let response200 = Response::Response200(Response200::new(vec![TodoDto { description: String::from(\"test\") }]))")
        raw("let raw_response = GetTodos::to_raw_response(&ser, response200)")
        raw("assert_eq!(raw_response.status_code, 200, \"Status should be 200\")")
        raw("assert!(raw_response.body.is_some(), \"Body should be present\")")
        raw("let from_raw_resp = GetTodos::from_raw_response(&ser, raw_response)")
        raw("match from_raw_resp { Response::Response200(r) => assert_eq!(r.status, 200, \"Status should be 200\"), _ => panic!(\"Should be Response200\") }")
    }
}
