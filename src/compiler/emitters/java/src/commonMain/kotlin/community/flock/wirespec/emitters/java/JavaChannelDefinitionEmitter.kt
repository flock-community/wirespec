package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Interface
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.transformParametersWhere
import community.flock.wirespec.language.generator.generateJava

interface JavaChannelDefinitionEmitter : ChannelDefinitionEmitter, HasPackageName, JavaTypeDefinitionEmitter {

    override fun emit(channel: Channel): String {
        val fullyQualifiedPrefix = channel.emitFullyQualified(channel.reference)
        return """
            |@FunctionalInterface
            |${channel.convert().withFullyQualifiedPrefix(fullyQualifiedPrefix).generateJava()}
            |
        """.trimMargin()
    }

    private fun Interface.withFullyQualifiedPrefix(prefix: String): Interface =
        if (prefix.isNotEmpty()) {
            transformParametersWhere(
                predicate = { it.name == "message" },
                transform = { param ->
                    when (val t = param.type) {
                        is Type.Custom -> param.copy(type = t.copy(name = prefix + t.name))
                        else -> param
                    }
                },
            ) as Interface
        } else {
            this
        }

    private fun Definition.emitFullyQualified(reference: Reference) =
        if (identifier.value == reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }
}
