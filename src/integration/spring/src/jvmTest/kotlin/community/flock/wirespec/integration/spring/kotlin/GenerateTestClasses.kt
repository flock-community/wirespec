package community.flock.wirespec.integration.spring.kotlin

import community.flock.wirespec.integration.spring.kotlin.emit.SpringKotlinEmitter
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import kotlin.test.Test
import java.io.File

class GenerateTestClasses {

    val basePkg = "community.flock.wirespec.integration.spring.kotlin"
    val kotlinPkg = "${basePkg}.generated"

    val kotlinEmitter = SpringKotlinEmitter(kotlinPkg)

    fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    val baseDir = File("src/jvmTest")
    val outputDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))

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

