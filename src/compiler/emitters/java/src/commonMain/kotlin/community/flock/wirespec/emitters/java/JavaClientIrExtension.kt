package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Assignment
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.import
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.emit.SanitizationConfig
import community.flock.wirespec.ir.emit.sanitizeNames
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.convertClient
import community.flock.wirespec.ir.extensions.convertEndpointClient
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class JavaClientIrExtension(
    private val packageName: PackageName,
    private val sanitizationConfig: SanitizationConfig,
    private val wirespecImport: Import,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = import("${packageName.value}.endpoint", endpoint.identifier.value)
        val file = endpoint.convertEndpointClient().sanitizeNames(sanitizationConfig).transformTypeDescriptors()
        val endpointName = endpoint.identifier.value

        val transformedFile = file.transform {
            matchingElements { func: LanguageFunction ->
                if (func.isAsync && func.body.size >= 2) {
                    val transportAssign = func.body[func.body.size - 2]
                    val returnStmt = func.body.last()
                    if (transportAssign is Assignment && returnStmt is ReturnStatement) {
                        val bodyPrefix = func.body.dropLast(2)
                        func.copy(
                            body = bodyPrefix + ReturnStatement(
                                FunctionCall(
                                    name = Name.of("thenApply"),
                                    receiver = transportAssign.value,
                                    arguments = mapOf(
                                        Name.of("mapper") to RawExpression(
                                            "rawResponse -> $endpointName.fromRawResponse(serialization(), rawResponse)",
                                        ),
                                    ),
                                ),
                            ),
                        )
                    } else {
                        func
                    }
                } else {
                    func
                }
            }
        }

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + transformedFile.name.pascalCase().sanitizeSymbol()),
            elements = listOf(Package(subPackageName.value)) +
                listOf(wirespecImport) +
                imports +
                listOf(endpointImport) +
                transformedFile.elements,
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
