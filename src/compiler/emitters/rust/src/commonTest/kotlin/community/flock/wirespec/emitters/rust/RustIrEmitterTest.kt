package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileRpcTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.ir.generator.RustGenerator
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RustIrEmitterTest {

    @Test
    fun compileEnumTest() {
        val rust = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileTypeTest() {
        val rust = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileChannelTest() {
        val rust = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileRpcTest() {
        val rust = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileRefinedTest() {
        val rust = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileUnionTest() {
        val rust = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileMinimalEndpointTest() {
        val rust = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileFullEndpointTest() {
        val rust = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileNestedTypeTest() {
        val rust = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileComplexModelTest() {
        val rust = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = RustIrEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(RustGenerator::generate) shouldBe expected
    }
}
