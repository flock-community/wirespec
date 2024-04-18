package community.flock.wirespec.integration.jackson.kotlin

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.utils.noLogger
import java.io.File
import kotlin.test.Test

class GenerateTestClasses {

    val basePkg = "community.flock.wirespec.integration.jackson"
    val javaPkg = "${basePkg}.java.generated"
    val kotlinPkg = "${basePkg}.kotlin.generated"

    val javaEmitter = JavaEmitter(javaPkg)
    val kotlinEmitter = KotlinEmitter(kotlinPkg)

    fun pkgToPath(pkg: String) = pkg.split(".").joinToString("/")

    val baseDir = File("src/jvmTest")
    val javaDir = baseDir.resolve("java").resolve(pkgToPath(javaPkg))
    val kotlinDir = baseDir.resolve("kotlin").resolve(pkgToPath(kotlinPkg))

    @Test
    fun generate() {
        val todoFile = File("src/commonTest/resources/wirespec/todos.ws").readText()
        val ast = WirespecSpec.parse(todoFile)(noLogger)
            .fold({ error("Cannot parse wirespec: ${it.first().message}") }, { it })
            .filterIsInstance<Definition>()

        val emittedJava = javaEmitter.emit(ast)
        val emittedKotlin = kotlinEmitter.emit(ast)

        javaDir.mkdirs()
        emittedJava.forEach {
            javaDir.resolve("${it.typeName}.java").writeText(it.result)
        }

        kotlinDir.mkdirs()
        emittedKotlin.forEach {
            kotlinDir.resolve("Todo.kt").writeText(it.result)
        }
    }
}

