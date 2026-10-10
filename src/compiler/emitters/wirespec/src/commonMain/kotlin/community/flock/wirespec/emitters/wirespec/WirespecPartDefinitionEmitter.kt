package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.PartDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Part

internal interface WirespecPartDefinitionEmitter : PartDefinitionEmitter, WirespecTypeDefinitionEmitter {

    override fun emit(part: Part): String = """
        |part ${emit(part.identifier)} {
        |${part.shape.emit()}
        |}
        |""".trimMargin()
}
