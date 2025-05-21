package community.flock.wirespec.integration.spring.kotlin

import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.plus
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.integration.spring.java.emit.SpringJavaEmitter
import community.flock.wirespec.integration.spring.kotlin.emit.SpringKotlinEmitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import java.io.File
import kotlin.test.Test

class GenerateTestClasses {

    private val basePkg = PackageName("community.flock.wirespec.integration.spring")
    private val kotlinPkg = basePkg + "kotlin.generated"
    private val javaPkg = basePkg + "java.generated"

    private val kotlinEmitter = SpringKotlinEmitter(kotlinPkg)
    private val javaEmitter = SpringJavaEmitter(javaPkg)

    private fun pkgToPath(pkg: PackageName) = pkg.value.split(".").joinToString("/")

    private val baseDir = File("src/jvmTest")
    private val kotlinOutputDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))
    private val javaOutputDir = baseDir.resolve("kotlin").resolve(pkgToPath(javaPkg))
    private val modules = listOf("model", "endpoint", "channel")

    @Test
    fun generateKotlin() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenAPIV3Parser.parse(ModuleContent("src/jvmTest/resources/petstore.json", petstoreFile), false)
        val emittedKotlin = kotlinEmitter.emit(ast, noLogger)

        modules.forEach { kotlinOutputDir.resolve(it).mkdirs() }
        emittedKotlin.forEach {
            baseDir.resolve("kotlin").resolve(it.file).writeText(it.result)
        }
    }

    @Test
    fun generateJava() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast = OpenAPIV3Parser.parse(ModuleContent("src/jvmTest/resources/petstore.json", petstoreFile), false)
        val emittedJava = javaEmitter.emit(ast, noLogger)

        modules.forEach { javaOutputDir.resolve(it).mkdirs() }
        emittedJava
            .filter { "Wirespec" !in it.file }.forEach {
                baseDir.resolve("kotlin").resolve(it.file).writeText(it.result)
            }
    }
}
