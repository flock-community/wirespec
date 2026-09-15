package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.generator.JavaGenerator
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavaEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(JavaEmitter())
    }

    @Test
    fun testEmitterType() {
        emitContext.emitFirst(NodeFixtures.type) shouldBe listOf(EmitterFixtures.testEmitterType)
    }

    @Test
    fun testEmitterEmptyType() {
        emitContext.emitFirst(NodeFixtures.emptyType) shouldBe listOf(EmitterFixtures.testEmitterEmptyType)
    }

    @Test
    fun testEmitterRefined() {
        emitContext.emitFirst(NodeFixtures.refined) shouldBe listOf(EmitterFixtures.testEmitterRefined)
    }

    @Test
    fun testEmitterEnum() {
        emitContext.emitFirst(NodeFixtures.enum) shouldBe listOf(EmitterFixtures.testEmitterEnum)
    }

    @Test
    fun compileFullEndpointTest() {
        CompileFullEndpointTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileFullEndpointTest
    }

    @Test
    fun compileChannelTest() {
        CompileChannelTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileChannelTest
    }

    @Test
    fun compileRpcTest() {
        CompileRpcTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileRpcTest
    }

    @Test
    fun compileEnumTest() {
        CompileEnumTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileEnumTest
    }

    @Test
    fun compileMinimalEndpointTest() {
        CompileMinimalEndpointTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileMinimalEndpointTest
    }

    @Test
    fun compileRefinedTest() {
        CompileRefinedTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileRefinedTest
    }

    @Test
    fun compileUnionTest() {
        CompileUnionTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileUnionTest
    }

    @Test
    fun compileTypeTest() {
        CompileTypeTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileTypeTest
    }

    @Test
    fun compileAnyTest() {
        CompileAnyTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileAnyTest
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        CompileFieldNameSanitizationTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileFieldNameSanitizationTest
    }

    @Test
    fun compileNestedTypeTest() {
        CompileNestedTypeTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileNestedTypeTest
    }

    @Test
    fun compileComplexModelTest() {
        CompileComplexModelTest.compiler { JavaEmitter() } shouldBeRight EmitterFixtures.compileComplexModelTest
    }

    @Test
    fun sharedOutputTest() {
        val emitter = JavaEmitter(emitShared = EmitShared(true))
        emitter.emitShared()?.let(JavaGenerator::generate) shouldBe EmitterFixtures.sharedOutputTest
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
