package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.emit.toField
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger

interface Emitters : TypeDefinitionEmitter, EnumDefinitionEmitter, RefinedTypeDefinitionEmitter, EndpointDefinitionEmitter

abstract class Emitter(val logger: Logger, val split: Boolean = false) : Emitters {

    open fun emit(ast: AST) = ast
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is Type -> it.name to it.emit()
                is Endpoint -> it.name to it.emit()
                is Enum -> it.name to it.emit()
                is Refined -> it.name to it.emit()
            }
        }
        .run {
            if (split) this
            else listOf("NoName" to joinToString("\n") { it.second })
        }

    companion object {
        const val SPACER = "  "
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase )
        fun String.firstToLower() = replaceFirstChar(Char::lowercase )
        fun AST.hasEndpoints() = any { it is Endpoint }
        fun Endpoint.joinParameters(content: Endpoint.Content? = null): List<Type.Shape.Field> {
            val pathField = path
                .filterIsInstance<Endpoint.Segment.Param>()
                .map { Type.Shape.Field(it.identifier, it.reference, false) }
            val parameters = pathField + query + headers + cookies
            return parameters
                .plus(content?.reference?.toField("body", false))
                .filterNotNull()
        }
        fun String.isInt() = toIntOrNull() != null
    }

}
