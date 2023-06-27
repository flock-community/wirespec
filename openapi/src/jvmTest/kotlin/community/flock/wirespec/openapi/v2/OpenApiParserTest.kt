package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.*
import community.flock.wirespec.openapi.IO
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiParserTest {

    @Test
    fun petstore() {
        val json = IO.readOpenApi("v2/petstore.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedDefinitions = listOf(
            Type(
                name = "ApiResponse",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "code"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "type"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "message"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Category",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Pet",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "category"),
                            reference = Custom(value = "Category", isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "photoUrls"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = true),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "tags"),
                            reference = Custom(value = "Tag", isIterable = true),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "status"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "PetCategory",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Tag",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Order",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "petId"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "quantity"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "shipDate"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "status"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "complete"),
                            reference = Primitive(type = Primitive.Type.Boolean, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "User",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "username"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "firstName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "lastName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "email"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "password"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "phone"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ), Field(
                            identifier = Identifier(value = "userStatus"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            )
        )
        val types = ast.filterIsInstance<Type>()
        assertEquals(expectedDefinitions, types)

        println(ast)
    }

}
