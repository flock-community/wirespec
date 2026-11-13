package community.flock.wirespec.emitters.php

import community.flock.wirespec.compiler.core.emit.DEFAULT_GENERATED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.namespace
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.Import
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Package
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.converter.convertToGenerator
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.generator.Generator
import community.flock.wirespec.compiler.core.ir.generator.PhpGenerator
import community.flock.wirespec.compiler.core.ir.transformer.sanitizeEnumEntries
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.core.parse.ast.Type as AstType

/**
 * Emits PHP 8.2, one file per definition. The package name becomes the root namespace
 * (`community.flock.wirespec.generated` is `Community\Flock\Wirespec\Generated`), with
 * `Model`, `Endpoint`, `Channel`, `Rpc`, `Generator` and `Client` below it, and the shared
 * runtime in `Wirespec`. An endpoint file declares several classes, so load the generated
 * code with a classmap autoloader.
 */
public open class PhpEmitter(
    packageName: PackageName = PackageName(DEFAULT_GENERATED_PACKAGE_STRING),
    private val emitShared: EmitShared = EmitShared(),
) : IrEmitter {

    override val generator: Generator = PhpGenerator

    override val extension: FileExtension = FileExtension.Php

    private val rootNamespace: String = packageName.value.split('.')
        .filter { it.isNotEmpty() }
        .joinToString("\\") { segment -> segment.replaceFirstChar { it.uppercaseChar() } }

    private val wirespecImport: Import = phpImport(rootNamespace, "Wirespec")

    override fun emitShared(): File? = PackageName("shared").convert()
        .takeIf { emitShared.value }
        ?.let { shared -> File(location("", "Wirespec"), listOf(Package(rootNamespace)) + shared.elements.filterNot { it is Package }) }

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).placeIn(definition.namespace().subNamespace(), definition.modelImports())

    override fun emit(type: AstType, module: Module): File = type.convert()

    override fun emit(enum: Enum, module: Module): File = enum.convert()
        .sanitizeEnumEntries(sanitizeEntry = { it.sanitizeEnumEntry() })

    override fun emit(union: Union): File = union.convert()

    override fun emit(refined: Refined): File = refined.convert().withPhpRefinedFunctions(refined)

    override fun emit(endpoint: Endpoint): File = endpoint.convert()

    override fun emit(channel: Channel): File = channel.convert()

    override fun emit(rpc: Rpc): File = rpc.convert()

    override fun emitGenerator(definition: Definition, module: Module): File? = when (definition) {
        is AstType -> definition.convertToGenerator(module)
        is Enum -> definition.convertToGenerator().withEnumFrom(definition)
        is Refined -> definition.convertToGenerator()
        is Union -> definition.convertToGenerator()
        else -> null
    }?.placeIn("Generator", (listOf(definition.identifier.value) + definition.importReferences().map { it.value }).toModelImports())

    override fun emitEndpointClient(endpoint: Endpoint): File =
        super.emitEndpointClient(endpoint).placeIn("Client", endpoint.endpointImports() + endpoint.modelImports())

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File = super.emitClient(endpoints, logger).let { client ->
        File(
            name = location("", client.name.pascalCase()),
            elements = listOf(Package(rootNamespace), wirespecImport) +
                endpoints.flatMap { it.endpointImports() } +
                endpoints.map { phpImport(namespaceOf("Client"), "${it.identifier.className()}Client") } +
                endpoints.flatMap { it.importReferences() }.map { it.value }.toModelImports() +
                client.elements,
        )
    }

    private fun File.placeIn(subNamespace: String, imports: List<Element>): File = File(
        name = location(subNamespace, name.pascalCase()),
        elements = listOf(Package(namespaceOf(subNamespace)), wirespecImport) + imports + elements,
    )

    private fun namespaceOf(subNamespace: String): String = listOf(rootNamespace, subNamespace).filter { it.isNotEmpty() }.joinToString("\\")

    private fun location(subNamespace: String, fileName: String): Name =
        Name(listOf("${namespaceOf(subNamespace)}\\$fileName".removePrefix("\\").replace('\\', '/')))

    private fun String.subNamespace(): String = replaceFirstChar { it.uppercaseChar() }

    private fun Definition.modelImports(): List<Element> = when (namespace()) {
        "model" -> emptyList()
        else -> importReferences().map { it.value }.toModelImports()
    }

    private fun Endpoint.endpointImports(): List<Element> = listOf(phpImport(namespaceOf("Endpoint"), identifier.className()))

    private fun List<String>.toModelImports(): List<Element> = map { Name.of(it).pascalCase() }
        .distinct()
        .map { phpImport(namespaceOf("Model"), it) }

    private fun Identifier.className(): String = Name.of(value).pascalCase()

    private fun phpImport(namespace: String, name: String): Import = Import(namespace, Type.Custom(Name(listOf(name))))

    public companion object : Keywords by PhpGenerator
}
