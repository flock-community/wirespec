package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.emit.SanitizationConfig
import community.flock.wirespec.ir.emit.sanitizeNames
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.convertClient
import community.flock.wirespec.ir.extensions.convertEndpointClient
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class JavaClientIrExtension(
    private val packageName: PackageName,
    private val sanitizationConfig: SanitizationConfig,
    private val wirespecImport: Import,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val file = endpoint.convertEndpointClient().sanitizeNames(sanitizationConfig)

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(subPackageName.value)) +
                listOf(wirespecImport) +
                imports +
                listOf(endpointImport) +
                file.elements,
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .filter { imp -> endpoints.none { it.identifier.value == imp.value } }
            .map { import("${packageName.value}.model", it.value) }
        val endpointImports = endpoints.map { import("${packageName.value}.endpoint", it.identifier.value) }
        val clientImports = endpoints.map { import("${packageName.value}.client", "${it.identifier.value}Client") }
        val allImports = imports + endpointImports + clientImports
        val file = endpoints.convertClient().sanitizeNames(sanitizationConfig)
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(packageName.value)) +
                listOf(wirespecImport) +
                allImports +
                file.elements,
        )
    }

    override fun buildClientServerInterfaces(style: AccessorStyle): List<Element> = neutralBuildClientServerInterfaces(style)

    private fun Endpoint.buildImports(): List<Import> = importReferences()
        .filter { identifier.value != it.value }
        .map { import("${packageName.value}.model", it.value) }

    private fun String.sanitizeSymbol(): String = this
        .split(".", " ", "-")
        .mapIndexed { index, s -> if (index > 0) s.replaceFirstChar { c -> c.uppercaseChar() } else s }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '_' }
        .let { if (it.firstOrNull()?.isDigit() == true) "_$it" else it }
}
