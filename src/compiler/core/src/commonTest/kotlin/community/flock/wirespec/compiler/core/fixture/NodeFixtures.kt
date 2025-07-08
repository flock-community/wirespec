package community.flock.wirespec.compiler.core.fixture

import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

object NodeFixtures {

    val refined = Refined(
        comment = null,
        identifier = DefinitionIdentifier("UUID"),
        reference = Primitive(
            isNullable = false,
            type = Primitive.Type.String(
                constraint = Primitive.Type.Constraint.RegExp("/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/"),
            ),
        ),
    )

    val enum = Enum(
        comment = null,
        identifier = DefinitionIdentifier("TodoStatus"),
        entries = setOf(
            "OPEN",
            "IN_PROGRESS",
            "CLOSE",
        ),
    )

    val type = Type(
        comment = null,
        identifier = DefinitionIdentifier("Todo"),
        shape = Type.Shape(
            value = listOf(
                Field(
                    identifier = FieldIdentifier("name"),
                    reference = Primitive(
                        type = Primitive.Type.String(null),
                        isNullable = false,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("description"),
                    reference = Primitive(
                        type = Primitive.Type.String(null),
                        isNullable = true,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("notes"),
                    reference = Reference.Iterable(
                        reference = Primitive(
                            type = Primitive.Type.String(null),
                            isNullable = false,
                        ),
                        isNullable = false,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("done"),
                    reference = Primitive(
                        type = Primitive.Type.Boolean,
                        isNullable = false,
                    ),
                ),
            ),
        ),
        extends = emptyList(),
    )

    val emptyType = Type(
        comment = null,
        identifier = DefinitionIdentifier("TodoWithoutProperties"),
        shape = Type.Shape(
            value = emptyList(),
        ),
        extends = emptyList(),
    )
}
