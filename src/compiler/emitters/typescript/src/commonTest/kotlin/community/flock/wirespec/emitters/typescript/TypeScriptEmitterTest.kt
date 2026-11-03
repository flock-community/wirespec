package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.ir.generator.TypeScriptGenerator
import community.flock.wirespec.compiler.test.CompileAnyTest
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileDefaultValueTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFieldNameSanitizationTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileNestedValidationTest
import community.flock.wirespec.compiler.test.CompilePartTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileRpcTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.compile
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class TypeScriptEmitterTest {

    @Test
    fun compilePartTest() {
        CompilePartTest.compiler { TypeScriptEmitter() }.shouldBeRight() shouldBe CompilePartTest.expandedCompiler { TypeScriptEmitter() }.shouldBeRight()
    }

    @Test
    fun compileFullEndpointTest() {
        val typescript = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileChannelTest() {
        val typescript = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileRpcTest() {
        val typescript = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileEnumTest() {
        val typescript = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileMinimalEndpointTest() {
        val typescript = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileRefinedTest() {
        val typescript = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileUnionTest() {
        val typescript = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileDefaultValueTest() {
        CompileDefaultValueTest.compiler { TypeScriptEmitter() }
            .shouldBeLeft()
            .head.message shouldStartWith "TypeScript does not support default values, but these fields have one: Settings.name, Settings.greeting"
    }

    @Test
    fun compileTypeTest() {
        val typescript = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileAnyTest() {
        val typescript = EmitterFixtures.compileAnyTest

        CompileAnyTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val typescript = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileNestedTypeTest() {
        val typescript = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileComplexModelTest() {
        val typescript = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileNestedValidationTest() {
        val typescript = EmitterFixtures.compileNestedValidationTest

        CompileNestedValidationTest.compiler { TypeScriptEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileCapitalisedFieldValidationTest() {
        // Field names that collide with a global (`Error`) or a type (`UserCompany`) must still be read from `obj`.
        val source = """
            |type BunqId {
            |  id: Integer(1, _)
            |}
            |
            |type UserCompany {
            |  name: String
            |}
            |
            |type Note {
            |  `Id`: BunqId?,
            |  `Error`: BunqId,
            |  `UserCompany`: UserCompany?,
            |  attachment: BunqId?
            |}
        """.trimMargin()

        val compiler = compile(source)
        compiler { TypeScriptEmitter() }.shouldBeRight() shouldContain """
            |export function validateNote(obj: Note): string[] {
            |  return [...obj.Id != null ? validateBunqId(obj.Id).map(e => `Id.${'$'}{e}`) : [] as string[], ...validateBunqId(obj.Error).map(e => `Error.${'$'}{e}`), ...obj.UserCompany != null ? validateUserCompany(obj.UserCompany).map(e => `UserCompany.${'$'}{e}`) : [] as string[], ...obj.attachment != null ? validateBunqId(obj.attachment).map(e => `attachment.${'$'}{e}`) : [] as string[]];
            |}
        """.trimMargin()
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = TypeScriptEmitter()
        emitter.emitShared()?.let(TypeScriptGenerator::generate) shouldBe expected
    }
}
