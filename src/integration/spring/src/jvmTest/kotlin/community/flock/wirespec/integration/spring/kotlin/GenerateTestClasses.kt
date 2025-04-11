package community.flock.wirespec.integration.spring.kotlin

import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.integration.spring.java.emit.SpringJavaEmitter
import community.flock.wirespec.integration.spring.kotlin.emit.SpringKotlinEmitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import java.io.File
import kotlin.test.Test

class GenerateTestClasses {

    private val basePkg = "community.flock.wirespec.integration.spring"
    private val kotlinPkg = "$basePkg.kotlin.generated"
    private val javaPkg = "$basePkg.java.generated"

    private val kotlinEmitter = SpringKotlinEmitter(kotlinPkg)
    private val javaEmitter = SpringJavaEmitter(javaPkg)

    private fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    private val baseDir = File("src/jvmTest")
    private val kotlinOutputDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))
    private val javaOutputDir = baseDir.resolve("kotlin").resolve(pkgToPath(javaPkg))

    @Test
    fun generateKotlin() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenAPIV3Parser.parse(ModuleContent("src/jvmTest/resources/petstore.json", petstoreFile))
        val emittedKotlin = kotlinEmitter.emit(ast, noLogger)

        kotlinOutputDir.mkdirs()
        emittedKotlin.forEach {
            kotlinOutputDir.resolve("Petstorev3.kt").writeText(it.result)
        }
    }

    @Test
    fun generateJava() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenAPIV3Parser.parse(ModuleContent("src/jvmTest/resources/petstore.json", petstoreFile))
        val emittedJava = javaEmitter.emit(ast, noLogger)

        javaOutputDir.mkdirs()
        emittedJava
            .filter { "Wirespec" !in it.typeName }.forEach {
                baseDir.resolve("kotlin").resolve(it.typeName).writeText(it.result)
            }
    }
}
