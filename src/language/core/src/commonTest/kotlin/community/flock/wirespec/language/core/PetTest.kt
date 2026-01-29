package com.wirelang

import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.generator.JavaGenerator
import kotlin.test.Test
import kotlin.test.assertTrue

class PetTest {

    @Test
    fun getTodos() {
        val getTodos = file("GetTodos") {
            `package`("community.flock.wirespec.generated.examples.spring.endpoint")

            import("community.flock.wirespec.generated.examples.spring.model.Todo")
            import("community.flock.wirespec.generated.examples.spring.model.Error")

            import("community.flock.wirespec.java.Wirespec")

            static("GetTodos", type("Wirespec.Endpoint")) {
                struct("Path", interfaces = type("Wirespec.Path"))

                struct("Queries", interfaces = type("Wirespec.Queries")) {
                    field("done", boolean.nullable())
                }

                struct("RequestHeaders", interfaces = type("Wirespec.Request.Headers")) {}

                struct("Request", interfaces = type("Wirespec.Request", Type.Unit)) {
                    field("path", type("Path"))
                    field("method", type("Wirespec.Method"))
                    field("queries", type("Queries"))
                    field("headers", type("RequestHeaders"))
                    field("body", type("Void"))
                    constructo {
                        arg("done", boolean.nullable())
                        assign("path", construct(type("Path")))
                        assign("method", "Wirespec.Method.GET")
                        assign(
                            "queries",
                            construct(type("Queries")) {
                                arg("done", "done")
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
                    struct("Headers", interfaces = type("Wirespec.Response.Headers"))
                    constructo {
                        arg("body", list(type("Todo")))
                        assign("status", literal(200))
                        assign("headers", construct(type("Headers")))
                        assign("body", "body")
                    }
                }

                struct("Response404") {
                    field("status", integer)
                    field("headers", type("Headers"))
                    field("body", type("Error"))
                    struct("Headers", interfaces = type("Wirespec.Response.Headers"))
                    constructo {
                        arg("body", type("Error"))
                        assign("status", literal(404))
                        assign("headers", construct(type("Headers")))
                        assign("body", "body")
                    }
                }

                `interface`("Handler", type("Wirespec.Handler")) {

                    function("toRequest", type("Wirespec.RawRequest")) {
                        arg("serialization", type("Wirespec.Serializer"))
                        arg("request", type("Request"))
                        returns(
                            construct(type("Wirespec.RawRequest")) {
                                arg("method", call("request.method.name"))
                                arg("path", listOf(kotlin.collections.listOf(literal("todos")), string))
                                arg(
                                    "queries",
                                    mapOf(
                                        mapOf(
                                            "done" to call("serialization.serializeParam") {
                                                arg("value", "request.queries.done")
                                                arg(
                                                    "type",
                                                    call("Wirespec.getType") {
                                                        arg("type", "Boolean.class")
                                                        arg("container", "java.util.Optional.class")
                                                    },
                                                )
                                            },
                                        ),
                                        string,
                                        string,
                                    ),
                                )
                                arg("headers", mapOf(emptyMap<String, Any>(), string, string))
                                arg("body", "null")
                            },
                        )
                    }

                    function("fromRequest", type("Request")) {
                        arg("serialization", type("Wirespec.Deserializer"))
                        arg("request", type("Wirespec.RawRequest"))
                        returns(
                            construct(type("Request")) {
                                arg(
                                    "done",
                                    call("serialization.deserializeParam") {
                                        arg(
                                            "value",
                                            call("request.queries().getOrDefault") {
                                                arg("key", literal("done"))
                                                arg("defaultValue", emptyList(string))
                                            },
                                        )
                                        arg(
                                            "type",
                                            call("Wirespec.getType") {
                                                arg("type", "Boolean.class")
                                                arg("container", "java.util.Optional.class")
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }

                    function("fromResponse", type("Response")) {
                        arg("serialization", type("Wirespec.Deserializer"))
                        arg("response", type("Wirespec.RawResponse"))
                        switch(call("response.statusCode")) {
                            case(literal(200)) {
                                returns(
                                    construct(type("Response200")) {
                                        arg(
                                            "body",
                                            call("serialization.deserializeBody") {
                                                arg("body", "response.body()")
                                                arg(
                                                    "type",
                                                    call("Wirespec.getType") {
                                                        arg("type", "Todo.class")
                                                        arg("container", "java.util.List.class")
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
                                            call("serialization.deserializeBody") {
                                                arg("body", call("response.body"))
                                                arg(
                                                    "type",
                                                    call("Wirespec.getType") {
                                                        arg("type", "Error.class")
                                                        arg("container", "null")
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

                    function("toResponse", type("Wirespec.RawResponse")) {
                        arg("serialization", type("Wirespec.Serializer"))
                        arg("response", type("Response"))
                        switch("response") {
                            case(type("Response200"), "r") {
                                returns(
                                    construct(type("Wirespec.RawResponse")) {
                                        arg("statusCode", call("r.status"))
                                        arg("headers", "java.util.Collections.emptyMap()")
                                        arg(
                                            "body",
                                            call("serialization.serializeBody") {
                                                arg("body", "r.body")
                                                arg(
                                                    "type",
                                                    call("Wirespec.getType") {
                                                        arg("type", "Todo.class")
                                                        arg("container", "java.util.List.class")
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            case(type("Response404"), "r") {
                                returns(
                                    construct(type("Wirespec.RawResponse")) {
                                        arg("statusCode", call("r.status"))
                                        arg("headers", "java.util.Collections.emptyMap()")
                                        arg(
                                            "body",
                                            call("serialization.serializeBody") {
                                                arg("body", "r.body")
                                                arg(
                                                    "type",
                                                    call("Wirespec.getType") {
                                                        arg("type", "Error.class")
                                                        arg("container", "null")
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


                    asyncFunction("getTodos", type("Response"))
                }
            }
        }

        val output = JavaGenerator.generate(getTodos)

        println(output)

        assertTrue(output.contains("package community.flock.wirespec.generated.examples.spring.endpoint;"))
        assertTrue(output.contains("import community.flock.wirespec.java.Wirespec;"))
        assertTrue(output.contains("import community.flock.wirespec.generated.examples.spring.model.Todo;"))
        assertTrue(output.contains("import community.flock.wirespec.generated.examples.spring.model.Error;"))

        assertTrue(output.contains("public sealed interface Response extends Wirespec.Response {}"))
        assertTrue(output.contains("public sealed interface Response2XX extends Response {}"))
        assertTrue(output.contains("public sealed interface Response4XX extends Response {}"))

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
        assertTrue(output.contains("java.util.Map.ofEntries(java.util.Map.entry(\"done\", serialization.serializeParam(request.queries.done, Wirespec.getType(Boolean.class, java.util.Optional.class))))"))
        assertTrue(output.contains("java.util.Collections.emptyMap()"))
        assertTrue(output.contains("null"))
        assertTrue(output.contains("public interface Handler extends Wirespec.Handler {"))
        assertTrue(output.contains("public java.util.concurrent.CompletableFuture<Response> getTodos();"))
        assertTrue(output.contains("public default Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response response) {"))
        assertTrue(output.contains("case Response200 r -> {"))
        assertTrue(output.contains("case Response404 r -> {"))
        assertTrue(output.contains("return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Todo.class, java.util.List.class)));"))
        assertTrue(output.contains("default -> {"))
    }
}
