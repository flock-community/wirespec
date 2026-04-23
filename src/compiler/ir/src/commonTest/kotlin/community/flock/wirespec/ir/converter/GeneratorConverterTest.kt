package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Literal
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
}
