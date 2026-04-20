package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.emit.AccessorStyle
import community.flock.wirespec.ir.extensions.ClientIrExtension
import community.flock.wirespec.ir.extensions.buildClientServerInterfaces as neutralBuildClientServerInterfaces

class TypeScriptClientIrExtension(
    private val hasEndpointParams: (Endpoint) -> Boolean,
) : ClientIrExtension {

    override fun emitEndpointClient(endpoint: Endpoint): File {
        val endpointName = endpoint.identifier.value
        val methodName = endpointName.firstToLowerLocal()

        val imports = endpoint.importReferences().distinctBy { it.value }
            .joinToString("\n") { "import {type ${it.value}} from '../model'" }

        val hasParams = hasEndpointParams(endpoint)
        val paramList = if (hasParams) "params: $endpointName.RequestParams" else ""

        val requestArgs = if (hasParams) "$endpointName.request(params)" else "$endpointName.request()"

        val code = buildString {
            appendLine("export const ${methodName}Client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({")
            appendLine("  $methodName: async ($paramList): Promise<$endpointName.Response<unknown>> => {")
            appendLine("    const request: $endpointName.Request = $requestArgs;")
            appendLine("    const rawRequest = $endpointName.toRawRequest(serialization, request);")
            appendLine("    const rawResponse = await transportation.transport(rawRequest);")
            appendLine("    return $endpointName.fromRawResponse(serialization, rawResponse);")
            appendLine("  }")
            append("})")
        }

        return File(
            Name.of("client/${endpointName}Client"),
            buildList {
                add(RawElement("import {Wirespec} from '../Wirespec'"))
                add(RawElement("import {$endpointName} from '../endpoint/$endpointName'"))
                if (imports.isNotEmpty()) add(RawElement(imports))
                add(RawElement(code))
            },
        )
    }

    override fun emitClient(endpoints: List<Endpoint>, logger: Logger): File {
        logger.info("Emitting main Client for ${endpoints.size} endpoints")

        val clientImports = endpoints.joinToString("\n") {
            val methodName = it.identifier.value.firstToLowerLocal()
            "import {${methodName}Client} from './client/${it.identifier.value}Client'"
        }

        val spreadEntries = endpoints.joinToString("\n") {
            val methodName = it.identifier.value.firstToLowerLocal()
            "  ...${methodName}Client(serialization, transportation),"
        }

        val code = buildString {
            appendLine("export const client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({")
            appendLine(spreadEntries)
            append("})")
        }

        return File(
            Name.of("Client"),
            listOf(
                RawElement("import {Wirespec} from './Wirespec'"),
                RawElement(clientImports),
                RawElement(code),
            ),
        )
    }

    override fun buildClientServerInterfaces(style: AccessorStyle): List<Element> = neutralBuildClientServerInterfaces(style)

    private fun String.firstToLowerLocal() = replaceFirstChar { it.lowercase() }
}
