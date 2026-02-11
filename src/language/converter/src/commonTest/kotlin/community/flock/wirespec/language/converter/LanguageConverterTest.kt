package community.flock.wirespec.language.converter

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.enum
import community.flock.wirespec.language.core.file
import community.flock.wirespec.language.core.struct
import community.flock.wirespec.language.core.union
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum
import community.flock.wirespec.compiler.core.parse.ast.Refined as AstRefined
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

class LanguageConverterTest {

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
                field("bar", string)
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
                entry("FOO")
                entry("BAR")
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
                    implements(Type.Custom("MyUnion"))
                    field("a", string)
                }
            },
            file("Bar") {
                struct("Bar") {
                    implements(Type.Custom("MyUnion"))
                    field("b", string)
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
            }
        }

        assertEquals(expected, result)
    }
}
