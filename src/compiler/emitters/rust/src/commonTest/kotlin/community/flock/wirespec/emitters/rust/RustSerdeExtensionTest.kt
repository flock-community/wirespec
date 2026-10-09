package community.flock.wirespec.emitters.rust

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.CompileTypeTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class RustSerdeExtensionTest {

    private fun output() = CompileTypeTest.compiler { RustEmitter().applyExtensions(nonEmptyListOf(RustSerdeExtension())) }.shouldBeRight()

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
        CompileTypeTest.compiler { RustEmitter() }.shouldBeRight() shouldNotContain "serde"
    }
}
