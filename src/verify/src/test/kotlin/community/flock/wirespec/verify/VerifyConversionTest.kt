package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyConversionTest : FunSpec({

    languages.values.forEach { lang ->
        test("conversion functions - $lang") {
            val isRust = lang.emitter is RustIrEmitter
            val isTypeScript = lang.emitter is TypeScriptIrEmitter
            val isPython = lang.emitter is PythonIrEmitter
            val endpointRef: Expression? = if (isRust) null else RawExpression("GetTodos")
            val requestType = if (isRust || isPython) Type.Custom("Request") else Type.Custom("GetTodos.Request")
            val response200Type = if (isRust || isPython) Type.Custom("Response200") else Type.Custom("GetTodos.Response200")
            val todoDtoType = Type.Custom("TodoDto")

            val testFile = file("ConversionTest") {
                when (lang.emitter) {
                    is JavaIrEmitter -> {
                        import("community.flock.wirespec.java", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }

                    is KotlinIrEmitter -> {
                        import("community.flock.wirespec.kotlin", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }

                    is TypeScriptIrEmitter -> {
                        import("./Wirespec", "Wirespec")
                        import("./endpoint/GetTodos", "GetTodos")
                        import("./model/TodoDto", "TodoDto")
                    }

                    is PythonIrEmitter -> {
                        import("community.flock.wirespec.generated.wirespec", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "Request")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "Response200")
                        import("community.flock.wirespec.generated.model.TodoDto", "TodoDto")
                    }

                    is RustIrEmitter -> {
                        // Rust imports are handled by run() use statements
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                }

                // Rust/Python: struct/class definitions must be at module level, not inside fn main()/def main()
                if (isRust) {
                    raw(rustSerializationDefs())
                }
                if (isPython) {
                    raw(pythonSerializationDefs())
                }

                main {
                    // Serialization mock
                    raw(serializationCode(lang))

                    // toRawRequest
                    when {
                        isRust -> {
                            raw("let request = Request::new()")
                            raw("let raw_request = to_raw_request(&serialization, request)")
                        }
                        isTypeScript -> {
                            raw("const request = GetTodos.request()")
                            raw("const rawRequest = GetTodos.toRawRequest(serialization, request)")
                        }
                        else -> {
                            assign("request", construct(requestType))
                            assign("rawRequest", functionCall("toRawRequest", receiver = endpointRef) {
                                arg("serialization", VariableReference("serialization"))
                                arg("request", VariableReference("request"))
                            })
                        }
                    }
                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("rawRequest"), Name.of("method")),
                            BinaryOp.Operator.EQUALS,
                            Literal("GET", Type.String)
                        ),
                        "Method should be GET"
                    )

                    // fromRawRequest
                    when {
                        isRust -> raw("let from_raw = from_raw_request(&serialization, raw_request)")
                        isTypeScript -> raw("const fromRaw = GetTodos.fromRawRequest(serialization, rawRequest)")
                        else -> assign("fromRaw", functionCall("fromRawRequest", receiver = endpointRef) {
                            arg("serialization", VariableReference("serialization"))
                            arg("request", VariableReference("rawRequest"))
                        })
                    }

                    // toRawResponse
                    when {
                        isRust -> {
                            raw("""let response200 = Response200::new(vec![TodoDto { description: "test".to_string() }])""")
                            raw("let raw_response = to_raw_response(&serialization, response200.into())")
                        }
                        isTypeScript -> {
                            raw("const response200 = GetTodos.response200({ body: [{ description: 'test' }] })")
                            raw("const rawResponse = GetTodos.toRawResponse(serialization, response200)")
                        }
                        else -> {
                            assign("response200", construct(response200Type) {
                                arg(
                                    "body", listOf(
                                        listOf(
                                            ConstructorStatement(
                                                todoDtoType,
                                                mapOf(Name.of("description") to Literal("test", Type.String))
                                            )
                                        ),
                                        todoDtoType
                                    )
                                )
                            })
                            assign("rawResponse", functionCall("toRawResponse", receiver = endpointRef) {
                                arg("serialization", VariableReference("serialization"))
                                arg("response", VariableReference("response200"))
                            })
                        }
                    }
                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("rawResponse"), Name.of("statusCode")),
                            BinaryOp.Operator.EQUALS,
                            Literal(200, Type.Integer())
                        ),
                        "Status should be 200"
                    )

                    // fromRawResponse
                    when {
                        isRust -> {
                            raw("let from_raw_resp = from_raw_response(&serialization, raw_response)")
                            raw("std::process::exit(0)")
                        }
                        isTypeScript -> raw("const fromRawResp = GetTodos.fromRawResponse(serialization, rawResponse)")
                        else -> assign("fromRawResp", functionCall("fromRawResponse", receiver = endpointRef) {
                            arg("serialization", VariableReference("serialization"))
                            arg("response", VariableReference("rawResponse"))
                        })
                    }
                }
            }

            lang.start(name = "conversion-test", fixture = CompileMinimalEndpointTest)
            lang.run(testFile)
        }
    }
})

private fun serializationCode(lang: Language): String = when (lang.emitter) {
    is JavaIrEmitter -> """Wirespec.Serialization serialization = new Wirespec.Serialization() {
    private final java.util.Map<String, Object> store = new java.util.HashMap<>();
    private String randomKey() { return java.util.UUID.randomUUID().toString(); }
    @Override public <T> byte[] serializeBody(T t, java.lang.reflect.Type type) { String key = randomKey(); store.put(key, t); return key.getBytes(); }
    @Override public <T> T deserializeBody(byte[] raw, java.lang.reflect.Type type) { return (T) store.get(new String(raw)); }
    @Override public <T> String serializePath(T t, java.lang.reflect.Type type) { return t.toString(); }
    @Override public <T> T deserializePath(String raw, java.lang.reflect.Type type) { return (T) raw; }
    @Override public <T> java.util.List<String> serializeParam(T value, java.lang.reflect.Type type) { return java.util.List.of(value.toString()); }
    @Override public <T> T deserializeParam(java.util.List<String> values, java.lang.reflect.Type type) { return (T) values.get(0); }
}"""

    is KotlinIrEmitter -> """val serialization = object : Wirespec.Serialization {
    private val store = mutableMapOf<String, Any>()
    private fun randomKey() = java.util.UUID.randomUUID().toString()
    override fun <T : Any> serializeBody(t: T, kType: kotlin.reflect.KType): ByteArray { val key = randomKey(); store[key] = t; return key.toByteArray() }
    override fun <T : Any> deserializeBody(raw: ByteArray, kType: kotlin.reflect.KType): T = store[String(raw)] as T
    override fun <T : Any> serializePath(t: T, kType: kotlin.reflect.KType): String = t.toString()
    override fun <T : Any> deserializePath(raw: String, kType: kotlin.reflect.KType): T = raw as T
    override fun <T : Any> serializeParam(value: T, kType: kotlin.reflect.KType): List<String> = listOf(value.toString())
    override fun <T : Any> deserializeParam(values: List<String>, kType: kotlin.reflect.KType): T = values[0] as T
}"""

    is TypeScriptIrEmitter -> """const store: Record<string, unknown> = {};
let counter = 0;
const serialization: Wirespec.Serialization = {
    serializeBody: <T>(t: T, type: Wirespec.Type): Uint8Array => { const key = String(counter++); store[key] = t; return new TextEncoder().encode(key); },
    deserializeBody: <T>(raw: Uint8Array, type: Wirespec.Type): T => store[new TextDecoder().decode(raw)] as T,
    serializePath: <T>(t: T, type: Wirespec.Type): string => String(t),
    deserializePath: <T>(raw: string, type: Wirespec.Type): T => raw as unknown as T,
    serializeParam: <T>(value: T, type: Wirespec.Type): string[] => [String(value)],
    deserializeParam: <T>(values: string[], type: Wirespec.Type): T => values[0] as unknown as T,
}"""

    is PythonIrEmitter -> "serialization = TestSerialization()"

    is RustIrEmitter -> "let serialization = MockSer"
    else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
}

private fun pythonSerializationDefs(): String = """
class TestSerialization(Wirespec.Serialization):
    def __init__(self):
        self.store = {}
        self.counter = 0
    def serializeBody(self, t, type):
        key = str(self.counter)
        self.counter += 1
        self.store[key] = t
        return key.encode()
    def deserializeBody(self, raw, type):
        return self.store[raw.decode()]
    def serializePath(self, t, type):
        return str(t)
    def deserializePath(self, raw, type):
        return raw
    def serializeParam(self, value, type):
        return [str(value)]
    def deserializeParam(self, values, type):
        return values[0]
""".trimStart()

private fun rustSerializationDefs(): String = """
struct MockSer;
impl BodySerializer for MockSer {
    fn serialize_body<T>(&self, t: &T, _type: &str) -> Vec<u8> {
        let size = std::mem::size_of::<T>();
        let ptr = t as *const T as *const u8;
        unsafe { std::slice::from_raw_parts(ptr, size).to_vec() }
    }
}
impl BodyDeserializer for MockSer {
    fn deserialize_body<T>(&self, raw: &[u8], _type: &str) -> T {
        unsafe { std::ptr::read(raw.as_ptr() as *const T) }
    }
}
impl PathSerializer for MockSer {
    fn serialize_path<T>(&self, _t: &T, _type: &str) -> String { String::new() }
}
impl PathDeserializer for MockSer {
    fn deserialize_path<T>(&self, _raw: &str, _type: &str) -> T { unreachable!() }
}
impl ParamSerializer for MockSer {
    fn serialize_param<T>(&self, _value: &T, _type: &str) -> Vec<String> { vec![] }
}
impl ParamDeserializer for MockSer {
    fn deserialize_param<T>(&self, _values: &[String], _type: &str) -> T { unreachable!() }
}
impl BodySerialization for MockSer {}
impl PathSerialization for MockSer {}
impl ParamSerialization for MockSer {}
impl Serializer for MockSer {}
impl Deserializer for MockSer {}
impl Serialization for MockSer {}
""".trimStart()
