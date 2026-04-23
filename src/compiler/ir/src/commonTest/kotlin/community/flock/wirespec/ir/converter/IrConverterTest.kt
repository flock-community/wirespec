package community.flock.wirespec.ir.converter

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Shared
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.core.Constraint
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.enum
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.ir.core.struct
import community.flock.wirespec.ir.core.union
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum
import community.flock.wirespec.compiler.core.parse.ast.Refined as AstRefined
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

class IrConverterTest {

    private inline fun <reified T> parse(source: String): T = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source)))
        .map { it.modules.flatMap(Module::statements) }
        .getOrElse { fail("Parse failed: $it") }
        .first()
        .let { it as? T ?: fail("Expected ${T::class.simpleName} but got ${it::class.simpleName}") }

    private fun parseNodes(source: String): List<Definition> = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source)))
        .map { it.modules.flatMap(Module::statements) }
        .getOrElse { fail("Parse failed: $it") }

    @Test
    fun testLanguageConverter() {
        val source = """
            type Foo {
                bar: String
            }
        """.trimIndent()

        val result = parse<AstType>(source).convert()

        val expected = file("Foo") {
            struct("Foo") {
                implements(Type.Custom("Wirespec.Model"))
                field(Name(listOf("bar")), string)
                function("validate", isOverride = true) {
                    returnType(Type.Array(Type.String))
                    returns(LiteralList(emptyList(), Type.String))
                }
            }
        }

        assertEquals(expected, result)
    }

    @Test
    fun testEnumConversion() {
        val source = """
            enum MyEnum {
                FOO, BAR
            }
        """.trimIndent()

        val result = parse<AstEnum>(source).convert()

        val expected = file("MyEnum") {
            enum("MyEnum", Type.Custom("Wirespec.Enum")) {
                entry("FOO", "\"FOO\"")
                entry("BAR", "\"BAR\"")
            }
        }

        assertEquals(expected, result)
    }

    @Test
    fun testUnionConversion() {
        val source = """
            type MyUnion = Foo | Bar
            type Foo { a: String }
            type Bar { b: String }
        """.trimIndent()

        val result = parseNodes(source).map { it.convert() }

        val expected = listOf(
            file("MyUnion") {
                union("MyUnion") {
                    member("Foo")
                    member("Bar")
                }
            },
            file("Foo") {
                struct("Foo") {
                    implements(Type.Custom("Wirespec.Model"))
                    implements(Type.Custom("MyUnion"))
                    field(Name(listOf("a")), string)
                    function("validate", isOverride = true) {
                        returnType(Type.Array(Type.String))
                        returns(LiteralList(emptyList(), Type.String))
                    }
                }
            },
            file("Bar") {
                struct("Bar") {
                    implements(Type.Custom("Wirespec.Model"))
                    implements(Type.Custom("MyUnion"))
                    field(Name(listOf("b")), string)
                    function("validate", isOverride = true) {
                        returnType(Type.Array(Type.String))
                        returns(LiteralList(emptyList(), Type.String))
                    }
                }
            },
        )

        assertEquals(expected, result)
    }

    @Test
    fun testRefinedConversion() {
        val source = """
            type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)
        """.trimIndent()

        val result = parse<AstRefined>(source).convert()

        val expected = file("DutchPostalCode") {
            struct("DutchPostalCode") {
                implements(type("Wirespec.Refined", string))
                field("value", Type.String)
                function("validate") {
                    returnType(Type.Boolean)
                    returns(
                        Constraint.RegexMatch(
                            pattern = "^([0-9]{4}[A-Z]{2})\$",
                            rawValue = "/^([0-9]{4}[A-Z]{2})\$/g",
                            value = VariableReference(Name.of("value")),
                        ),
                    )
                }
                function("toString") {
                    returnType(Type.String)
                    returns(VariableReference(Name.of("value")))
                }
            }
        }

        assertEquals(expected, result)
    }

    @Test
    fun testRefinedIntegerConversion() {
        val source = """
            type Age = Integer(0, 150)
        """.trimIndent()

        val result = parse<AstRefined>(source).convert()

        val expected = file("Age") {
            struct("Age") {
                implements(type("Wirespec.Refined", Type.Integer(Precision.P64)))
                field("value", Type.Integer(Precision.P64))
                function("validate") {
                    returnType(Type.Boolean)
                    returns(
                        Constraint.BoundCheck(
                            min = "0",
                            max = "150",
                            value = VariableReference(Name.of("value")),
                        ),
                    )
                }
                function("toString") {
                    returnType(Type.String)
                    returns(
                        FunctionCall(
                            receiver = VariableReference(Name.of("value")),
                            name = Name.of("toString"),
                        ),
                    )
                }
            }
        }

        assertEquals(expected, result)
    }

    @Test
    fun testSharedContainsGeneratorField() {
        val file = Shared("com.example").convert()
        val wirespecNamespace = file.elements
            .filterIsInstance<Namespace>()
            .first { it.name == Name.of("Wirespec") }
        val interfaces = wirespecNamespace.elements
            .filterIsInstance<Interface>()
        val structs = wirespecNamespace.elements
            .filterIsInstance<Struct>()

        val generatorField = interfaces.first { it.name == Name.of("GeneratorField") }
        assertTrue(generatorField.isSealed, "GeneratorField should be sealed")

        val generator = interfaces.first { it.name == Name.of("Generator") }
        val generateFn = generator.elements
            .filterIsInstance<Function>()
            .first { it.name == Name.of("generate") }
        assertEquals(3, generateFn.parameters.size, "generate() should take path, type, and field")
        assertEquals(Type.Reflect, generateFn.parameters[1].type, "second param must be Type.Reflect")

        val expectedVariants = setOf(
            "GeneratorFieldString", "GeneratorFieldInteger", "GeneratorFieldNumber",
            "GeneratorFieldBoolean", "GeneratorFieldBytes", "GeneratorFieldEnum",
            "GeneratorFieldUnion", "GeneratorFieldArray", "GeneratorFieldNullable",
            "GeneratorFieldDict",
        )
        val actualVariants = structs.map { it.name.value() }.toSet()
        assertTrue(
            expectedVariants.all { it in actualVariants },
            "Missing variants: ${expectedVariants - actualVariants}",
        )
    }
}
