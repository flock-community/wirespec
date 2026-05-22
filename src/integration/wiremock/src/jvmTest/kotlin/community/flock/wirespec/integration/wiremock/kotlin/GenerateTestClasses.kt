package community.flock.wirespec.integration.wiremock.kotlin

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

    private val basePkg = "community.flock.wirespec.integration.wiremock"
    private val javaPkg = "$basePkg.java.generated"
    private val kotlinPkg = "$basePkg.kotlin.generated"

    private val javaEmitter = JavaEmitter(PackageName(javaPkg))
    private val kotlinEmitter = KotlinEmitter(PackageName(kotlinPkg))

    private val baseDir = File("src/jvmTest")

    @Test
    fun generate() {
        val source = File("src/jvmTest/resources/wirespec/todos.ws").readText()
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source)))
            .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })

        val emittedJava = javaEmitter.emit(ast, noLogger)
        val emittedKotlin = kotlinEmitter.emit(ast, noLogger)

        emittedJava.forEach {
            val target = baseDir.resolve("java").resolve(it.file)
            target.parentFile.mkdirs()
            target.writeText(it.result)
        }
        emittedKotlin.forEach {
            val target = baseDir.resolve("kotlin").resolve(it.file)
            target.parentFile.mkdirs()
            target.writeText(it.result)
        }
    }
}
