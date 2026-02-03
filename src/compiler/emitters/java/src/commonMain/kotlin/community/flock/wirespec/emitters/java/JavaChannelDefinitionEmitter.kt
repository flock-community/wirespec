package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.ChannelDefinitionEmitter
import community.flock.wirespec.compiler.core.emit.HasPackageName
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.language.converter.convert
import community.flock.wirespec.language.core.Function
import community.flock.wirespec.language.core.Parameter
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.generator.generateJava

interface JavaChannelDefinitionEmitter : ChannelDefinitionEmitter, HasPackageName, JavaTypeDefinitionEmitter {

    override fun emit(channel: Channel): String {
        val fullyQualifiedPrefix = channel.emitFullyQualified(channel.reference)
        val converted = channel.convert()

        fun Parameter.convert() = when {
            (name == "message") -> {
                val newType = when (val t = type) {
                    is Type.Custom -> t.copy(name = fullyQualifiedPrefix + t.name)
                    else -> t
                }
                copy(type = newType)
            }

            else -> this
        }

        val element = if (fullyQualifiedPrefix.isNotEmpty()) {
            converted.copy(
                elements = converted.elements.map { function ->
                    when {
                        function is Function && function.name == "invoke" -> function.copy(
                            parameters = function.parameters.map { param -> param.convert() }
                        )

                        else -> function
                    }
                }
            )
        } else {
            converted
        }


        return """
            |@FunctionalInterface
            |${element.generateJava()}
            |
        """.trimMargin()
    }


    private fun Definition.emitFullyQualified(reference: Reference) =
        if (identifier.value == reference.value) {
            "${packageName.value}.model."
        } else {
            ""
        }

}
