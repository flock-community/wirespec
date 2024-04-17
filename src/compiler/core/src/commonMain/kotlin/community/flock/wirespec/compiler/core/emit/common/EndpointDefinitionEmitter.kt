package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.Endpoint

interface EndpointDefinitionEmitter {
    fun Endpoint.emit(): String
}
