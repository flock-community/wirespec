package community.flock.wirespec.emitters.scala

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.emit.SanitizationConfig
import community.flock.wirespec.ir.emit.sanitizeNames
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.convertClient
import community.flock.wirespec.ir.extensions.convertEndpointClient
import community.flock.wirespec.ir.core.Package as LanguagePackage
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class ScalaClientIrExtension(
    private val packageName: PackageName,
    private val sanitizationConfig: SanitizationConfig,
    private val wirespecImport: String,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val imports = endpoint.buildImports()
        val endpointImport = "import ${packageName.value}.endpoint.${endpoint.identifier.value}"
        val allImports = listOf(imports, endpointImport).filter { it.isNotEmpty() }.joinToString("\n")
        val file = endpoint.convertEndpointClient().sanitizeNames(sanitizationConfig).addIdentityTypeToCall()
        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(subPackageName.value))
                add(RawElement(wirespecImport))
                if (allImports.isNotEmpty()) add(RawElement(allImports))
                addAll(file.elements)
            },
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")
        val imports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .joinToString("\n") { "import ${packageName.value}.model.${it.value}" }
        val endpointImports = endpoints
            .joinToString("\n") { "import ${packageName.value}.endpoint.${it.identifier.value}" }
        val clientImports = endpoints
            .joinToString("\n") { "import ${packageName.value}.client.${it.identifier.value}Client" }
        val allImports = listOf(imports, endpointImports, clientImports).filter { it.isNotEmpty() }.joinToString("\n")
        val file = endpoints.convertClient().sanitizeNames(sanitizationConfig).addIdentityTypeToCall()
        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = buildList {
                add(LanguagePackage(packageName.value))
                add(RawElement(wirespecImport))
                if (allImports.isNotEmpty()) add(RawElement(allImports))
                addAll(file.elements)
            },
        )
    }

    override fun buildClientServerInterfaces(style: AccessorStyle): List<Element> = neutralBuildClientServerInterfaces(style)

    private fun Endpoint.buildImports() = importReferences()
        .distinctBy { it.value }
        .joinToString("\n") { "import ${packageName.value}.model.${it.value}" }

    private fun <T : Element> T.addIdentityTypeToCall(): T = transform {
        matchingElements { struct: Struct ->
            struct.copy(
                interfaces = struct.interfaces.map { type ->
                    if (type is LanguageType.Custom && type.name.endsWith(".Call")) {
                        type.copy(generics = listOf(LanguageType.Custom("[A] =>> A")))
                    } else {
                        type
                    }
                },
            )
        }
    }
}
