package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.PackageNameEmitter
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition

interface KotlinChannelDefinitionEmitter: PackageNameEmitter, ChannelDefinitionEmitter, ImportEmitter, TypeDefinitionEmitter, IdentifierEmitter {

    override fun emit(channel: Channel) = """
        |${channel.emitImports()}
        |
        |interface ${emit(channel.identifier)}Channel {
        |   operator fun invoke(message: ${channel.reference.emit()})
        |}
        |
    """.trimMargin()

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { "import ${packageName.value}.model.${it.value};" }.joinToString("\n") { it.trimStart() }

}
