package community.flock.wirespec.integration.kotlinxserialization.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.kotlinxserialization.extension.KotlinxSerializationExtension

fun main(args: Array<String>) = codegen(args) {
    emit("kotlin", wirespec()) { KotlinEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(KotlinxSerializationExtension())) }
}
