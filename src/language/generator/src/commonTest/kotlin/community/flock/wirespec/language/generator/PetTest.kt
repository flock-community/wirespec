package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.NullCheck
import community.flock.wirespec.language.core.NullLiteral
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.file
import kotlin.test.Test
import kotlin.test.assertTrue

class PetTest {

    @Test
    fun getTodos() {
        val getTodos = file("GetTodos") {
            `package`("community.flock.wirespec.generated.examples.spring.endpoint")

            import("community.flock.wirespec.generated.examples.spring.model", "Todo")
            import("community.flock.wirespec.generated.examples.spring.model", "Error")

            import("community.flock.wirespec.java", "Wirespec")

            static("GetTodos", type("Wirespec.Endpoint")) {
                struct("Path") {
                    implements(type("Wirespec.Path"))
                }

                struct("Queries") {
                    implements(type("Wirespec.Queries"))
                    field("done", boolean.nullable())
                }

                struct("RequestHeaders") {
                    implements(type("Wirespec.Request.Headers"))
                }

                struct("Request") {
                    implements(type("Wirespec.Request", Type.Unit))
                    field("path", type("Path"))
                    field("method", type("Wirespec.Method"))
                    field("queries", type("Queries"))
                    field("headers", type("RequestHeaders"))
                    field("body", type("Void"))
                    constructo {
                        arg("done", boolean.nullable())
                        assign("path", construct(type("Path")))
                        assign("method", RawExpression("Wirespec.Method.GET"))
                        assign(
                            "queries",
                            construct(type("Queries")) {
                                arg("done", RawExpression("done"))
                            },
                        )
                        assign("headers", construct(type("RequestHeaders")))
                    }
                }

                union("Response", extends = type("Wirespec.Response")) {
                    member("Response2XX")
                    member("Response4XX")
                }

                union("Response2XX") {
                    member("Response200")
                }

                union("Response4XX") {
                    member("Response404")
                }

                struct("Response200") {
                    field("status", integer)
                    field("headers", type("Headers"))
                    field("body", list(type("Todo")))
                    struct("Headers") {
                        implements(type("Wirespec.Response.Headers"))
                    }
                    constructo {
                        arg("body", list(type("Todo")))
                        assign("status", literal(200))
                        assign("headers", construct(type("Headers")))
                        assign("body", RawExpression("body"))
                    }
                }

                struct("Response404") {
                    field("status", integer)
                    field("headers", type("Headers"))
                    field("body", type("Error"))
                    struct("Headers") {
                        implements(type("Wirespec.Response.Headers"))
                    }
                    constructo {
                        arg("body", type("Error"))
                        assign("status", literal(404))
                        assign("headers", construct(type("Headers")))
                        assign("body", RawExpression("body"))
                    }
                }

                `interface`("Handler") {
                    extends(type("Wirespec.Handler"))
                    function("toRequest") {
                        returnType(type("Wirespec.RawRequest"))
                        arg("serialization", type("Wirespec.Serializer"))
                        arg("request", type("Request"))
                        returns(
                            construct(type("Wirespec.RawRequest")) {
                                arg("method", functionCall("request.method.name"))
                                arg("path", listOf(kotlin.collections.listOf(literal("todos")), string))
                                arg(
                                    "queries",
                                    mapOf(
                                        mapOf(
                                            "done" to NullCheck(
                                                expression = RawExpression("request.queries.done"),
                                                body = functionCall("serialization.serializeParam") {
                                                    arg("value", RawExpression("it"))
                                                    arg(
                                                        "type",
                                                        functionCall("Wirespec.getType") {
                                                            arg("type", RawExpression("Boolean.class"))
                                                            arg("container", RawExpression("java.util.Optional.class"))
                                                        },
                                                    )
                                                },
                                                alternative = emptyList(string),
                                            ),
                                        ),
                                        string,
                                        string,
                                    ),
                                )
                                arg("headers", emptyMap(string, string))
                                arg("body", NullLiteral)
                            },
                        )
                    }

                    function("fromRequest") {
                        returnType(type("Request"))
                        arg("serialization", type("Wirespec.Deserializer"))
                        arg("request", type("Wirespec.RawRequest"))
                        returns(
                            construct(type("Request")) {
                                arg(
                                    "done",
                                    NullCheck(
                                        expression = functionCall("request.queries().get") {
                                            arg("key", literal("done"))
                                        },
                                        body = functionCall("serialization.deserializeParam") {
                                            arg("value", RawExpression("it"))
                                            arg(
                                                "type",
                                                functionCall("Wirespec.getType") {
                                                    arg("type", RawExpression("Boolean.class"))
                                                    arg("container", RawExpression("java.util.Optional.class"))
                                                },
                                            )
                                        },
                                        alternative = NullLiteral,
                                    ),
                                )
                            },
                        )
                    }

                    function("fromResponse") {
                        returnType(type("Response"))
                        arg("serialization", type("Wirespec.Deserializer"))
                        arg("response", type("Wirespec.RawResponse"))
                        switch(functionCall("response.statusCode")) {
                            case(literal(200)) {
                                returns(
                                    construct(type("Response200")) {
                                        arg(
                                            "body",
                                            functionCall("serialization.deserializeBody") {
                                                arg("body", RawExpression("response.body()"))
                                                arg(
                                                    "type",
                                                    functionCall("Wirespec.getType") {
                                                        arg("type", RawExpression("Todo.class"))
                                                        arg("container", RawExpression("java.util.List.class"))
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            case(literal(404)) {
                                returns(
                                    construct(type("Response404")) {
                                        arg(
                                            "body",
                                            functionCall("serialization.deserializeBody") {
                                                arg("body", functionCall("response.body"))
                                                arg(
                                                    "type",
                                                    functionCall("Wirespec.getType") {
                                                        arg("type", RawExpression("Error.class"))
                                                        arg("container", NullLiteral)
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            default {
                                error(RawExpression("\"Cannot match response with status: \" + response.statusCode()"))
                            }
                        }
                    }

                    function("toResponse") {
                        returnType(type("Wirespec.RawResponse"))
                        arg("serialization", type("Wirespec.Serializer"))
                        arg("response", type("Response"))
                        switch(RawExpression("response"), "r") {
                            case(type("Response200")) {
                                returns(
                                    construct(type("Wirespec.RawResponse")) {
                                        arg("statusCode", functionCall("r.status"))
                                        arg("headers", RawExpression("java.util.Collections.emptyMap()"))
                                        arg(
                                            "body",
                                            functionCall("serialization.serializeBody") {
                                                arg("body", RawExpression("r.body"))
                                                arg(
                                                    "type",
                                                    functionCall("Wirespec.getType") {
                                                        arg("type", RawExpression("Todo.class"))
                                                        arg("container", RawExpression("java.util.List.class"))
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            case(type("Response404")) {
                                returns(
                                    construct(type("Wirespec.RawResponse")) {
                                        arg("statusCode", functionCall("r.status"))
                                        arg("headers", RawExpression("java.util.Collections.emptyMap()"))
                                        arg(
                                            "body",
                                            functionCall("serialization.serializeBody") {
                                                arg("body", RawExpression("r.body"))
                                                arg(
                                                    "type",
                                                    functionCall("Wirespec.getType") {
                                                        arg("type", RawExpression("Error.class"))
                                                        arg("container", NullLiteral)
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            default {
                                error(RawExpression("\"Cannot match response with status: \" + response.status()"))
                            }
                        }
                    }

                    asyncFunction("getTodos") {
                        returnType(type("Response"))
                    }
                }
            }
        }

        val output = JavaGenerator.generate(getTodos)

        println(output)

        assertTrue(output.contains("package community.flock.wirespec.generated.examples.spring.endpoint;"))
        assertTrue(output.contains("import community.flock.wirespec.java.Wirespec;"))
        assertTrue(output.contains("import community.flock.wirespec.generated.examples.spring.model.Todo;"))
        assertTrue(output.contains("import community.flock.wirespec.generated.examples.spring.model.Error;"))
        // Verify the imports are correctly composed from path + type

        assertTrue(output.contains("public sealed interface Response extends Wirespec.Response permits Response2XX, Response4XX {}"))
        assertTrue(output.contains("public sealed interface Response2XX extends Response permits Response200 {}"))
        assertTrue(output.contains("public sealed interface Response4XX extends Response permits Response404 {}"))

        assertTrue(output.contains("public static record Response200 ("))
        assertTrue(output.contains("java.util.List<Todo> body"))
        assertTrue(output.contains(") implements Response2XX {"))
        assertTrue(output.contains("public static record Headers () implements Wirespec.Response.Headers {"))

        assertTrue(output.contains("public static record Response404 ("))
        assertTrue(output.contains("Error body"))
        assertTrue(output.contains(") implements Response4XX {"))

        assertTrue(output.contains("public static record Queries ("))
        assertTrue(output.contains("java.util.Optional<Boolean> done"))
        assertTrue(output.contains(") implements Wirespec.Queries {"))
        assertTrue(output.contains("public static record Request ("))
        assertTrue(output.contains(") implements Wirespec.Request<Void> {"))
        assertTrue(output.contains("public Request(java.util.Optional<Boolean> done) {"))
        assertTrue(output.contains("request.method.name()"))
        assertTrue(output.contains("java.util.List.of(\"todos\")"))
        assertTrue(output.contains("java.util.Map.ofEntries(java.util.Map.entry(\"done\", java.util.Optional.ofNullable(request.queries.done).map(it -> serialization.serializeParam(it, Wirespec.getType(Boolean.class, java.util.Optional.class))).orElse(java.util.List.of())))"))
        assertTrue(output.contains("java.util.Collections.emptyMap()"))
        assertTrue(output.contains("null"))
        assertTrue(output.contains("public interface Handler extends Wirespec.Handler {"))
        assertTrue(output.contains("public java.util.concurrent.CompletableFuture<Response> getTodos();"))
        assertTrue(output.contains("public default Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response response) {"))
        assertTrue(output.contains("if (response instanceof Response200 r) {"))
        assertTrue(output.contains("else if (response instanceof Response404 r) {"))
        assertTrue(output.contains("return new Wirespec.RawResponse("))
        assertTrue(output.contains("else {"))
    }
}
