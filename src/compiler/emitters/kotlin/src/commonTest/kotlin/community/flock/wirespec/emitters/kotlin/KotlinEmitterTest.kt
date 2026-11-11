package community.flock.wirespec.emitters.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.SyncExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.ir.generator.KotlinGenerator
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.test.compile
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class KotlinEmitterTest {

    @Test
    fun compilePartTest() {
        CompilePartTest.compiler { KotlinEmitter() }.shouldBeRight() shouldBe CompilePartTest.expandedCompiler { KotlinEmitter() }.shouldBeRight()
    }

    @Test
    fun aSpecWithOnlyPartsHasNothingToEmit() {
        CompilePartTest.partsOnlyCompiler { KotlinEmitter() }
            .shouldBeLeft()
            .single().message shouldBe "Nothing to emit for Kotlin: the spec only contains parts, " +
            "and a part is only generated through the types, endpoints and rpcs that spread it."
    }

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinEmitter())
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

        CompileFullEndpointTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileChannelTest() {
        val kotlin = EmitterFixtures.compileChannelTest

        CompileChannelTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileRpcTest() {
        val kotlin = EmitterFixtures.compileRpcTest

        CompileRpcTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileEnumTest() {
        val kotlin = EmitterFixtures.compileEnumTest

        CompileEnumTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileMinimalEndpointTest() {
        val kotlin = EmitterFixtures.compileMinimalEndpointTest

        CompileMinimalEndpointTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun clientReferencesTheEndpointByItsDeclaredName() {
        val kotlin = compile(
            """
            |endpoint ToggleIRoas POST /items/{id: Integer}/iroas ?{active: Boolean} -> {
            |  200 -> Unit
            |}
            """.trimMargin(),
        ).invoke { KotlinEmitter() }.shouldBeRight()

        kotlin shouldContain "object ToggleIRoas : Wirespec.Endpoint"
        kotlin shouldContain "import community.flock.wirespec.generated.endpoint.ToggleIRoas\n"
        kotlin shouldContain "import community.flock.wirespec.generated.client.ToggleIRoasClient\n"
        kotlin shouldContain "data class ToggleIRoasClient("
        kotlin shouldNotContain "ToggleIROas"
    }

    @Test
    fun syncEmitsTheHandlerWithoutSuspend() {
        val kotlin = EmitterFixtures.compileMinimalEndpointTest
            .replace("suspend fun getTodos(request: Request)", "fun getTodos(request: Request)")

        CompileMinimalEndpointTest.compiler { KotlinEmitter().applyExtensions(nonEmptyListOf(SyncExtension(FileExtension.Kotlin))) } shouldBeRight kotlin
    }

    @Test
    fun syncEmitsTheRpcServiceWithoutSuspend() {
        val kotlin = EmitterFixtures.compileRpcTest
            .replace("suspend fun getUser(", "fun getUser(")
            .replace("suspend fun ping(", "fun ping(")

        CompileRpcTest.compiler { KotlinEmitter().applyExtensions(nonEmptyListOf(SyncExtension(FileExtension.Kotlin))) } shouldBeRight kotlin
    }

    @Test
    fun compileRefinedTest() {
        val kotlin = EmitterFixtures.compileRefinedTest

        CompileRefinedTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileUnionTest() {
        val kotlin = EmitterFixtures.compileUnionTest

        CompileUnionTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileTypeTest() {
        val kotlin = EmitterFixtures.compileTypeTest

        CompileTypeTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileAnyTest() {
        val kotlin = EmitterFixtures.compileAnyTest

        CompileAnyTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileFieldNameSanitizationTest() {
        val kotlin = EmitterFixtures.compileFieldNameSanitizationTest

        CompileFieldNameSanitizationTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileNestedTypeTest() {
        val kotlin = EmitterFixtures.compileNestedTypeTest

        CompileNestedTypeTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileComplexModelTest() {
        val kotlin = EmitterFixtures.compileComplexModelTest

        CompileComplexModelTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileNestedValidationTest() {
        val kotlin = EmitterFixtures.compileNestedValidationTest

        CompileNestedValidationTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun compileDefaultValueTest() {
        val kotlin = EmitterFixtures.compileDefaultValueTest

        CompileDefaultValueTest.compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun sharedOutputTest() {
        val expected = EmitterFixtures.sharedOutputTest

        val emitter = KotlinEmitter(emitShared = EmitShared(true))
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
