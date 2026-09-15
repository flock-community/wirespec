package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.generator.PythonGenerator
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

class PythonEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val python = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileChannelTest() {
        val python = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileRpcTest() {
        val python = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileEnumTest() {
        val python = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileMinimalEndpointTest() {
        val python = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileRefinedTest() {
        val python = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileUnionTest() {
        val python = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileTypeTest() {
        val python = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileAnyTest() {
        val python = EmitterFixtures.compileAnyTest

        CompileAnyTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val python = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileNestedTypeTest() {
        val python = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun compileComplexModelTest() {
        val python = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = PythonEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(PythonGenerator::generate) shouldBe expected
    }
}
