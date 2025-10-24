package community.flock.wirespec.compiler.test

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type

object NodeFixtures {

    val refined = Refined(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("UUID"),
        reference = Reference.Primitive(
            isNullable = false,
            type = Reference.Primitive.Type.String(
                constraint = Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/"),
            ),
        ),
    )

    val enum = Enum(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("TodoStatus"),
        entries = setOf(
            "OPEN",
            "IN_PROGRESS",
            "CLOSE",
        ),
    )

    val type = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Todo"),
        shape = Type.Shape(
            value = listOf(
                Field(
                    identifier = FieldIdentifier("name"),
                    annotations = emptyList(),
                    reference = Reference.Primitive(
                        type = Reference.Primitive.Type.String(null),
                        isNullable = false,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("description"),
                    annotations = emptyList(),
                    reference = Reference.Primitive(
                        type = Reference.Primitive.Type.String(null),
                        isNullable = true,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("notes"),
                    annotations = emptyList(),
                    reference = Reference.Iterable(
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                        isNullable = false,
                    ),
                ),
                Field(
                    identifier = FieldIdentifier("done"),
                    annotations = emptyList(),
                    reference = Reference.Primitive(
                        type = Reference.Primitive.Type.Boolean,
                        isNullable = false,
                    ),
                ),
            ),
        ),
        extends = emptyList(),
    )

    val emptyType = Type(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("TodoWithoutProperties"),
        shape = Type.Shape(
            value = emptyList(),
        ),
        extends = emptyList(),
    )
}
