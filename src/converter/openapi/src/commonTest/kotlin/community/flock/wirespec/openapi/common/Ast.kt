package community.flock.wirespec.openapi.common

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.openapi.toDescriptionAnnotationList

object Ast {

    val objectInRequest = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("TestWithDashGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test-with-dash")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(value = "TestWithDashGETRequestBody", isNullable = false),
                    ),
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("TestWithDashGETRequestBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("id"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = true),
                    ),
                    Field(
                        identifier = FieldIdentifier("nest"),
                        annotations = emptyList(),
                        reference = Reference.Custom(value = "TestWithDashGETRequestBodyNest", isNullable = true),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("TestWithDashGETRequestBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number(constraint = null), isNullable = true),
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.Number(constraint = null), isNullable = true),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val objectInResponse = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Test"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "test")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null,
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "Test200ResponseBody",
                            isNullable = false,
                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Test200ResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("id"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("nest"),
                        annotations = emptyList(),
                        reference = Reference.Custom(
                            value = "Test200ResponseBodyNest",
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Test200ResponseBodyNest"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number(constraint = null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number(constraint = null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val additionalProperties = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("AdditionalProperties"),
            method = Endpoint.Method.GET,
            path = listOf(
                Endpoint.Segment.Literal(value = "additional"),
                Endpoint.Segment.Literal(value = "properties"),
            ),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Dict(
                            reference = Reference.Custom(
                                value = "Message",
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Dict(
                            reference = Reference.Custom(
                                value = "Message",
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                    "Ok".toDescriptionAnnotationList(),
                ),
                Endpoint.Response(
                    status = "404",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Dict(
                            reference = Reference.Custom(
                                value = "AdditionalProperties404ResponseBody",
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                    "Not found".toDescriptionAnnotationList(),
                ),
                Endpoint.Response(
                    status = "500",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Dict(
                            reference = Reference.Any(
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                    "Error".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("AdditionalProperties404ResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("User"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("username"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(constraint = null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("email"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("age"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null, precision = Reference.Primitive.Type.Precision.P64),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val array = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("ArrayGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "array")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Iterable(
                            reference = Reference.Iterable(
                                reference = Reference.Custom(
                                    value = "MessageArray",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Iterable(
                            reference = Reference.Custom(
                                value = "ArrayGET200ResponseBody",
                                isNullable = false,
                            ),
                            isNullable = false,

                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
                Endpoint.Response(
                    status = "201",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Iterable(
                            reference = Reference.Iterable(
                                reference = Reference.Custom(
                                    value = "MessageArray",
                                    isNullable = false,

                                ),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                    annotations = "Created".toDescriptionAnnotationList(),
                ),
                Endpoint.Response(
                    status = "202",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Iterable(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String(null),
                                isNullable = false,
                            ),
                            isNullable = false,

                        ),
                    ),
                    annotations = "Created".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("ArrayGET200ResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number(constraint = null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("MessageArray"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Number(constraint = null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val allOf = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("AllofGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "allof")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null,
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "AllofGET200ResponseBody",
                            isNullable = false,
                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("AllofGET200ResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("b"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("c"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("d"),
                        annotations = emptyList(),
                        reference = Reference.Custom(
                            value = "AllofGET200ResponseBodyD",
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("AllofGET200ResponseBodyD"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("e"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("b"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val oneOf = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("OneofGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "oneof")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null,
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    annotations = "Ok".toDescriptionAnnotationList(),
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "OneofGET200ResponseBody",
                            isNullable = false,
                        ),
                    ),
                ),
            ),
        ),
        Union(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("OneofGET200ResponseBody"),
            entries = setOf(
                Reference.Custom(value = "Foo", isNullable = false),
                Reference.Custom(value = "Bar", isNullable = false),
                Reference.Custom(
                    value = "OneofGET200ResponseBody2",
                    isNullable = false,
                ),
                Reference.Custom(
                    value = "OneofGET200ResponseBody3",
                    isNullable = false,
                ),
            ),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("OneofGET200ResponseBody2"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("c"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("OneofGET200ResponseBody3"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("d"),
                        annotations = emptyList(),
                        reference = Reference.Custom(
                            value = "OneofGET200ResponseBody3D",
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("OneofGET200ResponseBody3D"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("e"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Foo"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("a"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Bar"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("b"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
    )

    val enum = nonEmptyListOf(
        Endpoint(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("EnumGET"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "enum")),
            queries = listOf(
                Field(
                    identifier = FieldIdentifier("order"),
                    annotations = emptyList(),
                    reference = Reference.Custom(
                        value = "EnumGETParameterOrder",
                        isNullable = true,
                    ),
                ),
            ),
            headers = emptyList(),
            requests = listOf(
                Endpoint.Request(
                    content = null,
                ),
            ),
            responses = listOf(
                Endpoint.Response(
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "Message",
                            isNullable = false,
                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
                Endpoint.Response(
                    status = "201",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Custom(
                            value = "EnumGET201ResponseBody",
                            isNullable = false,
                        ),
                    ),
                    annotations = "Ok".toDescriptionAnnotationList(),
                ),
            ),
        ),
        Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("EnumGETParameterOrder"),
            entries = setOf("ASC", "DESC"),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("EnumGET201ResponseBody"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Custom(
                            value = "EnumGET201ResponseBodyCode",
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("EnumGET201ResponseBodyCode"),
            entries = setOf("WARNING", "ERROR"),
        ),
        Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Message"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("code"),
                        annotations = emptyList(),
                        reference = Reference.Iterable(
                            reference = Reference.Custom(
                                value = "ErrorType",
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("text"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        ),
        Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("ErrorType"),
            entries = setOf("WARNING", "ERROR"),
        ),
    )
}
