package community.flock.wirespec.integration.jvm.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.integration.jvm.extension.JvmBuilderExtension

fun main(args: Array<String>) = codegen(args) {
    val ast = wirespec()
    emit("java", ast) {
        JavaEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(JvmBuilderExtension(FileExtension.Java)))
    }
    emit("kotlin", ast) {
        KotlinEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(JvmBuilderExtension(FileExtension.Kotlin)))
    }
    emit("scala", ast) {
        ScalaEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(JvmBuilderExtension(FileExtension.Scala)))
    }
}
