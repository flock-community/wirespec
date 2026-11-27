package community.flock.wirespec.emitters.php

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.generator.PhpGenerator
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
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlin.test.Test

class PhpEmitterTest {

    @Test
    fun compilePartTest() {
        CompilePartTest.compiler { PhpEmitter() }.shouldBeRight() shouldBe CompilePartTest.expandedCompiler { PhpEmitter() }.shouldBeRight()
    }

    @Test
    fun compileFullEndpointTest() {
        val php = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileChannelTest() {
        val php = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileRpcTest() {
        val php = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileEnumTest() {
        val php = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileMinimalEndpointTest() {
        val php = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileRefinedTest() {
        val php = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileUnionTest() {
        val php = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileDefaultValueTest() {
        CompileDefaultValueTest.compiler { PhpEmitter() }
            .shouldBeLeft()
            .head.message shouldStartWith "Php does not support default values, but these fields have one: Settings.name, Settings.greeting"
    }

    @Test
    fun compileTypeTest() {
        val php = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileAnyTest() {
        val php = EmitterFixtures.compileAnyTest

        CompileAnyTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val php = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileNestedTypeTest() {
        val php = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileComplexModelTest() {
        val php = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun compileNestedValidationTest() {
        val php = EmitterFixtures.compileNestedValidationTest

        CompileNestedValidationTest.compiler { PhpEmitter() } shouldBeRight php
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = PhpEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(PhpGenerator::generate) shouldBe expected
    }
}
