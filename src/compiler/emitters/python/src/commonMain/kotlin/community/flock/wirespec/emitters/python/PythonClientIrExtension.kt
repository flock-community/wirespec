package community.flock.wirespec.emitters.python

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Import
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.findElement
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.emit.SanitizationConfig
import community.flock.wirespec.ir.emit.sanitizeNames
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.convertClient
import community.flock.wirespec.ir.extensions.convertEndpointClient
import community.flock.wirespec.ir.core.Function as LanguageFunction
import community.flock.wirespec.ir.core.Type as LanguageType
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class PythonClientIrExtension(
    private val packageName: PackageName,
    private val sanitizationConfig: SanitizationConfig,
    private val buildImports: (path: String) -> List<Element>,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val modelImports = endpoint.importReferences().distinctBy { it.value }
            .map { Import("..model.${it.value}", LanguageType.Custom(it.value)) }
        val endpointImport = Import("..endpoint.${endpoint.identifier.value}", LanguageType.Custom("*"))
        val endpointName = endpoint.identifier.value

        val file = endpoint.convertEndpointClient()
            .sanitizeNames(sanitizationConfig)
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .flattenEndpointTypeRefs(endpointName)

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + file.name.pascalCase()),
            elements = buildImports("..wirespec") +
                modelImports +
                listOf(endpointImport) +
                file.elements,
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")
        val modelImports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .map { Import(".model.${it.value}", LanguageType.Custom(it.value)) }
        val endpointImports = endpoints.map { Import(".endpoint.${it.identifier.value}", LanguageType.Custom("*")) }
        val clientImports = endpoints.map {
            Import(".client.${it.identifier.value}Client", LanguageType.Custom("${it.identifier.value}Client"))
        }
        val allImports = modelImports + endpointImports + clientImports
        val endpointNames = endpoints.map { it.identifier.value }

        val file = endpoints.convertClient()
            .sanitizeNames(sanitizationConfig)
            .addSelfReceiverToClientFields()
            .snakeCaseClientFunctions()
            .let { f -> endpointNames.fold(f) { acc, name -> acc.flattenEndpointTypeRefs(name) } }

        return File(
            name = Name.of(packageName.toDir() + file.name.pascalCase()),
            elements = buildImports(".wirespec") +
                allImports +
                file.elements,
        )
    }

    override fun buildClientServerInterfaces(style: AccessorStyle): List<Element> = neutralBuildClientServerInterfaces(style)

    private fun <T : Element> T.flattenEndpointTypeRefs(endpointName: String): T = transform {
        type { type, _ ->
            if (type is LanguageType.Custom && type.name.dotted().startsWith("$endpointName.")) {
                val suffix = type.name.dotted().removePrefix("$endpointName.")
                if (suffix == "Call" || suffix == "Handler") {
                    type
                } else {
                    type.copy(name = Name.fromDotted(suffix))
                }
            } else {
                type
            }
        }
    }

    private fun <T : Element> T.addSelfReceiverToClientFields(): T {
        val struct = (this as? File)?.findElement<Struct>()
        val fieldNames = struct?.fields?.map { it.name.value() }?.toSet() ?: emptySet()
        if (fieldNames.isEmpty()) return this

        return transform {
            statementAndExpression { stmt, tr ->
                when (stmt) {
                    is FieldCall -> {
                        if (stmt.receiver == null && stmt.field.value() in fieldNames) {
                            FieldCall(receiver = VariableReference(Name.of("self")), field = stmt.field)
                        } else {
                            FieldCall(
                                receiver = stmt.receiver?.let { tr.transformExpression(it) },
                                field = stmt.field,
                            )
                        }
                    }
                    else -> stmt.transformChildren(tr)
                }
            }
        }
    }

    private fun <T : Element> T.snakeCaseClientFunctions(): T = transform {
        matchingElements { func: LanguageFunction ->
            func.copy(
                name = Name.of(func.name.snakeCase()),
                parameters = listOf(Parameter(Name.of("self"), LanguageType.Custom(""))) + func.parameters,
            )
        }
        statementAndExpression { stmt, tr ->
            when (stmt) {
                is FunctionCall -> {
                    val nameStr = stmt.name.value()
                    val newName = if ("." in nameStr) stmt.name else Name.of(Name.of(nameStr).snakeCase())
                    FunctionCall(
                        name = newName,
                        receiver = stmt.receiver?.let { tr.transformExpression(it) },
                        arguments = stmt.arguments.mapValues { (_, v) -> tr.transformExpression(v) },
                        isAwait = stmt.receiver != null,
                    )
                }
                else -> stmt.transformChildren(tr)
            }
        }
    }
}
