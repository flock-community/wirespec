package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.ir.generator.TypeScriptGenerator
import community.flock.wirespec.compiler.test.CompileAnyTest
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFieldNameSanitizationTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileRpcTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TypeScriptEmitterTest {

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
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = TypeScriptEmitter()
        emitter.emitShared()?.let(TypeScriptGenerator::generate) shouldBe expected
    }
}
