package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.ir.generator.TypeScriptGenerator
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TypeScriptIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val typescript = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileChannelTest() {
        val typescript = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileEnumTest() {
        val typescript = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileMinimalEndpointTest() {
        val typescript = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileRefinedTest() {
        val typescript = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileUnionTest() {
        val typescript = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileTypeTest() {
        val typescript = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileNestedTypeTest() {
        val typescript = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileComplexModelTest() {
        val typescript = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = TypeScriptIrEmitter()
        emitter.emitShared()?.let(TypeScriptGenerator::generate) shouldBe expected
    }
}
