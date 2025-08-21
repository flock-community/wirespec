package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.ClientEmitter
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToLower
import community.flock.wirespec.compiler.core.emit.LanguageEmitter.Companion.firstToUpper
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.emit.paramList
import community.flock.wirespec.compiler.core.emit.root
import community.flock.wirespec.compiler.core.emit.spacer
import community.flock.wirespec.compiler.core.emit.allStatements
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference

interface KotlinClientEmitter: ClientEmitter, HasPackageName, KotlinTypeDefinitionEmitter {

    override fun emitClient(ast: AST): List<Emitted> {
        return emitClientInterfaces(ast) + listOf(emitClientClass(ast))
    }
    override fun emitClientInterfaces(ast: AST): List<Emitted> = ast.allStatements
        .filterIsInstance<Endpoint>()
        .map { endpoint ->
            Emitted("${packageName.toDir()}/client/${emit(endpoint.identifier)}Client.${extension.value}", """
             |package ${packageName.value}.client
             |
             |import ${packageName.value}.endpoint.${emit(endpoint.identifier)}
             |
             |${endpoint.requests.first().paramList(endpoint).map { it.reference.root() }.filterIsInstance<Reference.Custom>().distinctBy { it.value }.joinToString("\n") { "import ${packageName.value}.model.${it.value}" }}
             |
             |interface ${emit(endpoint.identifier)}Client {
             |  suspend fun ${emit(endpoint.identifier).firstToLower()}(${endpoint.requests.first().paramList(endpoint).joinToString(", ") { "${emit(it.identifier)}: ${it.reference.emit()}"}}): ${emit(endpoint.identifier)}.Response<*>
             |}
            """.trimMargin())
        }

    override fun emitClientClass(ast: AST): Emitted = Emitted("${packageName.toDir()}/Client.${extension.value}", """
        |package ${packageName.value}
        |
        |import community.flock.wirespec.kotlin.Wirespec
        |
        |${ast.allStatements.filterIsInstance<Endpoint>().joinToString("\n") { "import ${packageName.value}.client.${emit(it.identifier)}Client" }}
        |
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint) -> "import ${packageName.value}.endpoint.${emit(endpoint.identifier)}" }}
        |
        |${ast.modules.flatMap { it.statements }.toList().flatMap { it.importReferences() }.distinctBy { it.value }.joinToString("\n") { "import ${packageName.value}.model.${it.value}" }}
        |
        |${ast.emitModuleInterfaces().joinToString("\n")}
        |
        |interface All: 
        |${ast.modules.map { it.emitInterfaceName() }.joinToString(",\n").spacer(1)}
        |
        |open class Client(val serialization: Wirespec.Serialization<String>, val handler: (Wirespec.RawRequest) -> Wirespec.RawResponse ): All {
        |${ast.emitClientEndpointRequest().joinToString("\n") { (endpoint, request) -> emitFunction(endpoint, request) }.spacer(1)}
        |}
        |
    """.trimMargin())

    fun Endpoint.Request.emitClientInterface(endpoint: Endpoint) =
        this.paramList(endpoint).joinToString(", ") { "${emit(it.identifier)}: ${it.reference.emit()}" }

    fun emitFunction(endpoint: Endpoint, request: Endpoint.Request) = """
        |override suspend fun ${emit(endpoint.identifier).firstToLower()}(${request.emitClientInterface(endpoint)}) = 
        |   ${emit(endpoint.identifier)}.Request${request.paramList(endpoint).takeIf { it.isNotEmpty() }?.joinToString(", ", "(", ")") { emit(it.identifier) }.orEmpty()}
        |     .let { req -> ${endpoint.identifier.value}.toRequest(serialization, req) }
        |     .let { rawReq -> handler(rawReq) }
        |     .let { rawRes -> ${endpoint.identifier.value}.fromResponse(serialization, rawRes) }
    """.trimMargin()

    fun Module.emitInterfaceName() = fileUri
        .value
        .split(".").first()
        .split("/").last()
        .firstToUpper()
        .let { "${it}Module" }

    fun AST.emitModuleInterfaces() = modules.map {  """
        |interface ${it.emitInterfaceName()}: 
        |${it.statements.filterIsInstance<Endpoint>().joinToString(",\n") { "${emit(it.identifier)}Client" }.spacer(1)}
    """.trimMargin()}

}
