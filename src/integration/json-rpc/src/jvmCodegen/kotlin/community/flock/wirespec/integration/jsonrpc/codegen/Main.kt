package community.flock.wirespec.integration.jsonrpc.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.extension.SyncExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.jsonrpc.extension.JsonRpcExtension

// The generated code compiles against the Wirespec of the runtime library, so the shared file is not emitted.
fun main(args: Array<String>) = codegen(args) {
    val users = wirespec("users.ws")
    emit("java", users) { JavaEmitter(it).applyExtensions(nonEmptyListOf(JsonRpcExtension(it, FileExtension.Java))) }
    emit("kotlin", users) { KotlinEmitter(it).applyExtensions(nonEmptyListOf(JsonRpcExtension(it, FileExtension.Kotlin))) }
    // With --sync the services are plain functions, while the transportation stays suspend.
    emit("sync", users) {
        KotlinEmitter(it).applyExtensions(nonEmptyListOf(SyncExtension(FileExtension.Kotlin), JsonRpcExtension(it, FileExtension.Kotlin)))
    }
    // The methods of the examples of the JSON-RPC 2.0 specification, which the servers answer as the specification does.
    val specification = wirespec("specification.ws")
    emit("javaspec", specification) { JavaEmitter(it).applyExtensions(nonEmptyListOf(JsonRpcExtension(it, FileExtension.Java))) }
    emit("kotlinspec", specification) { KotlinEmitter(it).applyExtensions(nonEmptyListOf(JsonRpcExtension(it, FileExtension.Kotlin))) }
}
