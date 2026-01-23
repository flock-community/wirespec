package community.flock.wirespec.plugin.cli

import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Format.OpenAPIV2
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.test.Test

class CommandLineEntitiesTest {

    @Test
    fun testAllOptions() {
        val opts = Options.entries.flatMap {
            listOfNotNull(
                it.flags.first(),
                when (it) {
                    Options.Input -> "src/commonTest/resources/wirespec"
                    Options.Output -> "src/commonTest/resources/wirespec"
                    Options.Language -> "Wirespec"
                    Options.PackageName -> "packageName"
                    Options.LogLevel -> "error"
                    // To enable flags they only need the flag name. Therefore, the 'argument' part is null.
                    Options.Shared -> null
                    Options.Strict -> null
                },
            )
        }.toTypedArray()
        WirespecCli(
            noopCompiler {
                it.input.shouldHaveSize(2).first().name.value shouldBe "todo"
                it.emitters.shouldHaveSize(1).first().shouldBeTypeOf<WirespecEmitter>()
                it.packageName.shouldNotBeNull().value shouldBe "packageName"
                it.logger.run {
                    shouldDebugLog.shouldBeFalse()
                    shouldInfoLog.shouldBeFalse()
                    shouldWarnLog.shouldBeFalse()
                    shouldErrorLog.shouldBeTrue()
                }
                it.shared.also(::println) shouldBe true
                it.strict shouldBe true
            },
            noopConverter {},
        ).main(arrayOf("compile") + opts)
    }

    @Test
    fun testMinimumCliArgumentsForConvert() {
        WirespecCli(
            noopCompiler { },
            noopConverter {
                it.format.shouldBeTypeOf<Format>() shouldBe OpenAPIV2
                it.input.shouldHaveSize(1).first().name.value shouldBe "keto"
                it.emitters.shouldHaveSize(1).first().shouldBeTypeOf<WirespecEmitter>()
                it.packageName.shouldNotBeNull().value shouldBe DEFAULT_GENERATED_PACKAGE_STRING
                it.logger.run {
                    shouldDebugLog.shouldBeFalse()
                    shouldInfoLog.shouldBeTrue()
                    shouldWarnLog.shouldBeTrue()
                    shouldErrorLog.shouldBeTrue()
                }
                it.shared shouldBe false
                it.strict shouldBe false
            },
        ).main(arrayOf("convert", "-i", "src/commonTest/resources/openapi/keto.json", "openapiv2"))
    }
}
