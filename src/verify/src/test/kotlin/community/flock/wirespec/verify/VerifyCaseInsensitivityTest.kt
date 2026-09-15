package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.emitters.rust.RustEmitter
import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.fieldCall
import community.flock.wirespec.compiler.core.ir.FunctionCall
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.LiteralList
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.NullableEmpty
import community.flock.wirespec.compiler.core.ir.NullableGet
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.TypeDescriptor
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import io.kotest.core.spec.style.FunSpec

/**
 * Tests that HTTP headers are deserialized case-insensitively (RFC 7230)
 * while query parameters remain case-sensitive (RFC 3986).
 *
 * Uses CompileFullEndpointTest fixture which has:
 *   endpoint PutTodo PUT PotentialTodoDto /todos/{id: String}
 *       ?{done: Boolean, name: String?}
 *       #{token: Token, `Refresh-Token`: Token?} -> {
 *       200 -> TodoDto
 *       201 -> TodoDto #{token: Token, refreshToken: Token?}
 *       500 -> Error
 *   }
 *
 * Constructs a RawRequest with differently-cased header keys ("TOKEN", "refresh-token")
 * and verifies fromRawRequest deserializes them correctly despite case mismatch.
 */
class VerifyCaseInsensitivityTest : FunSpec({

    languages.values.forEach { lang ->
        test("header case insensitivity - $lang") {
            val isRust = lang.emitter is RustEmitter
            val endpointRef: Expression = RawExpression("PutTodo")

            val testFile = file("CaseInsensitivityTest") {
                endpointImports(lang, CompileFullEndpointTest)

                main(statics = {
                        raw(serializationCode(lang, CompileFullEndpointTest))
                }) {

                    assign("rawRequest", construct(type(if (isRust) "RawRequest" else "Wirespec.RawRequest")) {
                        arg("method", literal("PUT"))
                        arg("path", listOf(
                            listOf(Literal("todos", Type.String), Literal("123", Type.String)),
                            Type.String
                        ))
                        arg("queries", mapOf(
                            mapOf(
                                "done" to LiteralList(listOf(Literal("true", Type.String)), Type.String),
                                "name" to LiteralList(listOf(Literal("test", Type.String)), Type.String)
                            ),
                            Type.String, Type.Array(Type.String)
                        ))
                        arg("headers", mapOf(
                            mapOf(
                                "TOKEN" to LiteralList(listOf(Literal("issValue", Type.String)), Type.String),
                                "refresh-token" to LiteralList(listOf(Literal("refreshIssValue", Type.String)), Type.String)
                            ),
                            Type.String, Type.Array(Type.String)
                        ))
                        arg("body", NullableOf(
                            FunctionCall(
                                receiver = VariableReference(Name.of("serialization")),
                                name = Name("serialize", "Body"),
                                typeArguments = listOf(Type.Custom("PotentialTodoDto")),
                                arguments = mapOf(
                                    Name.of("value") to ConstructorStatement(
                                        Type.Custom("PotentialTodoDto"),
                                        mapOf(
                                            Name.of("name") to Literal("test", Type.String),
                                            Name.of("done") to Literal(true, Type.Boolean),
                                        ),
                                    ),
                                    Name.of("type") to TypeDescriptor(Type.Custom("PotentialTodoDto")),
                                ),
                            ),
                        ))
                    })

                    // fromRawRequest call
                    assign("fromRaw", functionCall("fromRawRequest", receiver = if (isRust) null else endpointRef) {
                        arg("serialization", if (isRust) with(RustEmitter) { VariableReference("serialization").borrow() } else VariableReference("serialization"))
                        arg("rawRequest", VariableReference("rawRequest"))
                    })

                    // Assert token.iss matches despite case mismatch
                    assertThat(
                        BinaryOp(
                            VariableReference("fromRaw").fieldCall("headers").fieldCall("token").fieldCall("iss"),
                            BinaryOp.Operator.EQUALS,
                            Literal("issValue", Type.String)
                        ),
                        "Header 'token' should match 'TOKEN' case-insensitively"
                    )

                    // Assert refreshToken is present
                    assertThat(
                        BinaryOp(
                            VariableReference("fromRaw").fieldCall("headers").fieldCall("refreshToken"),
                            BinaryOp.Operator.NOT_EQUALS,
                            NullableEmpty
                        ),
                        "Header 'Refresh-Token' should match 'refresh-token' case-insensitively"
                    )

                    // Assert refreshToken.iss == "refreshIssValue" — unwrap then access
                    assertThat(
                        BinaryOp(
                            NullableGet(VariableReference("fromRaw").fieldCall("headers").fieldCall("refreshToken")).fieldCall("iss"),
                            BinaryOp.Operator.EQUALS,
                            Literal("refreshIssValue", Type.String)
                        ),
                        "Header 'Refresh-Token' value should be correct"
                    )

                }
            }

            lang.start(name = "case-insensitivity-test", fixture = CompileFullEndpointTest)
            lang.run(testFile)
        }
    }
})
