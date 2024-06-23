package community.flock.wirespec.integration.spring.emit

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.transformer.EndpointClass
import community.flock.wirespec.compiler.utils.Logger

class SpringJavaEmitter(logger: Logger, split:Boolean): JavaEmitter() {
    override fun callAnnotation(endpointClass: EndpointClass): String {
        return when(endpointClass.method.uppercase()){
            "GET" -> "@GetMapping(\"${endpointClass.path}\")\n"
            "POST" -> "@PostMapping(\"${endpointClass.path}\")\n"
            "PUT" -> "@PutMapping(\"${endpointClass.path}\")\n"
            "DELETE" -> "@DeleteMapping(\"${endpointClass.path}\")\n"
            else -> error("Method not implemented: ${endpointClass.method}")
        }
    }
}