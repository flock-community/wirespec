package community.flock.wirespec.emitters.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFieldNameSanitizationTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.generator.KotlinGenerator
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KotlinIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinIrEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            EmitterFixtures.testEmitterType,
        )

        val res = emitContext.emitFirst(NodeFixtures.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterEmptyType() {
        val expected = listOf(
            EmitterFixtures.testEmitterEmptyType,
        )

        val res = emitContext.emitFirst(NodeFixtures.emptyType)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            EmitterFixtures.testEmitterRefined,
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = listOf(
            EmitterFixtures.testEmitterEnum,
        )

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    @Test
    fun compileFullEndpointTest() {
        val kotlin = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileChannelTest() {
        val kotlin = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileEnumTest() {
        val kotlin = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileMinimalEndpointTest() {
        val kotlin = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileRefinedTest() {
        val kotlin = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileUnionTest() {
        val kotlin = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileTypeTest() {
        val kotlin = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val kotlin = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileNestedTypeTest() {
        val kotlin = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileComplexModelTest() {
        val kotlin = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { KotlinIrEmitter() } shouldBeRight kotlin
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = KotlinIrEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(KotlinGenerator::generate) shouldBe expected
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map {
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        it.emit(ast, logger).first().result
    }
}
