package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.extension.IrExtension

/**
 * Adds a typesafe Kotest scenario DSL to the generated output: one `<Endpoint>Dsl.kt`
 * / `<Channel>Dsl.kt` per operation. Each file carries a `generate` extension property
 * on the generated endpoint/channel object grouping the DSL entry points
 * (`PutTodo.generate.request { … }`, `PutTodo.generate.response200 { … }`); an endpoint
 * request is sent by chaining its `call()` extension
 * (`PutTodo.generate.request { … }.call()`), while a channel keeps its
 * `generate.call { … }` scope (there is no request object to chain from).
 * Register on a Kotlin
 * [community.flock.wirespec.ir.emit.IrEmitter] (e.g. `KotlinIrEmitter`); the DSL
 * files live in `<packageName>.kotest` and reference the models/endpoints the base
 * emitter produces in `<packageName>.model` / `.endpoint` / `.channel`.
 *
 * Ported from the standalone `TypesafeDslEmitter` (which subclassed `KotlinIrEmitter`)
 * into the IR extension model so it composes with any Kotlin IR emitter and is
 * registerable through the Wirespec Gradle/Maven plugins via `extensionClasses`.
 */
open class KotestDslExtension(
    private val packageName: PackageName,
) : IrExtension {

    override fun extend(ir: IR, ast: AST): IR {
        val modules = ast.modules.toList()
        val allStatements = modules.flatMap { it.statements.toList() }
        val types = allStatements.filterIsInstance<Type>().associateBy { it.identifier.value }
        val refined = allStatements.filterIsInstance<Refined>().associateBy { it.identifier.value }

        val endpoints = allStatements.filterIsInstance<Endpoint>()
        val channels = allStatements.filterIsInstance<Channel>()

        val endpointDsl = endpoints.map { EndpointDslFile.build(it, packageName, types, refined) }
        val channelDsl = channels.map { ChannelDslFile.build(it, packageName, types, refined) }

        val extra: List<Element> = endpointDsl + channelDsl
        return ir + extra
    }
}
