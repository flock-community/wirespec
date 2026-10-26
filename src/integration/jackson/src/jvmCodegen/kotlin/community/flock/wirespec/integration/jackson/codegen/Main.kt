package community.flock.wirespec.integration.jackson.codegen

import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter

fun main(args: Array<String>) = codegen(args) {
    val ast = wirespec()
    emit("java", ast) { JavaEmitter(it) }
    emit("kotlin", ast) { KotlinEmitter(it) }
}
