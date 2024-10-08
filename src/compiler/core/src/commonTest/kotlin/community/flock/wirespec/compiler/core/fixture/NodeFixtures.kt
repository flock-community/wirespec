package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

object NodeFixtures {

    val refined = Refined(
        comment = null,
        identifier = Identifier("UUID"),
        validator = Refined.Validator(
            "/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$/"
        )
    )


    val enum = Enum(
        comment = null,
        identifier = Identifier("TodoStatus"),
        entries = setOf(
            "OPEN",
            "IN_PROGRESS",
            "CLOSE",
        )
    )


    val type = Type(
        comment = null,
        identifier = Identifier("Todo"),
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
        ),
        extends = emptyList(),
    )
}
