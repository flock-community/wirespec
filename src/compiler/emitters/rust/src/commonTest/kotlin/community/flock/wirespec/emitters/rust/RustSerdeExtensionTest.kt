package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.ir.extension.applyExtensions
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class RustSerdeExtensionTest {

    private fun output() = CompileTypeTest.compiler { RustIrEmitter().applyExtensions(listOf(RustSerdeExtension())) }.shouldBeRight()

    @Test
    fun modelStructsGetSerdeDerives() {
        output() shouldContain "#[derive(serde::Serialize, serde::Deserialize)]\npub struct Request {"
    }

    @Test
    fun fieldsKeepTheirWirespecNamesOnTheWire() {
        val output = output()

        output shouldContain """#[serde(rename = "type")]"""
        output shouldContain """#[serde(rename = "BODY_TYPE")]"""
        output shouldContain """#[serde(rename = "url")]"""
    }

    @Test
    fun withoutTheExtensionTheOutputStaysDependencyFree() {
        CompileTypeTest.compiler { RustIrEmitter() }.shouldBeRight() shouldNotContain "serde"
    }
}
