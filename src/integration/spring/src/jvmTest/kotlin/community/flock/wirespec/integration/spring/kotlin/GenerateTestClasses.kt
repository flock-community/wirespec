package community.flock.wirespec.integration.spring.kotlin

import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.integration.spring.kotlin.emit.SpringKotlinEmitter
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import java.io.File
import kotlin.test.Test

class GenerateTestClasses {

    private val basePkg = "community.flock.wirespec.integration.spring.kotlin"
    private val kotlinPkg = "${basePkg}.generated"

    private val kotlinEmitter = SpringKotlinEmitter(kotlinPkg, noLogger)

    private fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    private val baseDir = File("src/jvmTest")
    private val outputDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))

    @Test
    fun generate() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenApiV3Parser.parse(petstoreFile)
        val emittedKotlin = kotlinEmitter.emit(ast)

        outputDir.mkdirs()
        emittedKotlin.forEach {
            outputDir.resolve("Petstorev3.kt").writeText(it.result)
        }
    }
}
