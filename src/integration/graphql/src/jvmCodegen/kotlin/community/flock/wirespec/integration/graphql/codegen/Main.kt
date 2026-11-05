package community.flock.wirespec.integration.graphql.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.integration.graphql.extension.GraphQLClientExtension

fun main(args: Array<String>) = codegen(args) {
    val ast = wirespec()
    emit("java", ast) {
        JavaEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(GraphQLClientExtension(it, FileExtension.Java)))
    }
    emit("kotlin", ast) {
        KotlinEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(GraphQLClientExtension(it, FileExtension.Kotlin)))
    }
    emit("scala", wirespec("todo.ws")) {
        ScalaEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(GraphQLClientExtension(it, FileExtension.Scala)))
    }
}
