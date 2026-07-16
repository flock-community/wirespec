package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.IR
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.fieldList
import community.flock.wirespec.ir.core.raw
import community.flock.wirespec.ir.extension.IrExtension

/**
 * Adds a typesafe Kotest scenario DSL to the generated output: one `<Endpoint>Dsl.kt`
 * / `<Channel>Dsl.kt` per operation. Each file carries a `generate` extension property
 * on the generated endpoint/channel object grouping the DSL entry points, with sending
 * always chained off the materialised value:
 * - endpoint: `PutTodo.generate.request { … }.call()` (plus
 *   `PutTodo.generate.response200 { … }` for canned responses);
 * - channel: `Queue.generate.message { … }.send()` to publish (asserting on what the app
 *   published is left to the test's own broker consumer);
 * - type: `TodoDto.generate { … }` returns a `Gen<TodoDto>` for a standalone record.
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

        // One `<Type>Dsl.kt` per record carries the single reusable `<Type>Builder`, referenced by
        // endpoint bodies and channel payloads rather than replicated per operation.
        val typeDsl = types.values.map { TypeDslFile.build(it, packageName, types, refined) }
        val endpointDsl = endpoints.map { EndpointDslFile.build(it, packageName, types, refined) }
        val channelDsl = channels.map { ChannelDslFile.build(it, packageName, types, refined) }

        val extra: List<Element> = typeDsl + endpointDsl + channelDsl

        // `<Type>.generate` is a companion extension, but records carry no companion — inject an empty
        // one into each model record (keeping the model dependency-free). Field-less records emit as
        // `object`s with no companion and take an object-level `generate`, so they need no injection.
        val recordNames = types.values.map { Name.of(it.identifier.value).pascalCase() }.toSet()
        val modelPkg = "${packageName.value}.model"
        val withCompanions = ir.map { it.injectRecordCompanion(recordNames, modelPkg) }

        return withCompanions + extra
    }

    private fun Element.injectRecordCompanion(recordNames: Set<String>, modelPkg: String): Element = if (this is File && elements.any { it is Package && it.path == modelPkg }) {
        copy(
            elements = elements.map { child ->
                if (child is Struct && child.name.pascalCase() in recordNames && child.fieldList().isNotEmpty() && !child.hasCompanion()) {
                    child.copy(elements = child.elements + raw("companion object"))
                } else {
                    child
                }
            },
        )
    } else {
        this
    }

    private fun Struct.hasCompanion(): Boolean = elements.any { it is RawElement && it.code.trimStart().startsWith("companion object") }
}
