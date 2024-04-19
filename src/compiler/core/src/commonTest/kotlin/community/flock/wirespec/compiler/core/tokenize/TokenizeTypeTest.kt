package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.CustomRegex
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.Equals
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Pipe
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizeTypeTest {

    @Test
    fun testTypeTokenize() {
        val source = """
            type Foo {
                bar: String
            }
        """.trimIndent()

        val expected = listOf(
            WsTypeDef, CustomType, LeftCurly, CustomValue,
            Colon, WsString, RightCurly, EndOfProgram,
        )

        WirespecSpec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }

    @Test
    fun testRefinedTypeTokenize() {
        val source = """
            type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g
        """.trimIndent()

        val expected = listOf(
            WsTypeDef, CustomType, CustomRegex, EndOfProgram,
        )

        WirespecSpec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }

    @Test
    fun testUnionTypeTokenize() {
        val source = """
            type Foo = Bar | Bal
        """.trimIndent()

        val expected = listOf(
            WsTypeDef, CustomType, Equals, CustomType,
            Pipe, CustomType, EndOfProgram,
        )

        WirespecSpec.tokenize(source).removeWhiteSpace()
            .shouldNotBeEmpty()
            .also { it.size shouldBe expected.size }
            .map { it.type }.shouldNotContain(Invalid)
            .onEachIndexed { index, tokenType -> tokenType shouldBe expected[index] }
    }
}
