package community.flock.wirespec.ir.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransformTest {

    @Test
    fun transformShouldRenameCustomType() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("address"), Type.Custom("Address")),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals(Name.of("Person"), result.name)
        assertEquals(Type.String, result.fields[0].type)
        assertEquals(Type.Custom("Location"), result.fields[1].type)
    }

    @Test
    fun transformShouldRenameNestedCustomType() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("addresses"), Type.Array(Type.Custom("Address"))),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals(Type.Array(Type.Custom("Location")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameTypeInNullable() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Nullable(Type.Custom("Address"))),
            ),
        )

        val result = struct.renameType("Address", "Location")

        assertEquals(Type.Nullable(Type.Custom("Location")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameTypeInDict() {
        val struct = Struct(
            name = Name.of("Registry"),
            fields = listOf(
                Field(Name.of("items"), Type.Dict(Type.String, Type.Custom("Item"))),
            ),
        )

        val result = struct.renameType("Item", "Product")

        assertEquals(Type.Dict(Type.String, Type.Custom("Product")), result.fields[0].type)
    }

    @Test
    fun transformShouldRenameField() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("firstName"), Type.String),
                Field(Name.of("lastName"), Type.String),
            ),
        )

        val result = struct.renameField("firstName", "givenName")

        assertEquals(Name.of("givenName"), result.fields[0].name)
        assertEquals(Name.of("lastName"), result.fields[1].name)
    }

    @Test
    fun transformMatchingShouldTransformSpecificType() {
        val struct = Struct(
            name = Name.of("Container"),
            fields = listOf(
                Field(Name.of("items"), Type.Array(Type.Custom("Item"))),
                Field(Name.of("count"), Type.Integer()),
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
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(Name.of("Person"), listOf(Field(Name.of("name"), Type.String))),
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
            ),
        )

        val result = file.transformMatchingElements { struct: Struct ->
            struct.copy(name = Name.of("Prefixed${struct.name.pascalCase()}"))
        }

        val structs = result.elements.filterIsInstance<Struct>()
        assertEquals(Name.of("PrefixedPerson"), structs[0].name)
        assertEquals(Name.of("PrefixedAddress"), structs[1].name)
    }

    @Test
    fun transformFieldsWhereShouldTransformMatchingFields() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("age"), Type.Integer()),
                Field(Name.of("description"), Type.String),
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
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("addresses"), Type.Array(Type.Custom("Address"))),
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
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(Name.of("Person"), listOf(Field(Name.of("name"), Type.String))),
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
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
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("age"), Type.Integer()),
            ),
        )

        val fields = mutableListOf<Field>()
        struct.forEachField { fields.add(it) }

        assertEquals(2, fields.size)
        assertEquals(Name.of("name"), fields[0].name)
        assertEquals(Name.of("age"), fields[1].name)
    }

    @Test
    fun collectTypesShouldReturnAllTypes() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("age"), Type.Integer()),
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
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
                Field(Name.of("company"), Type.Custom("Company")),
                Field(Name.of("name"), Type.String),
            ),
        )

        val names = struct.collectCustomTypeNames()

        assertEquals(setOf("Address", "Company"), names)
    }

    @Test
    fun findAllShouldReturnAllMatchingElements() {
        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(Name.of("Person"), listOf(Field(Name.of("name"), Type.String))),
                Enum(Name.of("Status"), entries = listOf(Enum.Entry(Name.of("Active"), emptyList()))),
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
            ),
        )

        val structs = file.findAll<Struct>()

        assertEquals(2, structs.size)
        assertEquals(Name.of("Person"), structs[0].name)
        assertEquals(Name.of("Address"), structs[1].name)
    }

    @Test
    fun findAllTypesShouldReturnAllMatchingTypes() {
        val struct = Struct(
            name = Name.of("Container"),
            fields = listOf(
                Field(Name.of("items"), Type.Array(Type.Custom("Item"))),
                Field(Name.of("tags"), Type.Array(Type.String)),
                Field(Name.of("name"), Type.String),
            ),
        )

        val arrays = struct.findAllTypes<Type.Array>()

        assertEquals(2, arrays.size)
    }

    @Test
    fun transformShouldHandleDeeplyNestedStructures() {
        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(
                    name = Name.of("Outer"),
                    fields = listOf(
                        Field(Name.of("inner"), Type.Custom("Inner")),
                    ),
                    elements = listOf(
                        Struct(
                            name = Name.of("Inner"),
                            fields = listOf(
                                Field(Name.of("value"), Type.Custom("Value")),
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
            name = Name.of("process"),
            parameters = listOf(
                Parameter(Name.of("input"), Type.Custom("Input")),
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
            name = Name.of("Container"),
            fields = listOf(
                Field(Name.of("items"), Type.Custom("List", listOf(Type.Custom("Item")))),
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
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("name"), Type.String),
                Field(Name.of("age"), Type.Integer()),
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
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("id"), Type.Integer()),
                Field(Name.of("score"), Type.Number()),
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
            name = Name.of("create"),
            parameters = emptyList(),
            returnType = Type.Custom("Result"),
            body = listOf(
                ReturnStatement(
                    ConstructorStatement(
                        type = Type.Custom("Result"),
                        namedArguments = mapOf(
                            Name.of("value") to Literal("test", Type.String),
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
            name = Name.of("process"),
            parameters = listOf(Parameter(Name.of("input"), Type.Custom("Input"))),
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

    // TransformScope DSL tests

    @Test
    fun transformScopeSingleRenameType() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("address"), Type.Custom("Address")),
            ),
        )

        val result = struct.transform {
            renameType("Address", "Location")
        }

        assertEquals(Type.Custom("Location"), result.fields[0].type)
    }

    @Test
    fun transformScopeMultipleOperationsInSequence() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("firstName"), Type.Custom("Address")),
            ),
        )

        val result = struct.transform {
            renameType("Address", "Location")
            renameField("firstName", "givenName")
        }

        assertEquals(Type.Custom("Location"), result.fields[0].type)
        assertEquals(Name.of("givenName"), result.fields[0].name)
    }

    @Test
    fun transformScopeMatchingElements() {
        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(Name.of("Person"), listOf(Field(Name.of("name"), Type.String))),
                Struct(Name.of("Address"), listOf(Field(Name.of("street"), Type.String))),
            ),
        )

        val result = file.transform {
            matchingElements { struct: Struct ->
                struct.copy(name = Name.of("Prefixed${struct.name.pascalCase()}"))
            }
        }

        val structs = result.elements.filterIsInstance<Struct>()
        assertEquals(Name.of("PrefixedPerson"), structs[0].name)
        assertEquals(Name.of("PrefixedAddress"), structs[1].name)
    }

    @Test
    fun transformScopeApplyTransformer() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("id"), Type.Integer()),
            ),
        )

        val result = struct.transform {
            apply(transformer(
                transformType = { type, t ->
                    when (type) {
                        is Type.Integer -> Type.Integer(Precision.P64)
                        else -> type.transformChildren(t)
                    }
                },
            ))
        }

        assertEquals(Type.Integer(Precision.P64), result.fields[0].type)
    }

    @Test
    fun transformScopeFieldsWhereAndParametersWhere() {
        val function = Function(
            name = Name.of("process"),
            parameters = listOf(
                Parameter(Name.of("input"), Type.String),
            ),
            returnType = Type.String,
            body = emptyList(),
        )

        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(
                    name = Name.of("Person"),
                    fields = listOf(Field(Name.of("name"), Type.String)),
                ),
                function,
            ),
        )

        val result = file.transform {
            fieldsWhere({ it.type == Type.String }) { it.copy(type = Type.Nullable(Type.String)) }
            parametersWhere({ it.type == Type.String }) { it.copy(type = Type.Nullable(Type.String)) }
        }

        val struct = result.elements[0] as Struct
        assertEquals(Type.Nullable(Type.String), struct.fields[0].type)
        val fn = result.elements[1] as Function
        assertEquals(Type.Nullable(Type.String), fn.parameters[0].type)
    }

    @Test
    fun transformScopeInjectBeforeAndAfter() {
        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(
                    Name.of("Person"),
                    listOf(Field(Name.of("name"), Type.String)),
                    elements = listOf(RawElement("existing")),
                ),
            ),
        )

        val result = file.transform {
            injectBefore { _: Struct ->
                listOf(RawElement("before"))
            }
            injectAfter { _: Struct ->
                listOf(RawElement("after"))
            }
        }

        val struct = result.elements[0] as Struct
        assertEquals(3, struct.elements.size)
        assertEquals("before", (struct.elements[0] as RawElement).code)
        assertEquals("existing", (struct.elements[1] as RawElement).code)
        assertEquals("after", (struct.elements[2] as RawElement).code)
    }

    @Test
    fun transformScopeEmptyIsNoOp() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(Field(Name.of("name"), Type.String)),
        )

        val result = struct.transform { }

        assertEquals(struct, result)
    }

    @Test
    fun transformScopePreservesReturnType() {
        val file = File(
            name = Name.of("test.ws"),
            elements = listOf(
                Struct(Name.of("Person"), listOf(Field(Name.of("name"), Type.String))),
            ),
        )

        val result: File = file.transform {
            renameType("String", "Text")
        }

        assertTrue(result is File)
        assertEquals(Name.of("test.ws"), result.name)
    }
}
