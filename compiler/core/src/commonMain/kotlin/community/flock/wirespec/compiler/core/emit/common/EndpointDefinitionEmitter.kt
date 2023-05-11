package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Endpoint

interface EndpointDefinitionEmitter : TypeDefinitionEmitterLogger {

    fun Endpoint.emit(): String
    fun Endpoint.Response.emit(className: String): String
}