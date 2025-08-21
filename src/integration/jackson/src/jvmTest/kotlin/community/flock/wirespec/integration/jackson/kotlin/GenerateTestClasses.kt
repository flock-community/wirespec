package community.flock.wirespec.integration.jackson.kotlin

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import java.io.File
import kotlin.test.Test

class GenerateTestClasses {

    private val basePkg = "community.flock.wirespec.integration.jackson"
    private val javaPkg = "$basePkg.java.generated"
    private val kotlinPkg = "$basePkg.kotlin.generated"

    private val javaEmitter = JavaEmitter(PackageName(javaPkg))
    private val kotlinEmitter = KotlinEmitter(PackageName(kotlinPkg))

    private fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    private val baseDir = File("src/jvmTest")
    private val javaDir = baseDir.resolve("java").resolve(pkgToPath(javaPkg))
    private val kotlinDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))
    private val modules = listOf("model")

    @Test
    fun generate() {
        val todoFile = File("src/commonTest/resources/wirespec/todos.ws").readText()
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), todoFile)))
            .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

        val emittedJava = javaEmitter.emit(ast, noLogger)
        val emittedKotlin = kotlinEmitter.emit(ast, noLogger)

        modules.forEach { javaDir.resolve(it).mkdirs() }
        emittedJava
            .filter { !it.file.contains("Wirespec") }.forEach {
                baseDir.resolve("java").resolve(it.file).writeText(it.result)
            }

        modules.forEach { kotlinDir.resolve(it).mkdirs() }
        emittedKotlin.forEach {
            baseDir.resolve("kotlin").resolve(it.file).writeText(it.result)
        }
    }
}
