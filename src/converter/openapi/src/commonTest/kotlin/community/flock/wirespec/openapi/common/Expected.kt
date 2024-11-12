package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union

object Expected {

    val objectInRequest = listOf(
        Endpoint(
            comment = null,
            identifier = DefinitionIdentifier("TestWithDashGET"),
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
            identifier = DefinitionIdentifier("TestWithDashGETRequestBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("id"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isIterable = false),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("nest"),
                        reference = Reference.Custom(value = "TestWithDashGETRequestBodyNest", isIterable = false),
                        isNullable = true
                    )
                )
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            identifier = DefinitionIdentifier("TestWithDashGETRequestBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number, isIterable = false),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
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
            identifier = DefinitionIdentifier("Test"),
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
            identifier = DefinitionIdentifier("Test200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("nest"),
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
            identifier = DefinitionIdentifier("Test200ApplicationJsonResponseBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
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
            identifier = DefinitionIdentifier("AdditionalProperties"),
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
            identifier = DefinitionIdentifier("AdditionalProperties404ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("ArrayGET"),
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
            identifier = DefinitionIdentifier("ArrayGET200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("MessageArray"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("AllofGET"),
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
            identifier = DefinitionIdentifier("AllofGET200ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = FieldIdentifier("c"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false
                        ),
                        isNullable = true
                    ),
                    Field(
                        identifier = FieldIdentifier("d"),
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
            identifier = DefinitionIdentifier("AllofGET200ApplicationJsonResponseBodyD"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("e"),
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
            identifier = DefinitionIdentifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
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
            identifier = DefinitionIdentifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("b"),
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
            identifier = DefinitionIdentifier("OneofGET"),
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
            identifier = DefinitionIdentifier("OneofGET200ApplicationJsonResponseBody"),
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
            identifier = DefinitionIdentifier("OneofGET200ApplicationJsonResponseBody2"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("c"),
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
            identifier = DefinitionIdentifier("OneofGET200ApplicationJsonResponseBody3"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("d"),
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
            identifier = DefinitionIdentifier("OneofGET200ApplicationJsonResponseBody3D"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier(
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
            identifier = DefinitionIdentifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
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
            identifier = DefinitionIdentifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("b"),
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
            identifier = DefinitionIdentifier("EnumGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "enum")),
            queries = listOf(
                Field(
                    identifier = FieldIdentifier("order"),
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
            identifier = DefinitionIdentifier("EnumGETParameterOrder"),
            entries = setOf("ASC", "DESC")
        ),
        Type(
            comment = null,
            identifier = DefinitionIdentifier("EnumGET201ApplicationJsonResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Custom(
                            value = "EnumGET201ApplicationJsonResponseBodyCode",
                            isIterable = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("EnumGET201ApplicationJsonResponseBodyCode"),
            entries = setOf("WARNING", "ERROR")
        ),
        Type(
            comment = null,
            identifier = DefinitionIdentifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        reference = Reference.Custom(
                            value = "ErrorType",
                            isIterable = true
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
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
            identifier = DefinitionIdentifier("ErrorType"),
            entries = setOf("WARNING", "ERROR")
        )
    )
}
