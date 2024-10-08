package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

object Expected {

    val objectInRequest = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("TestWithDashGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test-with-dash")),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "TestWithDashGETRequestBody", isIterable = false),
                        isNullable = true
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = false),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("TestWithDashGETRequestBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("id"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = false),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("nest"),
                        reference = Reference.Custom(value = "TestWithDashGETRequestBodyNest", isIterable = false),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("TestWithDashGETRequestBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number, isIterable = false),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number, isIterable = false),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        )
    )
    val objectInResponse = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("Test"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test")),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "Test200ApplicationJsonResponseBody",
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("Test200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("nest"),
                        reference = Reference.Custom(
                            value = "Test200ApplicationJsonResponseBodyNest",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Test200ApplicationJsonResponseBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        )
    )
    val additionalProperties = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("AdditionalProperties"),
            method = Endpoint.Method.GET,
            path = listOf(
                Endpoint.Segment.Literal(value = "additional"),
                Endpoint.Segment.Literal(value = "properties")
            ),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isDictionary = true),
                        isNullable = true
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isDictionary = true),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "404",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "AdditionalProperties404ApplicationJsonResponseBody",
                            isIterable = false,
                            isDictionary = true
                        ),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "500",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Any(isIterable = false, isDictionary = true),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("AdditionalProperties404ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        )
    )
    val array = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("ArrayGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "array")),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = true, isDictionary = false),
                        isNullable = true
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "ArrayGET200ApplicationJsonResponseBody",
                            isIterable = true,
                            isDictionary = false
                        ),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "MessageArray", isIterable = true, isDictionary = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "202",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = true,
                            isDictionary = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("ArrayGET200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("MessageArray"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        )
    )

    val allOf = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("AllofGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "allof")),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "AllofGET200ApplicationJsonResponseBody",
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("AllofGET200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier("c"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = Identifier("d"),
                        reference = Reference.Custom(
                            value = "AllofGET200ApplicationJsonResponseBodyD",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("AllofGET200ApplicationJsonResponseBodyD"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("e"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            ),
            extends = emptyList(),
        )
    )

    val oneOf = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("OneofGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "oneof")),
            queries = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "OneofGET200ApplicationJsonResponseBody",
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Union(
            comment = null,
            identifier = Identifier("OneofGET200ApplicationJsonResponseBody"),
            entries = setOf(
                Reference.Custom(value = "Foo", isIterable = false, isDictionary = false),
                Reference.Custom(value = "Bar", isIterable = false, isDictionary = false),
                Reference.Custom(
                    value = "OneofGET200ApplicationJsonResponseBody2",
                    isIterable = false,
                    isDictionary = false
                ),
                Reference.Custom(
                    value = "OneofGET200ApplicationJsonResponseBody3",
                    isIterable = false,
                    isDictionary = false
                )
            )
        ),
        Type(
            comment = null,
            identifier = Identifier("OneofGET200ApplicationJsonResponseBody2"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("c"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("OneofGET200ApplicationJsonResponseBody3"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("d"),
                        reference = Reference.Custom(
                            value = "OneofGET200ApplicationJsonResponseBody3D",
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("OneofGET200ApplicationJsonResponseBody3D"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier(
                            "e"
                        ), reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = Identifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            ),
            extends = emptyList(),
        )
    )

    val enum = listOf(
        Endpoint(
            comment = null,
            identifier = Identifier("EnumGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "enum")),
            queries = listOf(
                Field(
                    identifier = Identifier("order"),
                    reference = Reference.Custom(
                        value = "EnumGETParameterOrder",
                        isIterable = false,
                        isDictionary = false
                    ),
                    isNullable = true
                )
            ),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isDictionary = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "EnumGET201ApplicationJsonResponseBody",
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Enum(
            comment = null,
            identifier = Identifier("EnumGETParameterOrder"),
            entries = setOf("ASC", "DESC")
        ),
        Type(
            comment = null,
            identifier = Identifier("EnumGET201ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Custom(
                            value = "EnumGET201ApplicationJsonResponseBodyCode",
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Enum(
            comment = null,
            identifier = Identifier("EnumGET201ApplicationJsonResponseBodyCode"),
            entries = setOf("WARNING", "ERROR")
        ),
        Type(
            comment = null,
            identifier = Identifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier("code"),
                        reference = Reference.Custom(
                            value = "ErrorType",
                            isIterable = true
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Enum(
            comment = null,
            identifier = Identifier("ErrorType"),
            entries = setOf("WARNING", "ERROR")
        )
    )
}
