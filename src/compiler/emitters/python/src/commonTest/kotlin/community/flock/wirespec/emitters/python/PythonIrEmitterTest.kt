package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.ir.core.RawElement
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PythonIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val python = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileChannelTest() {
        val python = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileEnumTest() {
        val python = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileMinimalEndpointTest() {
        val python = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileRefinedTest() {
        val python = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileUnionTest() {
        val python = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileTypeTest() {
        val python = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileNestedTypeTest() {
        val python = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun compileComplexModelTest() {
        val python = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { PythonIrEmitter() } shouldBeRight python
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = PythonIrEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.elements?.filterIsInstance<RawElement>()?.joinToString("") { it.code } shouldBe expected
    }
}
