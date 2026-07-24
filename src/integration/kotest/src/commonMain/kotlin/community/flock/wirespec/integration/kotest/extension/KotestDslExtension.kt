package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.integration.kotest.convert.ChannelDslFile
import community.flock.wirespec.integration.kotest.convert.EndpointDslFile
import community.flock.wirespec.integration.kotest.convert.TypeDslFile
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

/** Adds a typesafe Kotest scenario DSL to the generated output, one DSL file per endpoint/channel/type. */
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

        val typeDsl = types.values.map { TypeDslFile.build(it, packageName, types, refined) }
        val endpointDsl = endpoints.map { EndpointDslFile.build(it, packageName, types, refined) }
        val channelDsl = channels.map { ChannelDslFile.build(it, packageName, types, refined) }

        val extra: List<Element> = typeDsl + endpointDsl + channelDsl

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
