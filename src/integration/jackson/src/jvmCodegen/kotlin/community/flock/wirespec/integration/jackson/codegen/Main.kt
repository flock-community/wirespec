package community.flock.wirespec.integration.jackson.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.jackson.extension.JacksonExtension

// The models of `union.ws` are read by the annotations of the JacksonExtension, the others by the Wirespec module.
fun main(args: Array<String>) = codegen(args) {
    val ast = wirespec("todos.ws", "literal.ws", "refined.ws")
    val annotated = wirespec("union.ws")
    emit("java", ast) { JavaEmitter(it) }
    emit("kotlin", ast) { KotlinEmitter(it) }
    emit("java", annotated) { JavaEmitter(it).applyExtensions(nonEmptyListOf(JacksonExtension(FileExtension.Java))) }
    emit("kotlin", annotated) { KotlinEmitter(it).applyExtensions(nonEmptyListOf(JacksonExtension(FileExtension.Kotlin))) }
}
