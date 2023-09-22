package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference

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
                        identifier = Identifier(value = "id"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "nest"),
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
                        identifier = Identifier(value = "a"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Integer, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "b"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Integer, isIterable = false),
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
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "Test200ResponseBody",
                            isIterable = false
                        ),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "Test200ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "nest"),
                        reference = Reference.Custom(
                            value = "Test200ResponseBodyNest",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "Test200ResponseBodyNest",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
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
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isMap = true),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "404",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "AdditionalProperties404ResponseBody",
                            isIterable = false,
                            isMap = true
                        ),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "500",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Any(false, true),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "AdditionalProperties404ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
                        identifier = Identifier(value = "code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "ArrayGET200ResponseBody", isIterable = true, isMap = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "MessageArray", isIterable = true, isMap = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "202",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = true, isMap = false),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "ArrayGET200ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
                        identifier = Identifier(value = "code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "AllofGET200ResponseBody", isIterable = false, isMap = false),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "AllofGET200ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "c"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "d"),
                        reference = Reference.Custom(
                            value = "AllofGET200ResponseBodyD",
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        ),
        Type(
            name = "AllofGET200ResponseBodyD",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "e"),
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
                        identifier = Identifier(value = "a"),
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
                        identifier = Identifier(value = "b"),
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
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "Message", isIterable = false, isMap = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "EnumGET201ResponseBody", isIterable = false, isMap = false),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name = "EnumGET201ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "code"),
                        reference = Reference.Custom(
                            value = "EnumGET201ResponseBodyCode",
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
            name="EnumGET201ResponseBodyCode",
            entries= setOf("WARNING", "ERROR")
        ),
        Type(
            name = "Message",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Identifier(value = "code"),
                        reference = Reference.Custom(
                            value = "ErrorType",
                            isIterable = true
                        ),
                        isNullable = false
                    ),
                    Type.Shape.Field(
                        identifier = Identifier(value = "text"),
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
            name="ErrorType",
            entries= setOf("WARNING", "ERROR")
        )
    )
}
