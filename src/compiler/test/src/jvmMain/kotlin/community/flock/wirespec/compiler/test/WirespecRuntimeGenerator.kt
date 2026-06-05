@file:JvmName("WirespecRuntimeGenerator")

package community.flock.wirespec.compiler.test

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.ir.emit.IrEmitter
import java.io.File
import kotlin.reflect.full.primaryConstructor

private data class Target(
    val outputRoot: File,
    val fqcn: String,
    val packagePath: String,
    val fileName: String,
)

fun main(args: Array<String>) {
    require(args.size == 3) {
        "Usage: WirespecRuntimeGenerator <javaRoot> <kotlinRoot> <scalaRoot>"
    }
    val (javaRoot, kotlinRoot, scalaRoot) = args.map { File(it).apply { mkdirs() } }
    listOf(
        Target(
            outputRoot = javaRoot,
            fqcn = "community.flock.wirespec.emitters.java.JavaIrEmitter",
            packagePath = "community/flock/wirespec/java",
            fileName = "Wirespec.java",
        ),
        Target(
            outputRoot = kotlinRoot,
            fqcn = "community.flock.wirespec.emitters.kotlin.KotlinIrEmitter",
            packagePath = "community/flock/wirespec/kotlin",
            fileName = "Wirespec.kt",
        ),
        Target(
            outputRoot = scalaRoot,
            fqcn = "community.flock.wirespec.emitters.scala.ScalaIrEmitter",
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

private fun emitterFor(fqcn: String): IrEmitter {
    val ctor = Class.forName(fqcn).kotlin.primaryConstructor
        ?: error("No primary constructor on $fqcn")
    val emitSharedParam = ctor.parameters.firstOrNull { it.type.classifier == EmitShared::class }
    val args = emitSharedParam?.let { mapOf(it to EmitShared(value = true)) }.orEmpty()
    val instance = ctor.callBy(args)
    require(instance is IrEmitter) { "$fqcn is not an IrEmitter" }
    return instance
}

private fun IrEmitter.sharedSource(): String = emitShared()
    ?.let(generator::generate)
    ?: error("emitShared() returned null for ${this::class.qualifiedName}")
