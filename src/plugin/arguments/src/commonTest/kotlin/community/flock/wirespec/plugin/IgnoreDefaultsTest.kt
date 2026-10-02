package community.flock.wirespec.plugin

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class IgnoreDefaultsTest {

    private val source =
        // language=ws
        """
        |type Settings {
        |  name: String = "anonymous",
        |  nickname: String? = null
        |}
        """.trimMargin()

    private fun compileToKotlin(ignoreDefaults: Boolean): String {
        var output = ""
        compile(
            CompilerArguments(
                input = nonEmptySetOf(Source(Name("settings"), source)),
                emitters = nonEmptySetOf(KotlinEmitter()),
                writer = { emitted -> output = emitted.joinToString("\n") { it.result } },
                error = { error(it) },
                packageName = PackageName("community.flock.wirespec.generated"),
                logger = noLogger,
                shared = false,
                strict = false,
                ignoreDefaults = ignoreDefaults,
            ),
        )
        return output
    }

    @Test
    fun testDefaultsAreEmitted() {
        compileToKotlin(ignoreDefaults = false).run {
            shouldContain("val name: String = \"anonymous\"")
            shouldContain("val nickname: String? = null")
        }
    }

    @Test
    fun testDefaultsAreIgnored() {
        compileToKotlin(ignoreDefaults = true).run {
            shouldContain("val name: String,")
            shouldContain("val nickname: String?\n")
            shouldNotContain(" = \"anonymous\"")
        }
    }
}
