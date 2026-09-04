package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.RpcDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Rpc

internal interface WirespecRpcDefinitionEmitter : RpcDefinitionEmitter, WirespecTypeDefinitionEmitter {
    override fun emit(rpc: Rpc): String = when {
        rpc.shape.value.isEmpty() -> "rpc ${emit(rpc.identifier)} {} -> ${rpc.emitReturn()}\n"
        else -> """
            |rpc ${emit(rpc.identifier)} {
            |${rpc.shape.emit()}
            |} -> ${rpc.emitReturn()}
            |""".trimMargin()
    }

    private fun Rpc.emitReturn() = "${result.emit()}${error?.let { " ! ${it.emit()}" }.orEmpty()}"
}
