package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyConversionTest : FunSpec({

    test("conversion functions - java") {
        val java = languages["java-17"]!!
        val testFile = file("ConversionTest") {
            import("community.flock.wirespec.java", "Wirespec")
            import("community.flock.wirespec.generated.endpoint", "GetTodos")
            import("community.flock.wirespec.generated.model", "TodoDto")
            main {
                raw(
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

                // toRawRequest
                assign("request", construct(type("GetTodos.Request")))
                assign("rawRequest", functionCall("toRawRequest", receiver = RawExpression("GetTodos")) {
                    arg("serialization", VariableReference("serialization"))
                    arg("request", VariableReference("request"))
                })
                assertThat(
                    FunctionCall(
                        receiver = FieldCall(VariableReference("rawRequest"), Name.of("method")),
                        name = Name.of("equals"),
                        arguments = mapOf(Name.of("arg") to Literal("GET", Type.String))
                    ),
                    "Method should be GET"
                )
                assertThat(
                    BinaryOp(
                        FieldCall(VariableReference("rawRequest"), Name.of("path")),
                        BinaryOp.Operator.EQUALS,
                        LiteralList(listOf(Literal("todos", Type.String)), Type.String)
                    ),
                    "Path should be [todos]"
                )

                // fromRawRequest
                assign("fromRaw", functionCall("fromRawRequest", receiver = RawExpression("GetTodos")) {
                    arg("serialization", VariableReference("serialization"))
                    arg("request", VariableReference("rawRequest"))
                })
                assertThat(RawExpression("fromRaw != null"), "fromRawRequest should return non-null")

                // toRawResponse
                assign("response200", construct(type("GetTodos.Response200")) {
                    arg(
                        "body", listOf(
                            listOf(
                                ConstructorStatement(
                                    Type.Custom("TodoDto"),
                                    mapOf(Name.of("description") to Literal("test", Type.String))
                                )
                            ),
                            Type.Custom("TodoDto")
                        )
                    )
                })
                assign("rawResponse", functionCall("toRawResponse", receiver = RawExpression("GetTodos")) {
                    arg("serialization", VariableReference("serialization"))
                    arg("response", VariableReference("response200"))
                })
                assertThat(RawExpression("rawResponse.statusCode() == 200"), "Status should be 200")
                assertThat(
                    FunctionCall(
                        receiver = FieldCall(VariableReference("rawResponse"), Name.of("body")),
                        name = Name.of("isPresent")
                    ),
                    "Body should be present"
                )

                // fromRawResponse
                assign("fromRawResp", functionCall("fromRawResponse", receiver = RawExpression("GetTodos")) {
                    arg("serialization", VariableReference("serialization"))
                    arg("response", VariableReference("rawResponse"))
                })
                assertThat(RawExpression("fromRawResp instanceof GetTodos.Response200"), "Should be Response200")
                assertThat(RawExpression("fromRawResp.status() == 200"), "Status should be 200")
            }
        }
        java.start(name = "conversion-test", fixture = CompileMinimalEndpointTest)
        java.run(testFile)
    }
})
