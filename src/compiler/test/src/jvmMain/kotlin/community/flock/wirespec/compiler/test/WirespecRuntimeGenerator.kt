@file:JvmName("WirespecRuntimeGenerator")

package community.flock.wirespec.compiler.test

import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.instantiate
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import java.io.File

private data class Target(
    val outputRoot: File,
    val fqcn: String,
    val packagePath: String,
    val fileName: String,
)

public fun main(args: Array<String>) {
    require(args.size == 3) {
        "Usage: WirespecRuntimeGenerator <javaRoot> <kotlinRoot> <scalaRoot>"
    }
    val (javaRoot, kotlinRoot, scalaRoot) = args.map { File(it).apply { mkdirs() } }
    listOf(
        Target(
            outputRoot = javaRoot,
            fqcn = "community.flock.wirespec.emitters.java.JavaEmitter",
            packagePath = "community/flock/wirespec/java",
            fileName = "Wirespec.java",
        ),
        Target(
            outputRoot = kotlinRoot,
            fqcn = "community.flock.wirespec.emitters.kotlin.KotlinEmitter",
            packagePath = "community/flock/wirespec/kotlin",
            fileName = "Wirespec.kt",
        ),
        Target(
            outputRoot = scalaRoot,
            fqcn = "community.flock.wirespec.emitters.scala.ScalaEmitter",
            packagePath = "community/flock/wirespec/scala",
            fileName = "Wirespec.scala",
        ),
    ).forEach { target ->
        val source = emitterFor(target.fqcn).sharedSource()
        val outFile = target.outputRoot.resolve(target.packagePath).resolve(target.fileName)
        outFile.parentFile.mkdirs()
        outFile.writeText(source)
        println("[wirespec-runtime] wrote ${outFile.absolutePath}")
    }
}

private fun emitterFor(fqcn: String): IrEmitter = Class.forName(fqcn).instantiate(PackageName(DEFAULT_GENERATED_PACKAGE_STRING), EmitShared(true))

private fun IrEmitter.sharedSource(): String = emitShared()
    ?.let(generator::generate)
    ?: error("emitShared() returned null for ${this::class.qualifiedName}")
