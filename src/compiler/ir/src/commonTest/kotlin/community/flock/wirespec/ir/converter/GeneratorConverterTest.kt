package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.LiteralMap
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.core.transformer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

class GeneratorConverterTest {

    private fun definitionId(name: String) = DefinitionIdentifier(name)
    private fun fieldId(name: String) = FieldIdentifier(name)

    private inline fun <reified T : Statement> File.collectStatements(): List<T> = buildList {
        val collector = this
        val tr = transformer {
            statement { stmt, t ->
                if (stmt is T) collector.add(stmt)
                stmt.transformChildren(t)
            }
            expression { expr, t ->
                if (expr is Statement && expr is T) collector.add(expr)
                expr.transformChildren(t)
            }
        }
        tr.transformElement(this@collectStatements)
    }

    private inline fun <reified T : Expression> File.collectExpressions(): List<T> = buildList {
        val collector = this
        val tr = transformer {
            expression { expr, t ->
                if (expr is T) collector.add(expr)
                expr.transformChildren(t)
            }
            statement { stmt, t ->
                if (stmt is T) collector.add(stmt)
                stmt.transformChildren(t)
            }
        }
        tr.transformElement(this@collectExpressions)
    }

    @Test
    fun testTypeConvertToGeneratorProducesNamespace() {
        val address = TypeWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("Address"),
            shape = TypeWirespec.Shape(
                listOf(
                    Field(
                        emptyList(),
                        fieldId("street"),
                        Reference.Primitive(Reference.Primitive.Type.String(null), false),
                    ),
                    Field(
                        emptyList(),
                        fieldId("number"),
                        Reference.Primitive(Reference.Primitive.Type.Integer(constraint = null), false),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val file = address.convertToGenerator()
        val namespace = file.findElement<Namespace>()!!
        assertEquals(Name.of("AddressGenerator"), namespace.name)

        val fn = namespace.findElement<Function>()!!
        assertEquals(Name.of("generate"), fn.name)
        assertEquals(2, fn.parameters.size)
    }

    @Test
    fun testTypeConvertToGeneratorEmitsClassReferenceAsType() {
        val address = TypeWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("Address"),
            shape = TypeWirespec.Shape(
                listOf(
                    Field(
                        emptyList(),
                        fieldId("street"),
                        Reference.Primitive(Reference.Primitive.Type.String(null), false),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val file = address.convertToGenerator()
        val classRefs = file.collectExpressions<ClassReference>()
        assertTrue(
            classRefs.any { it.type == Type.Custom("Address") },
            "expected a ClassReference(Type.Custom(\"Address\")) in the generated body",
        )
    }

    @Test
    fun testEnumConvertToGenerator() {
        val color = EnumWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )
        val file = color.convertToGenerator()
        val namespace = file.findElement<Namespace>()!!
        assertEquals(Name.of("ColorGenerator"), namespace.name)
        val classRefs = file.collectExpressions<ClassReference>()
        assertTrue(classRefs.any { it.type == Type.Custom("Color") })
    }

    @Test
    fun testRefinedConvertToGeneratorWithRegex() {
        val uuid = RefinedWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("UUID"),
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-f]{8}$/g"),
                ),
                isNullable = false,
            ),
        )
        val file = uuid.convertToGenerator()
        val classRefs = file.collectExpressions<ClassReference>()
        assertTrue(classRefs.any { it.type == Type.Custom("UUID") })
        val literals = file.collectExpressions<Literal>()
        assertTrue(
            literals.any { it.value == "^[0-9a-f]{8}$" },
            "Refined should emit regex literal stripped of slashes and flags",
        )
    }

    @Test
    fun testUnionConvertToGeneratorHasSwitch() {
        val shape = UnionWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("Shape"),
            entries = setOf(
                Reference.Custom(value = "Circle", isNullable = false),
                Reference.Custom(value = "Square", isNullable = false),
            ),
        )
        val file = shape.convertToGenerator()
        val switches = file.collectStatements<Switch>()
        assertEquals(1, switches.size, "Union generator must contain one switch")
        assertEquals(2, switches[0].cases.size)
    }

    @Test
    fun testCoerceAnnotationValueLiteralBoolean() {
        assertEquals(Literal(true, Type.Boolean), coerceAnnotationValueLiteral("true"))
        assertEquals(Literal(false, Type.Boolean), coerceAnnotationValueLiteral("false"))
    }

    @Test
    fun testCoerceAnnotationValueLiteralInteger() {
        assertEquals(Literal(0L, Type.Integer()), coerceAnnotationValueLiteral("0"))
        assertEquals(Literal(42L, Type.Integer()), coerceAnnotationValueLiteral("42"))
        assertEquals(Literal(-7L, Type.Integer()), coerceAnnotationValueLiteral("-7"))
    }

    @Test
    fun testCoerceAnnotationValueLiteralDouble() {
        assertEquals(Literal(1.5, Type.Number()), coerceAnnotationValueLiteral("1.5"))
        assertEquals(Literal(-3.14, Type.Number()), coerceAnnotationValueLiteral("-3.14"))
        assertEquals(Literal(1.0e10, Type.Number()), coerceAnnotationValueLiteral("1.0e10"))
    }

    @Test
    fun testCoerceAnnotationValueLiteralStringFallback() {
        assertEquals(Literal("hello", Type.String), coerceAnnotationValueLiteral("hello"))
        assertEquals(Literal("True", Type.String), coerceAnnotationValueLiteral("True"))
        assertEquals(Literal("1e10", Type.String), coerceAnnotationValueLiteral("1e10"))
        assertEquals(Literal("+1", Type.String), coerceAnnotationValueLiteral("+1"))
        assertEquals(Literal("", Type.String), coerceAnnotationValueLiteral(""))
    }

    @Test
    fun testAnnotationToIrLiteralMapBare() {
        val ann = Annotation(name = "Deprecated", parameters = emptyList())
        val ir = ann.toIrLiteralMap()
        assertEquals(
            LiteralMap(
                values = mapOf(
                    "name" to Literal("Deprecated", Type.String),
                    "parameters" to LiteralMap(emptyMap(), Type.String, Type.Any),
                ),
                keyType = Type.String,
                valueType = Type.Any,
            ),
            ir,
        )
    }

    @Test
    fun testAnnotationToIrLiteralMapMixedSingleParams() {
        val ann = Annotation(
            name = "Range",
            parameters = listOf(
                Annotation.Parameter("min", Annotation.Value.Single("0")),
                Annotation.Parameter("max", Annotation.Value.Single("1.5")),
                Annotation.Parameter("label", Annotation.Value.Single("hello")),
                Annotation.Parameter("active", Annotation.Value.Single("true")),
            ),
        )
        val ir = ann.toIrLiteralMap()
        val params = (ir.values.getValue("parameters") as LiteralMap).values
        assertEquals(Literal(0L, Type.Integer()), params.getValue("min"))
        assertEquals(Literal(1.5, Type.Number()), params.getValue("max"))
        assertEquals(Literal("hello", Type.String), params.getValue("label"))
        assertEquals(Literal(true, Type.Boolean), params.getValue("active"))
    }

    @Test
    fun testAnnotationToIrLiteralMapArrayParam() {
        val ann = Annotation(
            name = "Tags",
            parameters = listOf(
                Annotation.Parameter(
                    name = "items",
                    value = Annotation.Value.Array(
                        listOf(Annotation.Value.Single("1"), Annotation.Value.Single("2")),
                    ),
                ),
            ),
        )
        val ir = ann.toIrLiteralMap()
        val items = (ir.values.getValue("parameters") as LiteralMap).values.getValue("items")
        assertEquals(
            LiteralList(
                values = listOf(Literal(1L, Type.Integer()), Literal(2L, Type.Integer())),
                type = Type.Any,
            ),
            items,
        )
    }

    @Test
    fun testAnnotationToIrLiteralMapDictParam() {
        val ann = Annotation(
            name = "Meta",
            parameters = listOf(
                Annotation.Parameter(
                    name = "info",
                    value = Annotation.Value.Dict(
                        listOf(
                            Annotation.Parameter("a", Annotation.Value.Single("1")),
                            Annotation.Parameter("b", Annotation.Value.Single("hello")),
                        ),
                    ),
                ),
            ),
        )
        val ir = ann.toIrLiteralMap()
        val info = (ir.values.getValue("parameters") as LiteralMap).values.getValue("info") as LiteralMap
        assertEquals(Literal(1L, Type.Integer()), info.values.getValue("a"))
        assertEquals(Literal("hello", Type.String), info.values.getValue("b"))
    }

    @Test
    fun testAnnotationsToIrListEmpty() {
        val ir = annotationsToIrList(emptyList())
        assertEquals(LiteralList(values = emptyList(), type = Type.Dict(Type.String, Type.Any)), ir)
    }

    @Test
    fun testAnnotationsToIrListPreservesOrderAndDuplicates() {
        val anns = listOf(
            Annotation("Validate", listOf(Annotation.Parameter("min", Annotation.Value.Single("0")))),
            Annotation("Validate", listOf(Annotation.Parameter("max", Annotation.Value.Single("100")))),
        )
        val ir = annotationsToIrList(anns)
        assertEquals(2, ir.values.size)
        val first = (ir.values[0] as LiteralMap).values.getValue("parameters") as LiteralMap
        val second = (ir.values[1] as LiteralMap).values.getValue("parameters") as LiteralMap
        assertEquals(Literal(0L, Type.Integer()), first.values.getValue("min"))
        assertEquals(Literal(100L, Type.Integer()), second.values.getValue("max"))
    }

    @Test
    fun testTypeConvertToGeneratorThreadsFieldAnnotations() {
        val person = TypeWirespec(
            comment = null,
            annotations = emptyList(),
            identifier = definitionId("Person"),
            shape = TypeWirespec.Shape(
                listOf(
                    Field(
                        annotations = listOf(
                            Annotation(
                                name = "Email",
                                parameters = emptyList(),
                            ),
                        ),
                        identifier = fieldId("email"),
                        reference = Reference.Primitive(Reference.Primitive.Type.String(null), false),
                    ),
                    Field(
                        annotations = emptyList(),
                        identifier = fieldId("name"),
                        reference = Reference.Primitive(Reference.Primitive.Type.String(null), false),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val file = person.convertToGenerator()
        val calls = file.collectExpressions<community.flock.wirespec.ir.core.FunctionCall>()
            .filter { it.name == Name.of("generate") && it.receiver is community.flock.wirespec.ir.core.VariableReference }

        assertEquals(2, calls.size, "expected one generator.generate() call per primitive field")

        val emailAnnotations = calls[0].arguments.getValue(Name.of("annotations")) as LiteralList
        assertEquals(1, emailAnnotations.values.size)
        val emailAnn = emailAnnotations.values.single() as LiteralMap
        assertEquals(Literal("Email", Type.String), emailAnn.values.getValue("name"))

        val nameAnnotations = calls[1].arguments.getValue(Name.of("annotations")) as LiteralList
        assertTrue(nameAnnotations.values.isEmpty(), "field with no annotations should pass empty list")
    }
}
