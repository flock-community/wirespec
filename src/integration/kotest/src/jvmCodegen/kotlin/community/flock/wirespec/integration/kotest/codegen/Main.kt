package community.flock.wirespec.integration.kotest.codegen

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.test.codegen
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.kotest.extension.KotestDslExtension
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser

fun main(args: Array<String>) = codegen(args) {
    val productSeries = OpenAPIV3Parser.parse(moduleContent(inputDir.resolve("product-series.json")), false)
    emit("kotlin", wirespec(), productSeries) { KotlinEmitter(it, EmitShared(false)).applyExtensions(nonEmptyListOf(KotestDslExtension(it))) }
}
