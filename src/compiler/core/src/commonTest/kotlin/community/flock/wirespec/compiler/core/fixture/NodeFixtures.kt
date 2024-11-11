package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.ClassIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

object NodeFixtures {

    val refined = Refined(
        comment = null,
        identifier = ClassIdentifier("UUID"),
        validator = Refined.Validator(
            "/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\$/"
        )
    )


    val enum = Enum(
        comment = null,
        identifier = ClassIdentifier("TodoStatus"),
        entries = setOf(
            "OPEN",
            "IN_PROGRESS",
            "CLOSE",
        )
    )


    val type = Type(
        comment = null,
        identifier = ClassIdentifier("Todo"),
        shape = Type.Shape(
            value = listOf(
                Field(
                    identifier = FieldIdentifier("name"),
                    reference = Primitive(type = Primitive.Type.String),
                    isNullable = false,
                ),
                Field(
                    identifier = FieldIdentifier("description"),
                    reference = Primitive(type = Primitive.Type.String),
                    isNullable = true,
                ),
                Field(
                    identifier = FieldIdentifier("notes"),
                    reference = Primitive(type = Primitive.Type.String, isIterable = true),
                    isNullable = false,
                ),
                Field(
                    identifier = FieldIdentifier("done"),
                    reference = Primitive(type = Primitive.Type.Boolean),
                    isNullable = false,
                )
            )
        ),
        extends = emptyList(),
    )
}
