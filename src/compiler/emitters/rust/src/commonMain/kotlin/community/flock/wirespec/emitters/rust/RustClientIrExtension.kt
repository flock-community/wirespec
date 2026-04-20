package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class RustClientIrExtension(
    private val packageName: PackageName,
    private val buildClientParams: (Endpoint) -> Pair<String, String>,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val endpointName = endpoint.identifier.value
        val endpointModuleName = endpointName.toSnake()
        val clientName = "${endpointName}Client"
        val methodName = endpointName.toSnake()
        val (paramsStr, requestArgs) = buildClientParams(endpoint)
        val requestConstruction = "$endpointModuleName::Request::new($requestArgs)"

        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "use super::super::model::${it.value.toSnake()}::${it.value};" }
        val namespacePath = "$endpointModuleName::$endpointName"
        val code = buildList {
            add("use super::super::wirespec::*;")
            add("use super::super::endpoint::$endpointModuleName;")
            if (imports.isNotEmpty()) add(imports)
            add("pub struct $clientName<'a, S: Serialization, T: Transportation> {")
            add("    pub serialization: &'a S,")
            add("    pub transportation: &'a T,")
            add("}")
            add("impl<'a, S: Serialization, T: Transportation> $namespacePath::Call for $clientName<'a, S, T> {")
            add("    async fn $methodName(&self$paramsStr) -> $endpointModuleName::Response {")
            add("        let request = $requestConstruction;")
            add("        let raw_request = $namespacePath::to_raw_request(self.serialization, request);")
            add("        let raw_response = self.transportation.transport(&raw_request).await;")
            add("        $namespacePath::from_raw_response(self.serialization, raw_response)")
            add("    }")
            add("}")
        }.joinToString("\n")

        val subPackageName = packageName + "client"
        return File(
            name = Name.of(subPackageName.toDir() + clientName.toSnake()),
            elements = listOf(RawElement(code)),
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")

        val modDeclarations = endpoints.joinToString("\n") { endpoint ->
            "pub mod ${(endpoint.identifier.value + "Client").toSnake()};"
        }

        val modelImports = endpoints.flatMap { it.importReferences() }.distinctBy { it.value }
            .filter { imp -> endpoints.none { it.identifier.value == imp.value } }
            .map { "use super::model::${it.value.toSnake()}::${it.value};" }

        val useStatements = endpoints.flatMap { endpoint ->
            val endpointModuleName = endpoint.identifier.value.toSnake()
            val clientModuleName = "${endpoint.identifier.value}Client".toSnake()
            listOf(
                "use super::endpoint::$endpointModuleName;",
                "use $clientModuleName::${endpoint.identifier.value}Client;",
            )
        }

        val implBlocks = endpoints.flatMap { endpoint ->
            val endpointName = endpoint.identifier.value
            val endpointModuleName = endpointName.toSnake()
            val namespacePath = "$endpointModuleName::$endpointName"
            val methodName = endpointName.toSnake()
            val (paramsStr, callArgs) = buildClientParams(endpoint)
            val delegateCall = if (callArgs.isNotEmpty()) {
                "${endpointName}Client { serialization: &self.serialization, transportation: &self.transportation }\n            .$methodName($callArgs).await"
            } else {
                "${endpointName}Client { serialization: &self.serialization, transportation: &self.transportation }\n            .$methodName().await"
            }

            listOf(
                "impl<S: Serialization, T: Transportation> $namespacePath::Call for Client<S, T> {",
                "    async fn $methodName(&self$paramsStr) -> $endpointModuleName::Response {",
                "        $delegateCall",
                "    }",
                "}",
            )
        }

        val code = (
            listOf(modDeclarations) +
                listOf("use super::wirespec::*;") +
                modelImports +
                useStatements +
                listOf(
                    "pub struct Client<S: Serialization, T: Transportation> {",
                    "    pub serialization: S,",
                    "    pub transportation: T,",
                    "}",
                ) +
                implBlocks
            ).joinToString("\n")

        return File(
            name = Name.of(packageName.toDir() + "client"),
            elements = listOf(RawElement(code)),
        )
    }

    override fun buildClientServerInterfaces(style: AccessorStyle): List<Element> = neutralBuildClientServerInterfaces(style)

    private fun String.toSnake(): String = Name.of(this).snakeCase()
}
