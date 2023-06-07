package community.flock.wirespec.compiler.cli

import community.flock.wirespec.compiler.cli.io.Directory
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test


class CliTest {

    fun outputDir() = "/tmp/${getRandomString(8)}"
    val inputDir = "src/commonTest/resources"

    @Test
    fun testMainHelp() {
        main(arrayOf("--help"))
    }

    @Test
    fun testMainOutput() {
        main(arrayOf(inputDir, "-o", outputDir()))
    }

    @Test
    fun testMainJavaPackage() {
        main(arrayOf(inputDir, "-o", outputDir(), "-l",  "Kotlin", "-l",  "Java", "-p", "community.flock.next"))
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}