package community.flock.wirespec.language.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransformTest {

    @Test
    fun transformShouldRenameCustomType() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("address", Type.Custom("Address")),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals("Person", result.name)
        assertEquals(Type.String, result.fields[0].type)
        assertEquals(Type.Custom("Location"), result.fields[1].type)
    }

    @Test
    fun transformShouldRenameNestedCustomType() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("addresses", Type.Array(Type.Custom("Address"))),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals(Type.Array(Type.Custom("Location")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameTypeInNullable() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("address", Type.Nullable(Type.Custom("Address"))),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals(Type.Nullable(Type.Custom("Location")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameTypeInDict() {
        val struct = Struct(
            name = "Registry",
            fields = listOf(
                Field("items", Type.Dict(Type.String, Type.Custom("Item"))),
            ),
        )

        val result = struct.renameType("Item", "Product")

        assertEquals(Type.Dict(Type.String, Type.Custom("Product")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameField() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("firstName", Type.String),
                Field("lastName", Type.String),
            ),
        )

        val result = struct.renameField("firstName", "givenName")

        assertEquals("givenName", result.fields[0].name)
        assertEquals("lastName", result.fields[1].name)
    }

    @Test
    fun transformMatchingShouldTransformSpecificType() {
        val struct = Struct(
            name = "Container",
            fields = listOf(
                Field("items", Type.Array(Type.Custom("Item"))),
                Field("count", Type.Integer()),
            ),
        )

        val result = struct.transformMatching { array: Type.Array ->
            Type.Custom("List", listOf(array.elementType))
        }

        assertEquals(Type.Custom("List", listOf(Type.Custom("Item"))), result.fields[0].type)
        assertEquals(Type.Integer(), result.fields[1].type)
    }

    @Test
    fun transformMatchingElementsShouldTransformSpecificElementType() {
        val file = File(
            name = "test.ws",
            elements = listOf(
                Struct("Person", listOf(Field("name", Type.String))),
                Struct("Address", listOf(Field("street", Type.String))),
            ),
        )

        val result = file.transformMatchingElements { struct: Struct ->
            struct.copy(name = "Prefixed${struct.name}")
        }

        val structs = result.elements.filterIsInstance<Struct>()
        assertEquals("PrefixedPerson", structs[0].name)
        assertEquals("PrefixedAddress", structs[1].name)
    }

    @Test
    fun transformFieldsWhereShouldTransformMatchingFields() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("age", Type.Integer()),
                Field("description", Type.String),
            ),
        )

        val result = struct.transformFieldsWhere(
            predicate = { it.type == Type.String },
            transform = { it.copy(type = Type.Nullable(it.type)) },
        )

        assertEquals(Type.Nullable(Type.String), result.fields[0].type)
        assertEquals(Type.Integer(), result.fields[1].type)
        assertEquals(Type.Nullable(Type.String), result.fields[2].type)
    }

    @Test
    fun forEachTypeShouldVisitAllTypes() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("addresses", Type.Array(Type.Custom("Address"))),
            ),
        )

        val types = mutableListOf<Type>()
        struct.forEachType { types.add(it) }

        assertTrue(types.contains(Type.String))
        assertTrue(types.contains(Type.Array(Type.Custom("Address"))))
        assertTrue(types.contains(Type.Custom("Address")))
    }

    @Test
    fun forEachElementShouldVisitAllElements() {
        val file = File(
            name = "test.ws",
            elements = listOf(
                Struct("Person", listOf(Field("name", Type.String))),
                Struct("Address", listOf(Field("street", Type.String))),
            ),
        )

        val elements = mutableListOf<Element>()
        file.forEachElement { elements.add(it) }

        assertEquals(3, elements.size)
        assertTrue(elements[0] is File)
        assertTrue(elements[1] is Struct)
        assertTrue(elements[2] is Struct)
    }

    @Test
    fun forEachFieldShouldVisitAllFields() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("age", Type.Integer()),
            ),
        )

        val fields = mutableListOf<Field>()
        struct.forEachField { fields.add(it) }

        assertEquals(2, fields.size)
        assertEquals("name", fields[0].name)
        assertEquals("age", fields[1].name)
    }

    @Test
    fun collectTypesShouldReturnAllTypes() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("age", Type.Integer()),
            ),
        )

        val types = struct.collectTypes()

        assertEquals(2, types.size)
        assertTrue(types.contains(Type.String))
        assertTrue(types.contains(Type.Integer()))
    }

    @Test
    fun collectCustomTypeNamesShouldReturnAllCustomTypeNames() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("address", Type.Custom("Address")),
                Field("company", Type.Custom("Company")),
                Field("name", Type.String),
            ),
        )

        val names = struct.collectCustomTypeNames()

        assertEquals(setOf("Address", "Company"), names)
    }

    @Test
    fun findAllShouldReturnAllMatchingElements() {
        val file = File(
            name = "test.ws",
            elements = listOf(
                Struct("Person", listOf(Field("name", Type.String))),
                Enum("Status", entries = listOf(Enum.Entry("Active", emptyList()))),
                Struct("Address", listOf(Field("street", Type.String))),
            ),
        )

        val structs = file.findAll<Struct>()

        assertEquals(2, structs.size)
        assertEquals("Person", structs[0].name)
        assertEquals("Address", structs[1].name)
    }

    @Test
    fun findAllTypesShouldReturnAllMatchingTypes() {
        val struct = Struct(
            name = "Container",
            fields = listOf(
                Field("items", Type.Array(Type.Custom("Item"))),
                Field("tags", Type.Array(Type.String)),
                Field("name", Type.String),
            ),
        )

        val arrays = struct.findAllTypes<Type.Array>()

        assertEquals(2, arrays.size)
    }

    @Test
    fun transformShouldHandleDeeplyNestedStructures() {
        val file = File(
            name = "test.ws",
            elements = listOf(
                Struct(
                    name = "Outer",
                    fields = listOf(
                        Field("inner", Type.Custom("Inner")),
                    ),
                    elements = listOf(
                        Struct(
                            name = "Inner",
                            fields = listOf(
                                Field("value", Type.Custom("Value")),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = file.renameType("Value", "Data")
        val outer = result.elements[0] as Struct
        val inner = outer.elements[0] as Struct

        assertEquals(Type.Custom("Data"), inner.fields[0].type)
    }

    @Test
    fun transformShouldHandleFunctionParametersAndReturnTypes() {
        val function = Function(
            name = "process",
            parameters = listOf(
                Parameter("input", Type.Custom("Input")),
            ),
            returnType = Type.Custom("Output"),
            body = emptyList(),
        )

        val result = function.renameType("Input", "Request")
            .renameType("Output", "Response")

        assertEquals(Type.Custom("Request"), result.parameters[0].type)
        assertEquals(Type.Custom("Response"), result.returnType)
    }

    @Test
    fun transformShouldHandleGenericTypes() {
        val struct = Struct(
            name = "Container",
            fields = listOf(
                Field("items", Type.Custom("List", listOf(Type.Custom("Item")))),
            ),
        )

        val result = struct.renameType("Item", "Product")

        assertEquals(
            Type.Custom("List", listOf(Type.Custom("Product"))),
            result.fields[0].type,
        )
    }

    @Test
    fun customTransformerShouldAllowComplexTransformations() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("name", Type.String),
                Field("age", Type.Integer()),
            ),
        )

        val transformer = object : Transformer {
            override fun transformField(field: Field): Field {
                val newType = when (field.type) {
                    Type.String -> Type.Nullable(Type.String)
                    else -> field.type
                }
                return field.copy(type = newType).transformChildren(this)
            }
        }

        val result = struct.transform(transformer)

        assertEquals(Type.Nullable(Type.String), result.fields[0].type)
        assertEquals(Type.Integer(), result.fields[1].type)
    }

    @Test
    fun transformerFunctionShouldCreateTransformerFromLambdas() {
        val struct = Struct(
            name = "Person",
            fields = listOf(
                Field("id", Type.Integer()),
                Field("score", Type.Number()),
            ),
        )

        val transformer = transformer(
            transformType = { type, t ->
                when (type) {
                    is Type.Integer -> Type.Integer(Precision.P64)
                    is Type.Number -> Type.Number(Precision.P32)
                    else -> type.transformChildren(t)
                }
            },
        )

        val result = struct.transform(transformer)

        assertEquals(Type.Integer(Precision.P64), result.fields[0].type)
        assertEquals(Type.Number(Precision.P32), result.fields[1].type)
    }

    @Test
    fun transformShouldHandleStatementsWithExpressions() {
        val function = Function(
            name = "create",
            parameters = emptyList(),
            returnType = Type.Custom("Result"),
            body = listOf(
                ReturnStatement(
                    ConstructorStatement(
                        type = Type.Custom("Result"),
                        namedArguments = mapOf(
                            "value" to Literal("test", Type.String),
                        ),
                    ),
                ),
            ),
        )

        val result = function.renameType("Result", "Response")

        assertEquals(Type.Custom("Response"), result.returnType)
        val returnStmt = result.body[0] as ReturnStatement
        val constructor = returnStmt.expression as ConstructorStatement
        assertEquals(Type.Custom("Response"), constructor.type)
    }

    @Test
    fun transformShouldHandleSwitchStatements() {
        val function = Function(
            name = "process",
            parameters = listOf(Parameter("input", Type.Custom("Input"))),
            returnType = Type.Custom("Output"),
            body = listOf(
                Switch(
                    expression = RawExpression("input"),
                    cases = listOf(
                        Case(
                            value = RawExpression("case1"),
                            body = listOf(
                                ReturnStatement(ConstructorStatement(Type.Custom("Output"))),
                            ),
                            type = Type.Custom("Type1"),
                        ),
                    ),
                    default = listOf(
                        ErrorStatement(Literal("Unknown", Type.String)),
                    ),
                ),
            ),
        )

        val result = function.renameType("Output", "Result")

        val switch = result.body[0] as Switch
        val case = switch.cases[0]
        val returnStmt = case.body[0] as ReturnStatement
        val constructor = returnStmt.expression as ConstructorStatement
        assertEquals(Type.Custom("Result"), constructor.type)
    }
}
