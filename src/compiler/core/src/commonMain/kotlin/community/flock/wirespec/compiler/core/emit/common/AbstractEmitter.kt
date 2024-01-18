package community.flock.wirespec.compiler.core.emit.common

import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Node
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.utils.Logger

interface Emitters : TypeDefinitionEmitter, EnumDefinitionEmitter, RefinedTypeDefinitionEmitter,
    EndpointDefinitionEmitter

abstract class Emitter(open val logger: Logger, open val split: Boolean) {

    abstract val shared: String?
    abstract fun emit(ast: AST): List<Emitted>

    companion object {
        const val SPACER = "  "
    }
}

abstract class AbstractEmitter(override val logger: Logger, override val split: Boolean = false) :
    Emitter(logger, split), Emitters {

    override fun emit(ast: AST): List<Emitted> = ast
        .map {
            logger.log("Emitting Node $this")
            when (it) {
                is Type -> Emitted(it.emitName(), it.emit())
                is Endpoint -> Emitted(it.emitName(), it.emit())
                is Enum -> Emitted(it.emitName(), it.emit())
                is Refined -> Emitted(it.emitName(), it.emit())
            }
        }
        .run {
            if (split) this
            else listOf(Emitted("NoName", joinToString("\n") { it.result }))
        }

    abstract fun Node.emitName():String

    fun Endpoint.Content.emitContentType() = type
        .substringBefore(";")
        .split("/", "-")
        .joinToString("") { it.firstToUpper() }
        .replace("+", "")


    companion object {
        const val SPACER = Emitter.SPACER
        fun String.firstToUpper() = replaceFirstChar(Char::uppercase)
        fun String.firstToLower() = replaceFirstChar(Char::lowercase)
        fun AST.hasEndpoints() = any { it is Endpoint }
        fun String.isInt() = toIntOrNull() != null
    }
}
