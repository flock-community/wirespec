package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference.Custom
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Type.Shape
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.openapi.common.toDescriptionAnnotationList
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser.parse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

/**
 * Regression coverage for issue #660: kotlin-openapi-bindings 0.3.1 split the single V3 model
 * into version-specific 3.0 / 3.1 / 3.2 type families. The parser used to cast everything to the
 * 3.0 types, silently dropping parameters (and more) for any spec that was not exactly 3.0.x.
 *
 * Each concrete subclass parses a genuine, version-appropriate spec for the SAME logical API and
 * asserts the SAME expected AST. Crucially the specs are not the 3.0 spec with the version string
 * swapped: the 3.1 / 3.2 fixtures express nullability with `type: [..., "null"]` (the 3.0 fixture
 * uses `nullable: true`), so they exercise the version-specific schema shapes — not just the
 * version field.
 */
abstract class OpenAPIV3VersionParserTest(private val resourceDir: String) {

    private fun parse(name: String) = OpenAPIV3
        .decodeFromString(
            SystemFileSystem
                .source(Path("src/commonTest/resources/v3/$resourceDir/$name.json"))
                .buffered()
                .readString(),
        )
        .parse()
        .shouldNotBeNull()

    @Test
    fun comprehensive() {
        val ast = parse("comprehensive")

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("GetPet"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("pets"),
                    Endpoint.Segment.Param(
                        identifier = FieldIdentifier("petId"),
                        reference = Primitive(Primitive.Type.Integer(Primitive.Type.Precision.P64, null), false),
                    ),
                ),
                queries = listOf(
                    Field(
                        identifier = FieldIdentifier("limit"),
                        annotations = emptyList(),
                        reference = Primitive(Primitive.Type.Integer(Primitive.Type.Precision.P32, null), true),
                    ),
                    Field(
                        identifier = FieldIdentifier("status"),
                        annotations = emptyList(),
                        reference = Custom("GetPetParameterStatus", true),
                    ),
                ),
                headers = listOf(
                    Field(
                        identifier = FieldIdentifier("X-Request-Id"),
                        annotations = emptyList(),
                        reference = Primitive(Primitive.Type.String(null), false),
                    ),
                ),
                requests = listOf(Endpoint.Request(null)),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content("application/json", Custom("Pet", true)),
                        annotations = "OK".toDescriptionAnnotationList(),
                    ),
                    Endpoint.Response(
                        status = "404",
                        headers = emptyList(),
                        content = null,
                        annotations = "Not found".toDescriptionAnnotationList(),
                    ),
                ),
            ),
            Enum(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("GetPetParameterStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Pet"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.Integer(Primitive.Type.Precision.P64, null), false),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), false),
                        ),
                        Field(
                            identifier = FieldIdentifier("tag"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Cat"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("sound"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), false),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Union(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Animal"),
                entries = setOf(Custom("Pet", false), Custom("Cat", false)),
            ),
        )

        ast.toList() shouldBe expected
    }
}

class OpenAPIV30VersionParserTest : OpenAPIV3VersionParserTest("v30")

class OpenAPIV31VersionParserTest : OpenAPIV3VersionParserTest("v31")

class OpenAPIV32VersionParserTest : OpenAPIV3VersionParserTest("v32")
