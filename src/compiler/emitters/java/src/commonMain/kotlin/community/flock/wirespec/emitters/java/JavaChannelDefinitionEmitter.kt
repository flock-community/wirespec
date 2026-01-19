package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.emit.importReferences
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Reference

interface JavaChannelDefinitionEmitter: ChannelDefinitionEmitter, HasPackageName, JavaTypeDefinitionEmitter  {

    override fun emit(channel: Channel) = """
        |${channel.emitImports()}
        |
        |@FunctionalInterface
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
