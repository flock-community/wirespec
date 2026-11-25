package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.generator.RustGenerator
import community.flock.wirespec.compiler.test.CompileAnyTest
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileDefaultValueTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFieldNameSanitizationTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileLiteralTest
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

class RustEmitterTest {

    @Test
    fun compilePartTest() {
        CompilePartTest.compiler { RustEmitter() }.shouldBeRight() shouldBe CompilePartTest.expandedCompiler { RustEmitter() }.shouldBeRight()
    }

    @Test
    fun compileEnumTest() {
        val rust = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileDefaultValueTest() {
        CompileDefaultValueTest.compiler { RustEmitter() }
            .shouldBeLeft()
            .head.message shouldStartWith "Rust does not support default values, but these fields have one: Settings.name, Settings.greeting"
    }

    @Test
    fun compileTypeTest() {
        val rust = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileAnyTest() {
        val rust = EmitterFixtures.compileAnyTest

        CompileAnyTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val rust = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileChannelTest() {
        val rust = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileRpcTest() {
        val rust = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileRefinedTest() {
        val rust = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileUnionTest() {
        val rust = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileMinimalEndpointTest() {
        val rust = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileFullEndpointTest() {
        val rust = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileNestedTypeTest() {
        val rust = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileComplexModelTest() {
        val rust = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileNestedValidationTest() {
        val rust = EmitterFixtures.compileNestedValidationTest

        CompileNestedValidationTest.compiler { RustEmitter() } shouldBeRight rust
    }

    @Test
    fun compileLiteralTest() {
        CompileLiteralTest.compiler { RustEmitter() } shouldBeRight EmitterFixtures.compileLiteralTest
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = RustEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(RustGenerator::generate) shouldBe expected
    }

    @Test
    fun modFilesListTheModulesOfEveryFile() {
        val directory = "community/flock/wirespec/generated/"
        val files = listOf("model/cat", "model/owner", "channel/cats", "generator/cat_generator")
            .map { File(Name.of(directory + it), emptyList()) }

        RustEmitter(emitShared = EmitShared(true)).emitIndex(files).associate { it.name.value().removePrefix(directory) to RustGenerator.generate(it) } shouldBe mapOf(
            "mod" to "#![allow(warnings)]\npub mod model;\npub mod endpoint;\npub mod channel;\npub mod generator;\npub mod wirespec;\n",
            "model/mod" to "pub mod cat;\npub mod owner;\n",
            "endpoint/mod" to "\n",
            "channel/mod" to "pub mod cats;\n",
            "generator/mod" to "pub mod cat_generator;\n",
        )
    }

    @Test
    fun keywordWithoutRawIdentifierGetsAnUnderscore() {
        compile("type Node { self: String, `type`: String }")
            .invoke { RustEmitter() }
            .shouldBeRight() shouldContain "pub self_: String,\n    pub r#type: String,"
    }
}
