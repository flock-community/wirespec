package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.BaseEmitter
import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.ImportEmitter
import community.flock.wirespec.compiler.core.emit.PackageNameEmitter
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Reference

interface JavaChannelDefinitionEmitter: JavaTypeDefinitionEmitter, PackageNameEmitter, ChannelDefinitionEmitter, ImportEmitter, IdentifierEmitter {

    override fun emit(channel: Channel) = """
        |${channel.emitImports()}
        |
        |public interface ${emit(channel.identifier)} {
        |   void invoke(${channel.emitFullyQualified(channel.reference)}${channel.reference.emitRoot()} message);
        |}
        |
    """.trimMargin()

    private fun Definition.emitImports() = importReferences()
        .filter { identifier.value != it.value }
        .map { "import ${packageName.value}.model.${it.value};" }.joinToString("\n") { it.trimStart() }

    private fun Definition.emitFullyQualified(reference: Reference) =
        if (identifier.value == reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }

}
