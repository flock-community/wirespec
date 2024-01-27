package community.flock.wirespec.integration.spring

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v3.OpenApiParser
import java.io.File
import org.junit.jupiter.api.Test

class GenerateTestClasses {

    val basePkg = "community.flock.wirespec.integration.spring"
    val kotlinPkg = "${basePkg}.generated"

    val kotlinEmitter = KotlinEmitter(kotlinPkg)

    fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    val baseDir = File("src/jvmTest")
    val outputDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))

    @Test
    fun generate(){
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenApiParser.parse(petstoreFile)
        val emittedKotlin = kotlinEmitter.emit(ast)

        outputDir.mkdirs()
        emittedKotlin.forEach {
            outputDir.resolve("Petstorev3.kt").writeText(it.result)
        }
    }
}

