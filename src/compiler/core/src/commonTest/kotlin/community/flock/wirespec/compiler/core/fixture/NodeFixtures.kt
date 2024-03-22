package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive

object NodeFixtures {

    val refined = Refined(
        name = "UUID",
        validator = Refined.Validator(
            "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$"
        )
    )


    val enum = Enum(
        name = "TodoStatus",
        entries = setOf(
            "OPEN",
            "IN_PROGRESS",
            "CLOSE",
        )
    )


    val type = Type(
        name = "Todo",
        shape = Type.Shape(
            value = listOf(
                Field(
                    identifier = Identifier("name"),
                    reference = Primitive(type = Primitive.Type.String),
                    isNullable = false,
                ),
                Field(
                    identifier = Identifier("description"),
                    reference = Primitive(type = Primitive.Type.String),
                    isNullable = true,
                ),
                Field(
                    identifier = Identifier("notes"),
                    reference = Primitive(type = Primitive.Type.String, isIterable = true),
                    isNullable = false,
                ),
                Field(
                    identifier = Identifier("done"),
                    reference = Primitive(type = Primitive.Type.Boolean),
                    isNullable = false,
                )
            )
        )
    )

    val endpoint = Endpoint(
        name = "",
        method = Endpoint.Method.GET,
        path = listOf(Endpoint.Segment.Literal("/todos")),
        query = emptyList(),
        headers = emptyList(),
        cookies = emptyList(),
        requests = emptyList(),
        responses = listOf(
            Endpoint.Response(
                status = "200",
                headers = emptyList(),
                content = null,
            )
        ),
    )
}
