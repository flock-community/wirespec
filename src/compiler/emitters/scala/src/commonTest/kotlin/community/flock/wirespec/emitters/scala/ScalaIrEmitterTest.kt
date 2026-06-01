package community.flock.wirespec.emitters.scala

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
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileRpcTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.generator.ScalaGenerator
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ScalaIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(ScalaIrEmitter())
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
    fun compileTypeTest() {
        val scala = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileEnumTest() {
        val scala = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileRefinedTest() {
        val scala = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileUnionTest() {
        val scala = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileChannelTest() {
        val scala = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileRpcTest() {
        val scala = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileMinimalEndpointTest() {
        val scala = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileFullEndpointTest() {
        val scala = EmitterFixtures.compileFullEndpointTest

        CompileFullEndpointTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileNestedTypeTest() {
        val scala = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun compileComplexModelTest() {
        val scala = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { ScalaIrEmitter() } shouldBeRight scala
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = ScalaIrEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(ScalaGenerator::generate) shouldBe expected
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
