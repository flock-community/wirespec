import community.flock.wirespec.compiler.core.emit.common.TypeDefinitionEmitterLogger
import community.flock.wirespec.compiler.core.parse.EndpointDefinition

interface EndpointDefinitionEmitter : TypeDefinitionEmitterLogger {

    fun EndpointDefinition.emit(): String
    fun EndpointDefinition.Response.emit(className: String): String
}