package community.flock.wirespec.plugin.cli

import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class WirespecCliErrorsTest {

    @Test
    fun testNoArgs() = inContext(emptyArray()) {
        statusCode shouldBe 0
        stdout shouldBe """
            |Usage: wirespec [<options>] <command> [<args>]...
            |
            |Options:
            |  -h, --help  Show this message and exit
            |
            |Commands:
            |  compile
            |  convert
            |
        """.trimMargin()
    }

    @Test
    fun testOnlyCompileArg() = inContext(arrayOf("compile")) {
        statusCode shouldBe 1
        stderr shouldBe """
            |No input file, directory, or stdin received.
            |
        """.trimMargin()
    }

    @Test
    fun testOnlyConvertArg() = inContext(arrayOf("convert")) {
        statusCode shouldBe 1
        stderr shouldBe """
            |Usage: wirespec convert [<options>] <format> [<stdin>]
            |
            |Error: missing argument <format>
            |
        """.trimMargin()
    }

    @Test
    fun testCannotAccessFileOrDirectory() = inContext(arrayOf("compile", "-l", "Kotlin", "-i", "yolo")) {
        statusCode shouldBe 1
        stderr shouldBe "Cannot access file or directory: yolo.\n"
    }

    @Test
    fun testChooseALogLevel() = inContext(arrayOf("compile", "-l", "Kotlin", "-i", "src/commonTest/resources/wirespec", "--log-level", "yolo")) {
        statusCode shouldBe 1
        stderr shouldBe "Choose one of these log levels: DEBUG, INFO, WARN, ERROR.\n"
    }

    @Test
    fun testConvertNeedsAFile() = inContext(arrayOf("convert", "-i", "src/commonTest/resources/openapi", "OpenAPIV2")) {
        statusCode shouldBe 1
        stderr shouldBe "To convert, please specify a file.\n"
    }

    private fun inContext(args: Array<out String>, block: CliktCommandTestResult.() -> Unit) = noopCli().test(argv = args).block()
}
