package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Union

object Expected {

    val objectInRequest = listOf(
        Endpoint(
            name = "TestWithDashGET",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test-with-dash")),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "TestWithDashGETRequestBody", isIterable = false),
                        isNullable = false
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
            name = "TestWithDashGETRequestBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("id"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("nest"),
                        reference = Reference.Custom(value = "TestWithDashGETRequestBodyNest", isIterable = false),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "TestWithDashGETRequestBodyNest",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number, isIterable = false),
                        isNullable = true
                    )
                )
            )
        )
    )
    val objectInResponse = listOf(
        Endpoint(
            name = "Test",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test")),
            query = emptyList(),
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
            name = "Test200ApplicationJsonResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("nest"),
                        reference = Reference.Custom(
                            value = "Test200ApplicationJsonResponseBodyNest",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Test200ApplicationJsonResponseBodyNest",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        )
    )
    val additionalproperties = listOf(
        Endpoint(
            name = "AdditionalProperties",
            method = Endpoint.Method.GET,
            path = listOf(
                Endpoint.Segment.Literal(value = "additional"),
                Endpoint.Segment.Literal(value = "properties")
            ),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isMap = true),
                        isNullable = false
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isMap = true),
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
                            isMap = true
                        ),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "500",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Any(false, true),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "AdditionalProperties404ApplicationJsonResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Message",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        )
    )
    val array = listOf(
        Endpoint(
            name = "ArrayGET",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "array")),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = true, isMap = false),
                        isNullable = false
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
                            isMap = false
                        ),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "MessageArray", isIterable = true, isMap = false),
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
                            isMap = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "ArrayGET200ApplicationJsonResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "MessageArray",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        )
    )

    val allOf = listOf(
        Endpoint(
            name = "AllofGET",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "allof")),
            query = emptyList(),
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
                            isMap = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "AllofGET200ApplicationJsonResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("c"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("d"),
                        reference = Reference.Custom(
                            value = "AllofGET200ApplicationJsonResponseBodyD",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "AllofGET200ApplicationJsonResponseBodyD",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("e"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isMap = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Foo",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Bar",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            )
        )
    )

    val oneOf = listOf(
        Endpoint(
            name = "OneofGET",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "oneof")),
            query = emptyList(),
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
                            isMap = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Union(
            name = "OneofGET200ApplicationJsonResponseBody",
            entries = setOf(
                Reference.Custom("Foo", false),
                Reference.Custom("Bar", false),
                Reference.Custom("OneofGET200ApplicationJsonResponseBodyD", false),
            )
        ),
        Type(
            name = "OneofGET200ApplicationJsonResponseBodyD",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("e"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isMap = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Foo",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Bar",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            )
        )
    )

    val enum = listOf(
        Endpoint(
            name = "EnumGET",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "enum")),
            query = listOf(
                Type.Shape.Field(
                    identifier = Identifier("order"),
                    reference = Reference.Custom(value = "EnumGETParameterOrder", isIterable = false, isMap = false),
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
                        reference = Reference.Custom(value = "Message", isIterable = false, isMap = false),
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
                            isMap = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Enum(
            name = "EnumGETParameterOrder",
            entries = setOf("ASC", "DESC")
        ),
        Type(
            name = "EnumGET201ApplicationJsonResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Custom(
                            value = "EnumGET201ApplicationJsonResponseBodyCode",
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Enum(
            name = "EnumGET201ApplicationJsonResponseBodyCode",
            entries = setOf("WARNING", "ERROR")
        ),
        Type(
            name = "Message",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier("code"),
                        reference = Reference.Custom(
                            value = "ErrorType",
                            isIterable = true
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier("text"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Enum(
            name = "ErrorType",
            entries = setOf("WARNING", "ERROR")
        )
    )
}
