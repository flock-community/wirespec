package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive

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
                        reference = Custom(value = "TestWithDashGETRequestBody", isIterable = false),
                        isNullable = false
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Primitive(type = Primitive.Type.String, isIterable = false),
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
                        identifier = Type.Shape.Field.Identifier(value = "id"),
                        reference = Primitive(type = Primitive.Type.String, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "nest"),
                        reference = Custom(value = "TestWithDashGETRequestBodyNest", isIterable = false),
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
                        identifier = Type.Shape.Field.Identifier(value = "a"),
                        reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "b"),
                        reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
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
                        reference = Custom(
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
                        identifier = Type.Shape.Field.Identifier(value = "id"),
                        reference = Primitive(
                            type = Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "nest"),
                        reference = Custom(
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
                        identifier = Type.Shape.Field.Identifier(value = "a"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "b"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
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
            path = listOf(Endpoint.Segment.Literal(value = "additional"), Endpoint.Segment.Literal(value = "properties")),
            query = emptyList(),
            headers = emptyList(),
            cookies = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Custom(value = "Message", isIterable = false, isMap = true),
                        isNullable = false
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Custom(value = "Message", isIterable = false, isMap = true),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "404",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Custom(value = "AdditionalProperties404ResponseBody", isIterable = false, isMap = true),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name="AdditionalProperties404ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "code"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "text"),
                        reference = Primitive(
                            type = Primitive.Type.String,
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
                        identifier = Type.Shape.Field.Identifier(value = "code"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "text"),
                        reference = Primitive(
                            type = Primitive.Type.String,
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
                        reference = Custom(value = "Message", isIterable = true, isMap = false),
                        isNullable = false
                    )
                )
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Custom(value = "ArrayGET200ResponseBody", isIterable = false, isMap = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "201",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Custom(value = "MessageArray", isIterable = true, isMap = false),
                        isNullable = false
                    )
                ),
                Endpoint.Response(
                    status = "202",
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Primitive(type = Primitive.Type.String, isIterable = true, isMap = false),
                        isNullable = false
                    )
                )
            )
        ),
        Type(
            name="ArrayGET200ResponseBody",
            shape = Type.Shape(
                value = listOf(
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "code"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "text"),
                        reference = Primitive(
                            type = Primitive.Type.String,
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
                        identifier = Type.Shape.Field.Identifier(value = "code"),
                        reference = Primitive(
                            type = Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Type.Shape.Field(
                        identifier = Type.Shape.Field.Identifier(value = "text"),
                        reference = Primitive(
                            type = Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    )
                )
            )
        )
    )
}