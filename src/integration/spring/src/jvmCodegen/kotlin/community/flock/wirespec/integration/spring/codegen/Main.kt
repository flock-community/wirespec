package community.flock.wirespec.integration.spring.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.integration.spring.extension.SpringNativeHintsExtension
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser

fun main(args: Array<String>) = codegen(args) {
    val petstore = OpenAPIV3Parser.parse(moduleContent(inputDir.resolve("petstore.json")), false)
    val todo = wirespec("todo.ws")
    emit("kotlin", petstore, todo) {
        KotlinEmitter(it, EmitShared(false)).applyExtensions(
            nonEmptyListOf(
                SpringMappingAnnotationsExtension(FileExtension.Kotlin),
                SpringNativeHintsExtension(it, FileExtension.Kotlin),
            ),
        )
    }
    emit("java", petstore, todo) {
        JavaEmitter(it, EmitShared(false)).applyExtensions(
            nonEmptyListOf(
                SpringMappingAnnotationsExtension(FileExtension.Java),
                SpringNativeHintsExtension(it, FileExtension.Java),
            ),
        )
    }
}
