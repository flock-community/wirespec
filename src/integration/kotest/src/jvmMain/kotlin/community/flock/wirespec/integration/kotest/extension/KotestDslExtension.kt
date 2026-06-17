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
 * / `<Channel>Dsl.kt` per operation plus one `<Controller>Catalog.kt` per source
 * `.ws` module. Register on a Kotlin
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

        // One catalog object per source `.ws` module (controller), named after the
        // file. Modules with neither endpoints nor channels (e.g. types-only) emit
        // nothing.
        val catalog = modules.mapNotNull { module ->
            val moduleEndpoints = module.statements.toList().filterIsInstance<Endpoint>().map { it.identifier.value }
            val moduleChannels = module.statements.toList().filterIsInstance<Channel>().map { it.identifier.value }
            if (moduleEndpoints.isEmpty() && moduleChannels.isEmpty()) {
                null
            } else {
                CatalogFile.build(
                    catalogName = catalogNameOf(module.fileUri.value),
                    endpointNames = moduleEndpoints,
                    channelNames = moduleChannels,
                    packageName = packageName,
                )
            }
        }

        val extra: List<Element> = endpointDsl + channelDsl + catalog
        return ir + extra
    }

    /**
     * Derive the catalog object name from a module's `.ws` file URI basename.
     * Hand-authored basenames may contain characters that are illegal in a
     * Kotlin identifier (e.g. `tool-calls.ws`); camel-case across the illegal
     * separators so the catalog still compiles (`tool-calls` -> `toolCalls`).
     */
    private fun catalogNameOf(fileUri: String): String = fileUri.substringAfterLast('/').substringAfterLast('\\').removeSuffix(".ws")
        .split(nonIdentifierChars)
        .filter { it.isNotEmpty() }
        .mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercaseChar) }
        .joinToString("")
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }

    private val nonIdentifierChars = Regex("[^A-Za-z0-9_]+")
}
