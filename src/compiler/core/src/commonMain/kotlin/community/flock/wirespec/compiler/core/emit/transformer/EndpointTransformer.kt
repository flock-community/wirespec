package community.flock.wirespec.compiler.core.emit.transformer

import community.flock.wirespec.compiler.core.parse.Endpoint

interface EndpointTransformer<T : Any> {
    fun Endpoint.transform(): T
}
