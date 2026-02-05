package community.flock.wirespec.integration.spring.kotlin

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.integration.spring.java.emit.SpringJavaEmitter
import community.flock.wirespec.integration.spring.kotlin.emit.SpringKotlinEmitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import io.kotest.matchers.shouldBe
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
    private val javaOutputDir = baseDir.resolve("java").resolve(pkgToPath(javaPkg))
    private val modules = listOf("model", "endpoint", "channel")

    @Test
    fun generateKotlin() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast =
            OpenAPIV3Parser.parse(ModuleContent(FileUri("src/jvmTest/resources/petstore.json"), petstoreFile), false)
        val emittedKotlin = kotlinEmitter.emit(ast, noLogger)

        modules.forEach { kotlinOutputDir.resolve(it).mkdirs() }
        emittedKotlin.forEach {
            baseDir.resolve("kotlin").resolve(it.file).writeText(it.result)
        }

        val ctx: CompilationContext = object : CompilationContext {
            override val emitters = nonEmptySetOf(kotlinEmitter)
            override val logger: Logger = noLogger
        }
        val todoFile = File("src/jvmTest/resources/todo.ws").readText()
        val result = ctx.compile(nonEmptyListOf(ModuleContent(FileUri("N/A"), todoFile)))
        result.isRight() shouldBe true
        result.getOrNull()?.forEach {
            baseDir.resolve("kotlin").resolve(it.file).writeText(it.result)
        }
    }

    @Test
    fun generateJava() {
        val petstoreFile = File("src/jvmTest/resources/petstore.json").readText()
        val ast =
            OpenAPIV3Parser.parse(ModuleContent(FileUri("src/jvmTest/resources/petstore.json"), petstoreFile), false)
        val emittedJava = javaEmitter.emit(ast, noLogger)

        modules.forEach { javaOutputDir.resolve(it).mkdirs() }
        emittedJava
            .filter { "Wirespec" !in it.file }.forEach {
                baseDir.resolve("java").resolve(it.file).writeText(it.result)
            }

        val ctx: CompilationContext = object : CompilationContext {
            override val emitters = nonEmptySetOf(javaEmitter)
            override val logger: Logger = noLogger
        }
        val todoFile = File("src/jvmTest/resources/todo.ws").readText()
        val result = ctx.compile(nonEmptyListOf(ModuleContent(FileUri("N/A"), todoFile)))
        result.isRight() shouldBe true
        result.getOrNull()?.forEach {
            baseDir.resolve("java").resolve(it.file).writeText(it.result)
        }
    }
}
