package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.emit.toField
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.utils.Logger

interface Emitters : TypeDefinitionEmitter, EnumDefinitionEmitter, RefinedTypeDefinitionEmitter, EndpointDefinitionEmitter

abstract class Emitter(open val logger: Logger, open val split:Boolean) {

    abstract fun emit(ast: AST): List<Pair<String, String>>

    companion object {
        const val SPACER = "  "
    }
}

abstract class AbstractEmitter(override val logger: Logger, override val split: Boolean = false) : Emitter(logger, split), Emitters {

    abstract val shared:String

    override fun emit(ast: AST): List<Pair<String, String>> = ast
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
        const val SPACER = Emitter.SPACER
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase )
        fun String.firstToLower() = replaceFirstChar(Char::lowercase )
        fun AST.hasEndpoints() = any { it is Endpoint }
        fun String.isInt() = toIntOrNull() != null
    }
}
